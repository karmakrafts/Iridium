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
import dev.karmakrafts.iridium.util.DefaultFirExtensionRegistrar
import dev.karmakrafts.iridium.util.DelegatingIrGenerationExtension
import dev.karmakrafts.iridium.util.IrGenerationCallback
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.fir.extensions.FirDeclarationGenerationExtension
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.ir.backend.js.lower.inline.CopyInlineFunctionBodyLowering
import java.io.File
import kotlin.reflect.KFunction

/**
 * DSL marker annotation for the compiler pipeline DSL.
 *
 * This annotation is used to mark classes that are part of the compiler pipeline DSL,
 * helping the compiler to provide better type checking and IDE support for the DSL.
 */
@DslMarker
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS)
internal annotation class CompilerPipelineDsl

/**
 * Builder class for creating [CompilerPipeline] instances using a DSL-style syntax.
 *
 * This class provides methods for configuring various aspects of the compiler pipeline,
 * such as language version settings, compiler configuration, and extensions.
 */
@CompilerPipelineDsl
class CompilerPipelineBuilder @PublishedApi internal constructor() {
    private val irExtensions: ArrayList<IrGenerationExtension> = ArrayList()
    private val firExtensionRegistrars: ArrayList<FirExtensionRegistrar> = ArrayList()

    @PublishedApi
    internal var messageCallback: CompilerMessageCallback = CompilerMessageCallback {}

    @PublishedApi
    internal val compilerConfig: CompilerConfiguration = CompilerConfiguration()

    /**
     * The target platform to compile the code for.
     * 
     * This property determines which platform (JVM, JS, Native, WASM) the code will be compiled for.
     * Default is JVM.
     */
    var target: CompileTarget = CompileTarget.JVM

    /**
     * The language version settings to use for compilation.
     */
    var languageVersionSettings: LanguageVersionSettings = LanguageVersionSettingsImpl.DEFAULT

    /**
     * Registers a callback for compiler messages.
     *
     * @param callback The callback to register
     */
    fun onMessage(callback: CompilerMessageCallback) {
        messageCallback += callback
    }

    /**
     * Configures the compiler configuration using a DSL-style block.
     *
     * @param block The configuration block
     */
    inline fun config(block: CompilerConfiguration.() -> Unit) {
        compilerConfig.block()
    }

    /**
     * Registers a FIR extension registrar.
     *
     * @param registrar The FIR extension registrar to register
     * @throws IllegalStateException if the registrar is already registered
     */
    fun firExtensionRegistrar(registrar: FirExtensionRegistrar) {
        check(registrar !in firExtensionRegistrars) { "FIR extension registrar is already added" }
        firExtensionRegistrars += registrar
    }

    /**
     * Registers a FIR declaration generation extension.
     *
     * This method wraps the extension in a [DefaultFirExtensionRegistrar] and registers it.
     *
     * @param extension The FIR declaration generation extension to register
     */
    fun firExtension(extension: FirDeclarationGenerationExtension) {
        firExtensionRegistrar(DefaultFirExtensionRegistrar(extension))
    }

    /**
     * Registers an IR generation extension.
     *
     * @param extension The IR generation extension to register
     * @throws IllegalStateException if the extension is already registered
     */
    fun irExtension(extension: IrGenerationExtension) {
        check(extension !in irExtensions) { "IR extension is already registered" }
        irExtensions += extension
    }

    /**
     * Registers an IR generation callback.
     *
     * This method wraps the callback in a [DelegatingIrGenerationExtension] and registers it.
     *
     * @param callback The IR generation callback to register
     */
    fun irExtension(callback: IrGenerationCallback) {
        irExtension(DelegatingIrGenerationExtension(callback))
    }

    /**
     * Builds a [CompilerPipeline] instance with the current configuration.
     *
     * @return A new [CompilerPipeline] instance
     */
    @PublishedApi
    internal fun build(): CompilerPipeline = CompilerPipeline(
        compileTarget = target,
        languageVersionSettings = languageVersionSettings,
        firExtensions = firExtensionRegistrars,
        irExtensions = irExtensions,
        compilerConfiguration = compilerConfig,
        messageCallback = messageCallback
    )
}

/**
 * Type alias for a function that configures a [CompilerPipelineBuilder].
 *
 * This is used as the receiver for the DSL-style configuration of a compiler pipeline.
 */
typealias CompilerPipelineSpec = CompilerPipelineBuilder.() -> Unit

/**
 * Applies a default configuration to a [CompilerPipelineBuilder].
 *
 * This function sets up a basic configuration for testing purposes, including:
 * - Enabling FIR
 * - Setting the module name
 * - Disabling incremental compilation
 * - Enabling Java compilation
 * - Setting the JDK home
 * - Adding necessary classpath roots
 *
 * @param moduleName The name of the module to use in the configuration
 */
@TestOnly
fun CompilerPipelineBuilder.defaultPipelineSpec(moduleName: String = "test") {
    config {
        put(CommonConfigurationKeys.USE_FIR, true)
        put(CommonConfigurationKeys.MODULE_NAME, moduleName)
        put(CommonConfigurationKeys.INCREMENTAL_COMPILATION, false)
        put(JVMConfigurationKeys.COMPILE_JAVA, true)
        put(JVMConfigurationKeys.JDK_HOME, File(System.getProperty("java.home")))
        addJvmClasspathRootByType<Function<*>>()
        addJvmClasspathRootByType<KFunction<*>>()
    }
}

/**
 * Creates a [CompilerPipeline] with the given configuration.
 *
 * This function provides a DSL-style way to create and configure a compiler pipeline.
 * By default, it applies the [defaultPipelineSpec] configuration.
 *
 * @param block The configuration block to apply to the pipeline builder
 * @return A configured [CompilerPipeline] instance
 */
@TestOnly
inline fun compilerPipeline(block: CompilerPipelineSpec = { defaultPipelineSpec() }): CompilerPipeline {
    return CompilerPipelineBuilder().apply(block).build()
}
