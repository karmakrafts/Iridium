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

package dev.karmakrafts.iridium

import dev.karmakrafts.iridium.pipeline.CompilerPipelineSpec
import dev.karmakrafts.iridium.pipeline.compilerPipeline
import dev.karmakrafts.iridium.pipeline.defaultPipelineSpec
import org.intellij.lang.annotations.Language
import org.jetbrains.annotations.TestOnly
import kotlin.time.TimeSource

@DslMarker
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
internal annotation class CompilerTestDsl @TestOnly constructor()

@CompilerTestDsl
class CompilerTestScope @PublishedApi @TestOnly internal constructor() {
    @get:TestOnly
    val compiler: CompilerAsserter = CompilerAsserter()

    @get:TestOnly
    val result: CompileResultAsserter = CompileResultAsserter()

    @PublishedApi
    internal var pipelineSpec: CompilerPipelineSpec = { defaultPipelineSpec() }

    @get:TestOnly
    @set:TestOnly
    @Language("kotlin")
    var source: String = ""

    @TestOnly
    inline fun pipeline(crossinline block: CompilerPipelineSpec) {
        val previousSpec = pipelineSpec
        pipelineSpec = {
            previousSpec()
            block()
        }
    }

    @PublishedApi
    internal fun evaluate() {
        compilerPipeline(pipelineSpec).use { pipeline ->
            val messageCollector = pipeline.messageCollector

            val startTime = TimeSource.Monotonic.markNow()
            val result = pipeline.run(source)
            val time = TimeSource.Monotonic.markNow() - startTime

            val errorCount = messageCollector.messages.count { it.severity.isError }
            if (errorCount > 0) System.err.println("Finished compilation in ${time.inWholeMilliseconds}ms with $errorCount errors")
            else println("Finished compilation in ${time.inWholeMilliseconds}ms")

            try {
                compiler.assert(result)
                this.result.assert(result)
            }
            catch (error: Throwable) {
                messageCollector.printErrors()
                throw error
            }
        }
    }
}

@TestOnly
inline fun runCompilerTest(scope: CompilerTestScope.() -> Unit) {
    val scope = CompilerTestScope()
    scope.scope()
    scope.evaluate()
}