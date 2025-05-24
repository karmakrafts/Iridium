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
import kotlin.time.TimeSource

/**
 * Marks classes and functions that are part of the compiler testing DSL.
 *
 * This annotation is used to provide type safety and better IDE support for the DSL.
 * It helps prevent accidental misuse of DSL elements outside their intended context.
 */
@DslMarker
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
internal annotation class CompilerTestDsl

/**
 * The main scope class for compiler tests.
 *
 * This class provides the context in which compiler tests are defined. It holds
 * the test configuration, including source code to compile, pipeline configuration,
 * and assertions about compilation results.
 */
@CompilerTestDsl
class CompilerTestScope @PublishedApi internal constructor() {
    /**
     * Provides access to the compiler asserter for specifying expectations about compiler messages.
     */
    val compiler: CompilerAsserter = CompilerAsserter()

    /**
     * Provides access to the result asserter for specifying expectations about compilation results.
     */
    val result: CompileResultAsserter = CompileResultAsserter()

    /**
     * Internal specification for the compiler pipeline configuration.
     */
    @PublishedApi
    internal var pipelineSpec: CompilerPipelineSpec = { defaultPipelineSpec() }

    private var source: String = ""

    /**
     * Sets the Kotlin source code to be compiled and tested.
     *
     * This method specifies the source code that will be passed to the compiler pipeline
     * during test evaluation. The source code is expected to be valid Kotlin code.
     *
     * @param source The Kotlin source code as a string
     */
    fun source(@Language("kotlin") source: String) {
        this.source = source
    }

    /**
     * Configures the compiler pipeline for this test.
     *
     * @param block A lambda that configures the compiler pipeline
     */
    inline fun pipeline(crossinline block: CompilerPipelineSpec) {
        val previousSpec = pipelineSpec
        pipelineSpec = {
            previousSpec()
            block()
        }
    }

    /**
     * Evaluates the test by compiling the source code and verifying all assertions.
     *
     * This method creates a compiler pipeline according to the configured specification,
     * compiles the source code, and applies all registered assertions to the compilation result.
     */
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
            } catch (error: Throwable) {
                messageCollector.printErrors()
                throw error
            }
        }
    }
}

/**
 * Runs a compiler test with the given configuration.
 *
 * This function is the main entry point for the compiler testing DSL. It creates a test scope,
 * applies the provided configuration, and evaluates the test.
 *
 * @param scope A lambda that configures the test using the DSL
 */
inline fun runCompilerTest(scope: CompilerTestScope.() -> Unit) {
    val scope = CompilerTestScope()
    scope.scope()
    scope.evaluate()
}
