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
import dev.karmakrafts.iridium.util.DelegatingIrGenerationExtension
import dev.karmakrafts.iridium.util.IrGenerationCallback
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import java.io.File
import kotlin.reflect.KFunction

@DslMarker
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS)
internal annotation class CompilerPipelineDsl @TestOnly constructor()

@CompilerPipelineDsl
class CompilerPipelineBuilder @PublishedApi @TestOnly internal constructor() {
    private val irExtensions: ArrayList<IrGenerationExtension> = ArrayList()
    private val firExtensionRegistrars: ArrayList<FirExtensionRegistrar> = ArrayList()

    @PublishedApi
    internal var messageCallback: CompilerMessageCallback = CompilerMessageCallback {}

    @PublishedApi
    internal val compilerConfig: CompilerConfiguration = CompilerConfiguration()

    @get:TestOnly
    @set:TestOnly
    var languageVersionSettings: LanguageVersionSettings = LanguageVersionSettingsImpl.DEFAULT

    @TestOnly
    fun onMessage(callback: CompilerMessageCallback) {
        messageCallback += callback
    }

    @TestOnly
    inline fun config(block: CompilerConfiguration.() -> Unit) {
        compilerConfig.block()
    }

    @TestOnly
    fun firExtensionRegistrar(registrar: FirExtensionRegistrar) {
        check(registrar !in firExtensionRegistrars) { "FIR extension registrar is already added" }
        firExtensionRegistrars += registrar
    }

    @TestOnly
    fun irExtension(extension: IrGenerationExtension) {
        check(extension !in irExtensions) { "IR extension is already registered" }
        irExtensions += extension
    }

    @TestOnly
    fun irExtension(callback: IrGenerationCallback) {
        irExtension(DelegatingIrGenerationExtension(callback))
    }

    @PublishedApi
    internal fun build(): CompilerPipeline = CompilerPipeline(
        languageVersionSettings = languageVersionSettings,
        firExtensions = firExtensionRegistrars,
        irExtensions = irExtensions,
        compilerConfiguration = compilerConfig,
        messageCallback = messageCallback
    )
}

typealias CompilerPipelineSpec = CompilerPipelineBuilder.() -> Unit

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

@TestOnly
inline fun compilerPipeline(block: CompilerPipelineSpec = { defaultPipelineSpec() }): CompilerPipeline {
    return CompilerPipelineBuilder().apply(block).build()
}