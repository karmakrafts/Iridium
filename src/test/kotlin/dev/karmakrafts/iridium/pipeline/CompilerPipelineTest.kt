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

import dev.karmakrafts.iridium.util.CompilerMessage
import dev.karmakrafts.iridium.util.CompilerMessageCallback
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.config.ApiVersion
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.fir.declarations.DirectDeclarationsAccess
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

@OptIn(UnsafeDuringIrConstructionAPI::class)
class CompilerPipelineTest {
    @Test
    fun `Run FIR and IR extensions on every target`() {
        for (target in CompilerTarget.entries) {
            var firRegistrations = 0
            var irInvocations = 0
            var extensionModule: IrModuleFragment? = null
            var extensionContext: IrPluginContext? = null
            var registrarMessageCollector: MessageCollector? = null
            compilerPipeline {
                defaultPipelineSpec()
                compilerTarget = target
                firExtensionRegistrar { messageCollector ->
                    registrarMessageCollector = messageCollector
                    object : FirExtensionRegistrar() {
                        override fun ExtensionRegistrarContext.configurePlugin() {
                            firRegistrations++
                        }
                    }
                }
                irExtension { module, context ->
                    irInvocations++
                    extensionModule = module
                    extensionContext = context
                    val stringClass = assertNotNull(
                        context.finderForBuiltins().findClass(ClassId.topLevel(FqName("kotlin.String")))
                    )
                    assertEquals("String", stringClass.owner.name.asString())
                }
            }.use { pipeline ->
                val source = "fun test(value: String): Int = value.length"
                val result = pipeline.run(source, "extension.kt")
                assertFalse(pipeline.messageCollector.hasErrors(), "$target: ${result.messages}")
                assertTrue(firRegistrations > 0, target.name)
                assertEquals(1, irInvocations, target.name)
                assertSame(pipeline.messageCollector, registrarMessageCollector)
                assertSame(extensionModule, result.module)
                assertSame(extensionContext, result.pluginContext)
                assertEquals(source, result.source)
                assertEquals("extension.kt", result.firFile.name)
            }
        }
    }

    @Test
    @OptIn(DirectDeclarationsAccess::class)
    fun `Compile empty source on every target`() {
        for (target in CompilerTarget.entries) {
            compilerPipeline {
                defaultPipelineSpec()
                compilerTarget = target
            }.use { pipeline ->
                val result = pipeline.run()
                assertFalse(pipeline.messageCollector.hasErrors(), "$target: ${result.messages}")
                assertEquals("", result.source)
                assertEquals("test.kt", result.firFile.name)
                assertTrue(result.firFile.declarations.isEmpty(), target.name)
                assertTrue(result.module.files.single().declarations.isEmpty(), target.name)
            }
        }
    }

    @Test
    fun `Preserve diagnostic locations and message callbacks on every target`() {
        for (target in CompilerTarget.entries) {
            val callbacks = ArrayList<CompilerMessage>()
            compilerPipeline {
                defaultPipelineSpec()
                compilerTarget = target
                onMessage(CompilerMessageCallback { callbacks += it })
            }.use { pipeline ->
                val source = "fun test(): Missing = TODO()"
                val result = pipeline.run(source, "negative.kt")
                val error = result.messages.first { it.severity.isError && "Missing" in it.message }
                val location = assertNotNull(error.location)
                assertTrue(location.path.endsWith("/negative.kt"), location.path)
                assertEquals(1, location.line)
                assertEquals(13, location.column)
                assertEquals(source, location.lineContent)
                assertTrue(pipeline.messageCollector.hasErrors(), target.name)
                assertTrue(pipeline.diagnosticsCollector.hasErrors, target.name)
                assertEquals(result.messages, callbacks)
            }
        }
    }

    @Test
    fun `Apply language version settings to source sessions on every target`() {
        val settings = LanguageVersion.KOTLIN_2_5 withApi ApiVersion.KOTLIN_1_9
        for (target in CompilerTarget.entries) {
            compilerPipeline {
                defaultPipelineSpec()
                compilerTarget = target
                languageVersionSettings = settings
            }.use { pipeline ->
                val result = pipeline.run("fun test(): Int = 42")
                assertFalse(pipeline.messageCollector.hasErrors(), "$target: ${result.messages}")
                assertSame(settings, pipeline.languageVersionSettings)
                assertSame(settings, result.firFile.moduleData.session.languageVersionSettings, target.name)
                assertSame(settings, result.pluginContext.languageVersionSettings, target.name)
            }
        }
    }
}