/*
 * Copyright 2025 (C) Karma Krafts & associates
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.karmakrafts.iridium.pipeline

import dev.karmakrafts.iridium.util.DelegatingDiagnosticsReporter
import dev.karmakrafts.iridium.util.RecordingMessageCollector
import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.serialization.DescriptorByIdSignatureFinderImpl
import org.jetbrains.kotlin.backend.jvm.JvmIrDeserializerImpl
import org.jetbrains.kotlin.backend.jvm.JvmIrTypeSystemContext
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.JvmPackagePartProvider
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.PsiBasedProjectFileSearchScope
import org.jetbrains.kotlin.cli.jvm.compiler.legacy.pipeline.convertToIrAndActualizeForJvm
import org.jetbrains.kotlin.cli.jvm.compiler.legacy.pipeline.createProjectEnvironment
import org.jetbrains.kotlin.com.intellij.openapi.Disposable
import org.jetbrains.kotlin.com.intellij.openapi.project.Project
import org.jetbrains.kotlin.com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.config.messageCollector
import org.jetbrains.kotlin.config.moduleName
import org.jetbrains.kotlin.diagnostics.impl.BaseDiagnosticsCollector
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSourceModuleData
import org.jetbrains.kotlin.fir.backend.jvm.JvmFir2IrExtensions
import org.jetbrains.kotlin.fir.caches.FirThreadUnsafeCachesFactory
import org.jetbrains.kotlin.fir.deserialization.SingleModuleDataProvider
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.pipeline.FirResult
import org.jetbrains.kotlin.fir.pipeline.ModuleCompilerAnalyzedOutput
import org.jetbrains.kotlin.fir.pipeline.buildResolveAndCheckFirFromKtFiles
import org.jetbrains.kotlin.fir.session.FirJvmSessionFactory
import org.jetbrains.kotlin.fir.session.FirSharableJavaComponents
import org.jetbrains.kotlin.fir.session.environment.AbstractProjectEnvironment
import org.jetbrains.kotlin.ir.backend.jvm.serialization.JvmDescriptorMangler
import org.jetbrains.kotlin.ir.backend.jvm.serialization.JvmIrLinker
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi2ir.generators.DeclarationStubGeneratorImpl

/**
 * A compiler pipeline that handles the compilation of Kotlin source code.
 *
 * This class provides a complete pipeline for compiling Kotlin source code, including:
 * - Setting up the compilation environment
 * - Parsing the source code
 * - Resolving and checking the FIR (Frontend IR) representation
 * - Converting FIR to IR (Intermediate Representation)
 * - Running IR extensions
 * - Collecting compiler messages
 *
 * The pipeline supports customization through FIR and IR extensions, language version settings,
 * and compiler configuration.
 */
class CompilerPipeline internal constructor(

    /**
     * The language version settings to use for compilation.
     */
    val languageVersionSettings: LanguageVersionSettings = LanguageVersionSettingsImpl.DEFAULT,

    /**
     * The list of FIR extension registrars to use during compilation.
     */
    val firExtensions: List<FirExtensionRegistrar> = emptyList(),

    /**
     * The list of IR generation extensions to use during compilation.
     */
    val irExtensions: List<IrGenerationExtension> = emptyList(),

    @PublishedApi internal val compilerConfiguration: CompilerConfiguration
) : AutoCloseable {
    private val disposable: Disposable = Disposer.newDisposable()

    inline val messageCollector: RecordingMessageCollector
        get() = compilerConfiguration.messageCollector as RecordingMessageCollector

    val diagnosticsCollector: BaseDiagnosticsCollector = DelegatingDiagnosticsReporter(messageCollector)

    private val environment: KotlinCoreEnvironment = createEnvironment()
    private inline val project: Project get() = environment.project
    private val projectEnvironment: AbstractProjectEnvironment = createProjectEnvironment()

    private val globalSearchScope: GlobalSearchScope = GlobalSearchScope.allScope(project)
    private val fileSearchScope: PsiBasedProjectFileSearchScope = PsiBasedProjectFileSearchScope(globalSearchScope)

    private val psiFactory: KtPsiFactory = KtPsiFactory(project, false)

    //private val sessionProvider: FirProjectSessionProvider = FirProjectSessionProvider()
    private val packagePartProvider: JvmPackagePartProvider by lazy {
        environment.createPackagePartProvider(
            globalSearchScope
        )
    }
    private val predefJavaComponents: FirSharableJavaComponents by lazy {
        FirSharableJavaComponents(
            FirThreadUnsafeCachesFactory
        )
    }

    private val sharedLibSession: FirSession = FirJvmSessionFactory.createSharedLibrarySession(
        mainModuleName = Name.special("<${compilerConfiguration.moduleName!!}>"),
        //sessionProvider = sessionProvider,
        projectEnvironment = projectEnvironment,
        extensionRegistrars = firExtensions,
        packagePartProvider = packagePartProvider,
        languageVersionSettings = languageVersionSettings,
        predefinedJavaComponents = predefJavaComponents
    )

    private val libModuleData: FirModuleData = createModuleData("${compilerConfiguration.moduleName!!}-lib")

    @Suppress("UNUSED") // This needs to be created and held for the lifetime of the pipeline
    private val libSession: FirSession = FirJvmSessionFactory.createLibrarySession(
        // sessionProvider = sessionProvider,
        sharedLibrarySession = sharedLibSession,
        moduleDataProvider = SingleModuleDataProvider(libModuleData),
        projectEnvironment = projectEnvironment,
        extensionRegistrars = firExtensions,
        scope = fileSearchScope,
        packagePartProvider = packagePartProvider,
        languageVersionSettings = languageVersionSettings,
        predefinedJavaComponents = predefJavaComponents
    )

    private val sourceModuleData: FirModuleData =
        createModuleData(compilerConfiguration.moduleName!!, listOf(libModuleData))

    private val sourceSession: FirSession = FirJvmSessionFactory.createSourceSession(
        moduleData = sourceModuleData,
        //sessionProvider = sessionProvider,
        javaSourcesScope = fileSearchScope,
        projectEnvironment = projectEnvironment,
        createIncrementalCompilationSymbolProviders = { null },
        extensionRegistrars = firExtensions,
        configuration = compilerConfiguration,
        predefinedJavaComponents = null,
        needRegisterJavaElementFinder = false,
        init = {},
        isForLeafHmppModule = false
    )

    private fun createProjectEnvironment(): AbstractProjectEnvironment = createProjectEnvironment(
        configuration = compilerConfiguration,
        parentDisposable = disposable,
        configFiles = EnvironmentConfigFiles.JVM_CONFIG_FILES,
        messageCollector = messageCollector,
    )

    private fun createEnvironment(): KotlinCoreEnvironment = KotlinCoreEnvironment.createForProduction( // @formatter:off
        configuration = compilerConfiguration,
        projectDisposable = disposable,
        configFiles = EnvironmentConfigFiles.JVM_CONFIG_FILES
    ) // @formatter:on

    private fun createModuleData( // @formatter:off
        name: String,
        dependencies: List<FirModuleData> = emptyList()
    ): FirSourceModuleData = FirSourceModuleData( // @formatter:on
        name = Name.special("<$name>"),
        dependencies = dependencies,
        dependsOnDependencies = emptyList(),
        friendDependencies = emptyList(),
        platform = JvmPlatforms.defaultJvmPlatform
    )

    /**
     * Compiles the given Kotlin source code and returns the compilation result.
     *
     * This method performs the following steps:
     * 1. Creates a PSI file from the source code
     * 2. Builds, resolves, and checks the FIR representation
     * 3. Creates the component storage for FIR to IR conversion
     * 4. Generates the IR module fragment
     * 5. Runs all registered IR extensions on the module
     * 6. Returns a [CompileResult] containing all compilation artifacts
     *
     * @param source The Kotlin source code to compile
     * @return A [CompileResult] containing the compilation artifacts and messages
     */
    fun run( // @formatter:off
        @Language("kotlin") source: String = "",
        fileName: String = "test.kt"
    ): CompileResult { // @formatter:on
        val input = psiFactory.createPhysicalFile(fileName, source)
        val (_, scopeSession, files) = buildResolveAndCheckFirFromKtFiles( // @formatter:off
            session = sourceSession,
            ktFiles = listOf(input),
            diagnosticsReporter = diagnosticsCollector
        ) // @formatter:on
        val firResult = FirResult(listOf(ModuleCompilerAnalyzedOutput(sourceSession, scopeSession, files)))
        val (module, _, pluginContext, _, irBuiltIns, symbolTable) = firResult.convertToIrAndActualizeForJvm(
            fir2IrExtensions = JvmFir2IrExtensions(compilerConfiguration, JvmIrDeserializerImpl()),
            configuration = compilerConfiguration,
            diagnosticsReporter = diagnosticsCollector,
            irGeneratorExtensions = irExtensions
        )
        // Right now, linking is only supported on the JVM target
        val descriptorMangler = JvmDescriptorMangler(null)
        val linker = JvmIrLinker(
            currentModule = module.descriptor,
            messageCollector = messageCollector,
            typeSystem = JvmIrTypeSystemContext(irBuiltIns),
            symbolTable = symbolTable,
            stubGenerator = DeclarationStubGeneratorImpl(
                moduleDescriptor = module.descriptor,
                symbolTable = symbolTable,
                irBuiltins = irBuiltIns,
                descriptorFinder = DescriptorByIdSignatureFinderImpl(
                    moduleDescriptor = module.descriptor, mangler = descriptorMangler
                )
            ),
            manglerDesc = descriptorMangler,
            enableIdSignatures = true
        )
        linker.init(module)
        linker.postProcess(true)
        return CompileResult(
            source = source,
            firFile = files.first(),
            module = module,
            messages = messageCollector.messages.toList(),
            pluginContext = pluginContext
        )
    }

    /**
     * Closes this compiler pipeline and disposes of all resources.
     *
     * This method should be called when the pipeline is no longer needed to free up resources.
     */
    override fun close() {
        Disposer.dispose(disposable)
    }
}
