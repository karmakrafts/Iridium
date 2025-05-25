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

import dev.karmakrafts.iridium.util.CompilerMessageCallback
import dev.karmakrafts.iridium.util.DelegatingDiagnosticsReporter
import dev.karmakrafts.iridium.util.RecordingMessageCollector
import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.jvm.JvmIrDeserializerImpl
import org.jetbrains.kotlin.builtins.jvm.JvmBuiltIns
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.PsiBasedProjectFileSearchScope
import org.jetbrains.kotlin.cli.jvm.compiler.legacy.pipeline.createProjectEnvironment
import org.jetbrains.kotlin.com.intellij.openapi.Disposable
import org.jetbrains.kotlin.com.intellij.openapi.project.Project
import org.jetbrains.kotlin.com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.config.messageCollector
import org.jetbrains.kotlin.config.moduleName
import org.jetbrains.kotlin.diagnostics.impl.BaseDiagnosticsCollector
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.FirModuleDataImpl
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.backend.Fir2IrCommonMemberStorage
import org.jetbrains.kotlin.fir.backend.Fir2IrComponentsStorage
import org.jetbrains.kotlin.fir.backend.Fir2IrConfiguration
import org.jetbrains.kotlin.fir.backend.Fir2IrConverter
import org.jetbrains.kotlin.fir.backend.Fir2IrPluginContext
import org.jetbrains.kotlin.fir.backend.Fir2IrSyntheticIrBuiltinsSymbolsContainer
import org.jetbrains.kotlin.fir.backend.Fir2IrVisibilityConverter
import org.jetbrains.kotlin.fir.backend.FirProviderWithGeneratedFiles
import org.jetbrains.kotlin.fir.backend.IrBuiltInsOverFir
import org.jetbrains.kotlin.fir.backend.jvm.JvmFir2IrExtensions
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.deserialization.SingleModuleDataProvider
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.java.FirProjectSessionProvider
import org.jetbrains.kotlin.fir.pipeline.buildResolveAndCheckFirFromKtFiles
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.session.FirJvmSessionFactory
import org.jetbrains.kotlin.fir.session.environment.AbstractProjectEnvironment
import org.jetbrains.kotlin.ir.backend.jvm.serialization.JvmIrMangler
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.storage.StorageManager

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
     * The target platform to compile the test code for.
     */
    val compileTarget: CompileTarget,

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

    private val compilerConfiguration: CompilerConfiguration,
    messageCallback: CompilerMessageCallback = CompilerMessageCallback {}
) : AutoCloseable {
    private val disposable: Disposable = Disposer.newDisposable()
    val messageCollector: RecordingMessageCollector = RecordingMessageCollector(messageCallback).apply {
        compilerConfiguration.messageCollector = this
    }
    val diagnosticsCollector: BaseDiagnosticsCollector = DelegatingDiagnosticsReporter(messageCollector)

    private val environment: KotlinCoreEnvironment = createEnvironment()
    private inline val project: Project get() = environment.project
    private val projectEnvironment: AbstractProjectEnvironment = createProjectEnvironment()
    private val storageManager: StorageManager = createStorageManager()

    private val globalSearchScope: GlobalSearchScope = GlobalSearchScope.allScope(project)
    private val fileSearchScope: PsiBasedProjectFileSearchScope = PsiBasedProjectFileSearchScope(globalSearchScope)

    private val psiFactory: KtPsiFactory = KtPsiFactory(project, false)
    private val sessionProvider: FirProjectSessionProvider = FirProjectSessionProvider()
    private val kotlinBuiltIns: JvmBuiltIns = JvmBuiltIns(storageManager, JvmBuiltIns.Kind.FROM_CLASS_LOADER)

    private val libModuleData: FirModuleDataImpl = createModuleData("${compilerConfiguration.moduleName!!}-lib")

    @Suppress("UNUSED")
    private val libSession: FirSession = createLibrarySession()
    private val moduleData: FirModuleDataImpl =
        createModuleData(compilerConfiguration.moduleName!!, listOf(libModuleData))
    private val moduleSession: FirSession = createModuleSession()

    private fun createModuleSession(): FirSession = FirJvmSessionFactory.createModuleBasedSession(
        sessionProvider = sessionProvider,
        projectEnvironment = projectEnvironment,
        extensionRegistrars = firExtensions,
        languageVersionSettings = languageVersionSettings,
        predefinedJavaComponents = null,
        moduleData = moduleData,
        javaSourcesScope = fileSearchScope,
        createIncrementalCompilationSymbolProviders = { null },
        jvmTarget = JvmTarget.DEFAULT,
        lookupTracker = null,
        enumWhenTracker = null,
        importTracker = null,
        needRegisterJavaElementFinder = true,
        init = {})

    private fun createLibrarySession(): FirSession = FirJvmSessionFactory.createLibrarySession(
        mainModuleName = libModuleData.name,
        sessionProvider = sessionProvider,
        moduleDataProvider = SingleModuleDataProvider(libModuleData),
        projectEnvironment = projectEnvironment,
        extensionRegistrars = firExtensions,
        scope = fileSearchScope,
        packagePartProvider = environment.createPackagePartProvider(globalSearchScope),
        languageVersionSettings = languageVersionSettings,
        predefinedJavaComponents = null
    )

    private fun createStorageManager(): StorageManager = LockBasedStorageManager.createWithExceptionHandling( // @formatter:off
        "CompilerPipeline storage manager debug",
        LockBasedStorageManager.ExceptionHandlingStrategy.THROW
    ) // @formatter:on

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
    ): FirModuleDataImpl = FirModuleDataImpl( // @formatter:on
        name = Name.special("<$name>"),
        dependencies = dependencies,
        dependsOnDependencies = emptyList(),
        friendDependencies = emptyList(),
        platform = compileTarget.platform
    )

    private fun createComponentStorage( // @formatter:off
        files: List<FirFile>,
        scopeSession: ScopeSession
    ): Pair<Fir2IrComponentsStorage, Fir2IrSyntheticIrBuiltinsSymbolsContainer> { // @formatter:on
        val syntheticSymbolContainer = Fir2IrSyntheticIrBuiltinsSymbolsContainer()
        return Fir2IrComponentsStorage(
            session = moduleSession,
            scopeSession = scopeSession,
            fir = files,
            extensions = JvmFir2IrExtensions(compilerConfiguration, JvmIrDeserializerImpl()),
            configuration = Fir2IrConfiguration.forJvmCompilation(compilerConfiguration, diagnosticsCollector),
            visibilityConverter = Fir2IrVisibilityConverter.Default,
            commonMemberStorage = Fir2IrCommonMemberStorage(),
            irMangler = JvmIrMangler,
            kotlinBuiltIns = kotlinBuiltIns,
            specialAnnotationsProvider = null,
            firProvider = FirProviderWithGeneratedFiles(moduleSession, emptyMap()),
            syntheticIrBuiltinsSymbolsContainer = syntheticSymbolContainer
        ) to syntheticSymbolContainer
    }

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
    fun run(
        @Language("kotlin") source: String = "", fileName: String = "test.kt"
    ): CompileResult {
        val input = psiFactory.createPhysicalFile(fileName, source)
        val (_, scopeSession, files) = buildResolveAndCheckFirFromKtFiles( // @formatter:off
            session = moduleSession,
            ktFiles = listOf(input),
            diagnosticsReporter = diagnosticsCollector
        ) // @formatter:on
        val (components, syntheticSymbolContainer) = createComponentStorage(files, scopeSession)
        val irBuiltIns = IrBuiltInsOverFir(components, syntheticSymbolContainer)
        val pluginContext = Fir2IrPluginContext(
            c = components,
            irBuiltIns = irBuiltIns,
            moduleDescriptor = components.moduleDescriptor,
            symbolTable = SymbolTable(null, IrFactoryImpl),
            messageCollector = messageCollector,
            diagnosticReporter = diagnosticsCollector
        )
        val module = Fir2IrConverter.generateIrModuleFragment(components, files)
        for (extension in irExtensions) {
            extension.generate(module, pluginContext)
        }
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
