/*
 * Copyright 2026 Karma Krafts
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

import dev.karmakrafts.iridium.util.CompilerHostInfo
import dev.karmakrafts.iridium.util.DelegatingDiagnosticsReporter
import dev.karmakrafts.iridium.util.RecordingMessageCollector
import dev.karmakrafts.iridium.util.formatString
import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.serialization.DescriptorByIdSignatureFinderImpl
import org.jetbrains.kotlin.backend.jvm.JvmIrTypeSystemContext
import org.jetbrains.kotlin.backend.konan.serialization.KonanManglerIr
import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.cli.CliDiagnostics
import org.jetbrains.kotlin.cli.create
import org.jetbrains.kotlin.cli.diagnosticFactoriesStorage
import org.jetbrains.kotlin.cli.jvm.compiler.VfsBasedProjectEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.legacy.pipeline.convertToIrAndActualizeForJvm
import org.jetbrains.kotlin.cli.jvm.compiler.legacy.pipeline.createProjectEnvironment
import org.jetbrains.kotlin.com.intellij.openapi.Disposable
import org.jetbrains.kotlin.com.intellij.openapi.project.Project
import org.jetbrains.kotlin.com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.KotlinCompilerVersion
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.config.messageCollector
import org.jetbrains.kotlin.config.moduleName
import org.jetbrains.kotlin.diagnostics.KtRegisteredDiagnosticFactoriesStorage
import org.jetbrains.kotlin.diagnostics.impl.BaseDiagnosticsCollector
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSourceModuleData
import org.jetbrains.kotlin.fir.backend.Fir2IrConfiguration
import org.jetbrains.kotlin.fir.backend.Fir2IrExtensions
import org.jetbrains.kotlin.fir.backend.Fir2IrVisibilityConverter
import org.jetbrains.kotlin.fir.backend.jvm.JvmFir2IrExtensions
import org.jetbrains.kotlin.fir.deserialization.SingleModuleDataProvider
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.pipeline.AllModulesFrontendOutput
import org.jetbrains.kotlin.fir.pipeline.SingleModuleFrontendOutput
import org.jetbrains.kotlin.fir.pipeline.buildResolveAndCheckFirFromKtFiles
import org.jetbrains.kotlin.fir.pipeline.convertToIrAndActualize
import org.jetbrains.kotlin.fir.session.AbstractFirKlibSessionFactory
import org.jetbrains.kotlin.fir.session.FirJsSessionFactory
import org.jetbrains.kotlin.fir.session.FirJvmSessionFactory
import org.jetbrains.kotlin.fir.session.FirJvmSessionFactory.Context
import org.jetbrains.kotlin.fir.session.FirNativeSessionFactory
import org.jetbrains.kotlin.fir.session.FirWasmSessionFactory
import org.jetbrains.kotlin.fir.session.environment.AbstractProjectFileSearchScope
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir.JsManglerIr
import org.jetbrains.kotlin.ir.backend.jvm.serialization.JvmDescriptorMangler
import org.jetbrains.kotlin.ir.backend.jvm.serialization.JvmIrLinker
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.types.IrTypeSystemContextImpl
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.js.config.libraries
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.loader.KlibLoader
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.wasm.WasmTarget
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi2ir.generators.DeclarationStubGeneratorImpl
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.io.path.walk

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
     * The platform to compile the Kotlin code for.
     */
    val compilerTarget: CompilerTarget = CompilerTarget.JVM,

    /**
     * Extra JARs or KLIBs to be loaded for the specified compiler platform.
     */
    val extraLibraries: List<Path> = emptyList(),

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

    @PublishedApi internal val compilerConfiguration: CompilerConfiguration = CompilerConfiguration.create(),
) : AutoCloseable {
    private val disposable: Disposable = Disposer.newDisposable()

    inline val messageCollector: RecordingMessageCollector
        get() = compilerConfiguration.messageCollector as RecordingMessageCollector

    val diagnosticsCollector: BaseDiagnosticsCollector = DelegatingDiagnosticsReporter(messageCollector)

    init {
        initializeDiagnosticFactoriesStorage()
    }

    private val projectEnvironment: VfsBasedProjectEnvironment = createProjectEnvironment()
    private inline val project: Project get() = projectEnvironment.project

    private val librariesScope: AbstractProjectFileSearchScope = projectEnvironment.getSearchScopeForProjectLibraries()
    private val javaSourcesScope: AbstractProjectFileSearchScope =
        projectEnvironment.getSearchScopeForProjectJavaSources()
    private val psiFactory: KtPsiFactory = KtPsiFactory(project, false)

    private val jvmContext: Context by lazy {
        Context(
            configuration = compilerConfiguration,
            projectEnvironment = projectEnvironment,
            librariesScope = librariesScope,
            registerJvmDeserializationExtension = false
        )
    }

    private val sharedLibSession: FirSession = createSharedLibrarySession()
    private val resolvedKlibLibraries: List<KotlinLibrary> by lazy { resolveKlibLibraries() }
    private val libModuleData: FirModuleData = createModuleData("${compilerConfiguration.moduleName!!}-lib")

    @Suppress("UNUSED") // This needs to be created and held for the lifetime of the pipeline
    private val libSession: FirSession = createLibrarySession()

    private val sourceModuleData: FirModuleData =
        createModuleData(compilerConfiguration.moduleName!!, listOf(libModuleData))

    private val sourceSession: FirSession = createSourceSession()

    private fun initializeDiagnosticFactoriesStorage() {
        if (compilerConfiguration.diagnosticFactoriesStorage != null) return
        compilerConfiguration.diagnosticFactoriesStorage = KtRegisteredDiagnosticFactoriesStorage().apply {
            registerDiagnosticContainers(CliDiagnostics)
        }
    }

    private fun createProjectEnvironment(): VfsBasedProjectEnvironment = createProjectEnvironment(
        configuration = compilerConfiguration,
        parentDisposable = disposable,
        configFiles = compilerTarget.environmentConfigFiles
    )

    private fun AbstractFirKlibSessionFactory<*>.createDefaultSharedLibrarySession(): FirSession =
        createSharedLibrarySession(
            mainModuleName = Name.special("<${compilerConfiguration.moduleName!!}>"),
            configuration = compilerConfiguration,
            extensionRegistrars = firExtensions
        )

    private fun createSharedLibrarySession(): FirSession = when (compilerTarget) {
        CompilerTarget.JVM -> FirJvmSessionFactory.createSharedLibrarySession(
            mainModuleName = Name.special("<${compilerConfiguration.moduleName!!}>"),
            extensionRegistrars = firExtensions,
            languageVersionSettings = languageVersionSettings,
            context = jvmContext
        )

        CompilerTarget.NATIVE -> FirNativeSessionFactory.createDefaultSharedLibrarySession()
        CompilerTarget.JS -> FirJsSessionFactory.createDefaultSharedLibrarySession()
        CompilerTarget.WASM -> FirWasmSessionFactory.of(WasmTarget.JS).createDefaultSharedLibrarySession()
    }

    private fun AbstractFirKlibSessionFactory<*>.createDefaultLibrarySession(): FirSession {
        return createLibrarySession(
            resolvedLibraries = resolvedKlibLibraries,
            sharedLibrarySession = sharedLibSession,
            moduleDataProvider = SingleModuleDataProvider(libModuleData),
            extensionRegistrars = firExtensions,
            compilerConfiguration = compilerConfiguration
        )
    }

    private fun createLibrarySession(): FirSession = when (compilerTarget) {
        CompilerTarget.JVM -> FirJvmSessionFactory.createLibrarySession(
            sharedLibrarySession = sharedLibSession,
            moduleDataProvider = SingleModuleDataProvider(libModuleData),
            extensionRegistrars = firExtensions,
            languageVersionSettings = languageVersionSettings,
            context = jvmContext
        )

        CompilerTarget.NATIVE -> FirNativeSessionFactory.createDefaultLibrarySession()
        CompilerTarget.JS -> FirJsSessionFactory.createDefaultLibrarySession()
        CompilerTarget.WASM -> FirWasmSessionFactory.of(WasmTarget.JS).createDefaultLibrarySession()
    }

    private fun AbstractFirKlibSessionFactory<*>.createDefaultSourceSession(): FirSession = createSourceSession(
        moduleData = sourceModuleData,
        extensionRegistrars = firExtensions,
        configuration = compilerConfiguration,
        isForLeafHmppModule = false,
        init = {})

    private fun createSourceSession(): FirSession = when (compilerTarget) {
        CompilerTarget.JVM -> FirJvmSessionFactory.createSourceSession(
            moduleData = sourceModuleData,
            javaSourcesScope = javaSourcesScope,
            createIncrementalCompilationSymbolProviders = { null },
            extensionRegistrars = firExtensions,
            configuration = compilerConfiguration,
            context = jvmContext,
            needRegisterJavaElementFinder = false,
            init = {},
            isForLeafHmppModule = false
        )

        CompilerTarget.NATIVE -> FirNativeSessionFactory.createDefaultSourceSession()
        CompilerTarget.JS -> FirJsSessionFactory.createDefaultSourceSession()
        CompilerTarget.WASM -> FirWasmSessionFactory.of(WasmTarget.JS).createDefaultSourceSession()
    }

    private fun createModuleData( // @formatter:off
        name: String,
        dependencies: List<FirModuleData> = emptyList()
    ): FirSourceModuleData = FirSourceModuleData( // @formatter:on
        name = Name.special("<$name>"),
        dependencies = dependencies,
        dependsOnDependencies = emptyList(),
        friendDependencies = emptyList(),
        platform = compilerTarget.targetPlatform
    )

    private fun resolveKlibLibraries(): List<KotlinLibrary> {
        if (compilerTarget == CompilerTarget.JVM) return emptyList()
        // @formatter:off
        val paths = (compilerConfiguration.libraries
            + resolveKlibLibraryPaths()
            + extraLibraries.map(Path::absolutePathString))
            .distinct()
        // @formatter:on
        if (paths.isEmpty()) return emptyList()
        val result = KlibLoader { libraryPaths(paths) }.load()
        for (lib in result.problematicLibraries) println(lib.formatString())
        return result.librariesStdlibFirst
    }

    private fun resolveKlibLibraryPaths(): List<String> = when (compilerTarget) { // @formatter:off
        CompilerTarget.JVM -> emptyList()
        CompilerTarget.NATIVE -> listOfNotNull(resolveNativeStdlibPath()) + resolveNativePlatformLibraryPaths()
        CompilerTarget.JS -> listOfNotNull(
            resolveGradleKlibPath("kotlin-stdlib-js", "kotlin-stdlib-js-${KotlinCompilerVersion.VERSION}.klib")
        )
        CompilerTarget.WASM -> listOfNotNull(
            resolveGradleKlibPath("kotlin-stdlib-wasm-js", "kotlin-stdlib-wasm-js-${KotlinCompilerVersion.VERSION}.klib")
        )
    } // @formatter:on

    private fun resolveNativeStdlibPath(): String? {
        val stdlibDir = CompilerHostInfo.konanPrebuiltDir.resolve("klib/common/stdlib")
        if (!stdlibDir.exists()) return null
        return stdlibDir.absolutePathString()
    }

    private fun resolveNativePlatformLibraryPaths(): List<String> {
        val platformTarget = "${CompilerHostInfo.osPrefix}_${CompilerHostInfo.shortArchSuffix}"
        val platformDir = CompilerHostInfo.konanPrebuiltDir.resolve("klib/platform/$platformTarget")
        if (!platformDir.exists()) return emptyList()
        // @formatter:off
        return platformDir.listDirectoryEntries()
            .filter { it.isDirectory() }
            .map { it.absolutePathString() }
            .sorted()
            .toList()
        // @formatter:on
    }

    private fun resolveGradleKlibPath(moduleName: String, fileName: String): String? {
        val moduleDir =
            CompilerHostInfo.gradleHome.resolve("caches/modules-2/files-2.1/org.jetbrains.kotlin/$moduleName/${KotlinCompilerVersion.VERSION}")
        return moduleDir.walk().firstOrNull { it.isRegularFile() && it.name == fileName }?.absolutePathString()
    }

    private fun setupJvmIrLinker(module: IrModuleFragment, symbolTable: SymbolTable, irBuiltIns: IrBuiltIns) {
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
                descriptorFinder = DescriptorByIdSignatureFinderImpl( // @formatter:off
                    moduleDescriptor = module.descriptor,
                    mangler = descriptorMangler
                ) // @formatter:on
            ),
            manglerDesc = descriptorMangler
        )
        linker.init(module)
        linker.postProcess(true)
    }

    private fun convertFirToJvmIr(allModulesOutput: AllModulesFrontendOutput) =
        allModulesOutput.convertToIrAndActualizeForJvm(
            fir2IrExtensions = JvmFir2IrExtensions(compilerConfiguration),
            configuration = compilerConfiguration,
            diagnosticsReporter = diagnosticsCollector,
            irGeneratorExtensions = irExtensions
        )

    private fun convertFirToNativeIr(allModulesOutput: AllModulesFrontendOutput) =
        allModulesOutput.convertToIrAndActualize(
            fir2IrExtensions = Fir2IrExtensions.Default,
            fir2IrConfiguration = Fir2IrConfiguration.forKlibCompilation(
                compilerConfiguration, diagnosticsCollector
            ),
            irGeneratorExtensions = irExtensions,
            irMangler = KonanManglerIr,
            visibilityConverter = Fir2IrVisibilityConverter.Default,
            kotlinBuiltIns = DefaultBuiltIns.Instance,
            typeSystemContextProvider = ::IrTypeSystemContextImpl,
            specialAnnotationsProvider = null,
            extraActualDeclarationExtractorsInitializer = { emptyList() })

    private fun convertFirToWebIr(allModulesOutput: AllModulesFrontendOutput) =
        allModulesOutput.convertToIrAndActualize(
            fir2IrExtensions = Fir2IrExtensions.Default,
            fir2IrConfiguration = Fir2IrConfiguration.forKlibCompilation(
                compilerConfiguration, diagnosticsCollector
            ),
            irGeneratorExtensions = irExtensions,
            irMangler = JsManglerIr,
            visibilityConverter = Fir2IrVisibilityConverter.Default,
            kotlinBuiltIns = DefaultBuiltIns.Instance,
            typeSystemContextProvider = ::IrTypeSystemContextImpl,
            specialAnnotationsProvider = null,
            extraActualDeclarationExtractorsInitializer = { emptyList() })

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

        val singleModuleOutputs = listOf(SingleModuleFrontendOutput(sourceSession, scopeSession, files))
        val allModulesOutput = AllModulesFrontendOutput(singleModuleOutputs)
        val (module, _, pluginContext, _, irBuiltIns, symbolTable) = when (compilerTarget) {
            CompilerTarget.JVM -> convertFirToJvmIr(allModulesOutput)
            CompilerTarget.NATIVE -> convertFirToNativeIr(allModulesOutput)
            CompilerTarget.JS, CompilerTarget.WASM -> convertFirToWebIr(allModulesOutput)
        }
        if (compilerTarget == CompilerTarget.JVM) setupJvmIrLinker(module, symbolTable, irBuiltIns)
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
