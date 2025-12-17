/*
 * Copyright 2025 Karma Krafts
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

package dev.karmakrafts.iridium

import dev.karmakrafts.iridium.matcher.hasAnnotation
import dev.karmakrafts.iridium.matcher.hasTypeParameter
import dev.karmakrafts.iridium.matcher.hasValueParameter
import dev.karmakrafts.iridium.matcher.returns
import dev.karmakrafts.iridium.pipeline.defaultPipelineSpec
import dev.karmakrafts.iridium.pipeline.withApi
import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.config.ApiVersion
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrFunctionReference
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.util.target
import org.junit.jupiter.api.Test

@OptIn(UnsafeDuringIrConstructionAPI::class)
class CompilerSmokeTest {
    @Language("kotlin")
    private val defaultProgram: String = """
        @Suppress("UNCHECKED_CAST")
        fun <T> test(value: T): T = value
        fun main(args: Array<String>) {
            println("Hello, World")
        }
    """.trimIndent()

    private fun CompilerTestScope.checkDefaultProgram() {
        default {
            compiler shouldNotReport { error() }
        }
        pipeline {
            defaultPipelineSpec()
        }
        source(defaultProgram)
        result irMatches {
            getChild<IrFunction> { it.name.asString() == "main" }.matches("main") {
                returns { unit() }
                hasValueParameter("args") { type(types.stringType.array()) }
            }
            getChild<IrFunction> { it.name.asString() == "test" }.matches("test") {
                hasAnnotation(type("kotlin/Suppress"))
                hasTypeParameter("T")
                returns { typeParameter("T") }
                hasValueParameter("value") { typeParameter("T") }
            }
            containsChild<IrCall> { it.target.name.asString() == "println" }
        }
    }

    @Test
    fun `Compile Kotlin JVM program`() = runCompilerTest {
        checkDefaultProgram()
    }

    //@Test
    //fun `Compile Kotlin JS program`() = runCompilerTest {
    //    pipeline {
    //        target = CompileTarget.JS
    //    }
    //    checkDefaultProgram()
    //} TODO: reimplement this

    //@Test
    //fun `Compile Kotlin Native program`() = runCompilerTest {
    //    pipeline {
    //        target = CompileTarget.NATIVE
    //    }
    //    checkDefaultProgram()
    //} TODO: reimplement this

    @Test
    fun `Compile simple Kotlin program with older API`() = runCompilerTest {
        pipeline {
            languageVersionSettings = LanguageVersion.KOTLIN_1_9 withApi ApiVersion.KOTLIN_1_9
        }
        checkDefaultProgram()
    }

    @Test
    fun `Compile simple Kotlin program with newer API`() = runCompilerTest {
        pipeline {
            languageVersionSettings = LanguageVersion.KOTLIN_2_2 withApi ApiVersion.KOTLIN_2_2
        }
        checkDefaultProgram()
    }

    @Test
    fun `Compile Kotlin program with error`() = runCompilerTest {
        pipeline {
            defaultPipelineSpec()
        }
        source(
            """
            fun main(args: Array<IDoNotExist>) {
                println("Hello, World", TESTING)
            }
        """.trimIndent()
        )
        compiler shouldReport {
            error()
            messageWith("IDoNotExist")
            atLine(1)
            inColumn(22)
        } atLeast 1
        compiler shouldReport {
            error()
            messageWith("TESTING")
            atLine(2)
            inColumn(29)
        } atLeast 1
    }

    @Test
    fun `Compile simple Kotlin program with Java APIs`() = runCompilerTest {
        pipeline {
            defaultPipelineSpec()
        }
        source(
            """
            import java.lang.Thread
            private fun threadMain() {
                for (number in 0..<10000) {
                    println(number)
                }
            }
            fun main(args: Array<String>) {   
                val threads = ArrayList<Thread>()
                for (i in 0..<10) {
                    threads += Thread(::threadMain)
                }
                threads.forEach(Thread::join)
            }
        """.trimIndent()
        )
        compiler shouldNotReport { error() }
        result irMatches {
            containsChild<IrFunctionReference> { it.reflectionTarget!!.owner.name.asString() == "join" }
        }
    }
}