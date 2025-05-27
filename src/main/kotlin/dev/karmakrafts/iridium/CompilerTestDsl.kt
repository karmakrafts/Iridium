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
import org.intellij.lang.annotations.Language

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
    companion object {
        private const val DEFAULT_FILE_NAME: String = "test.kt"
    }

    /**
     * Provides access to the compiler asserter for specifying expectations about compiler messages.
     */
    val compiler: CompilerAsserter = CompilerAsserter()

    /**
     * Provides access to the result asserter for specifying expectations about compilation results.
     */
    val result: CompileResultAsserter = CompileResultAsserter()

    @PublishedApi
    internal var pipelineSpec: CompilerPipelineSpec = {}

    /**
     * The Kotlin source code to be compiled and tested.
     *
     * This property holds the source code that will be passed to the compiler pipeline
     * during test evaluation. It can be set using the [source] method.
     */
    var source: String = ""
        private set

    /**
     * The name of the file to be used during compilation.
     *
     * This property specifies the filename that will be passed to the compiler pipeline
     * when compiling the source code. It defaults to "test.kt" but can be changed
     * to simulate compilation of files with different names.
     */
    var fileName: String = DEFAULT_FILE_NAME

    /**
     * The Kotlin source code split into individual lines.
     *
     * This property provides convenient access to the source code as a list of lines,
     * which can be useful for line-by-line processing or for assertions that need to
     * reference specific lines of code.
     */
    inline val sourceLines: List<String>
        get() = source.split('\n')

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
    fun evaluate() {
        compilerPipeline(pipelineSpec).use { pipeline ->
            val messageCollector = pipeline.messageCollector
            val result = pipeline.run(source, fileName)
            try {
                compiler.assert(result)
                this.result.assert(result)
            } catch (error: Throwable) {
                messageCollector.printErrors()
                throw error
            }
        }
    }

    /**
     * Resets all assertions in this test scope.
     *
     * This method clears all assertions that have been registered with both the compiler
     * and result asserters, allowing the test scope to be reused for a new test without
     * carrying over previous assertions.
     */
    fun resetAssertions() {
        compiler.reset()
        result.reset()
    }

    /**
     * Completely resets this test scope to its initial state.
     *
     * This method resets all configuration in the test scope, including:
     * - Clearing the pipeline specification
     * - Resetting the file name to the default value
     * - Clearing all registered assertions
     *
     * After calling this method, the test scope can be reused as if it were newly created.
     */
    fun reset() {
        pipelineSpec = {}
        fileName = DEFAULT_FILE_NAME
        resetAssertions()
    }
}

/**
 * Sets up a compiler test without evaluating it.
 *
 * This function creates a new [CompilerTestScope] and applies the provided configuration
 * to it. It allows setting up a test environment without immediately running the test,
 * which can be useful when you need to perform additional setup or want to control
 * when the test is evaluated.
 *
 * @param scope A lambda with receiver that configures the test scope
 */
inline fun setupCompilerTest(scope: CompilerTestScope.() -> Unit) {
    CompilerTestScope().scope()
}

/**
 * Sets up and immediately evaluates a compiler test.
 *
 * This function creates a new [CompilerTestScope], applies the provided configuration,
 * and then evaluates the test by compiling the source code and verifying all assertions.
 * It's a convenience function that combines test setup and evaluation in a single call.
 *
 * @param scope A lambda with receiver that configures the test scope
 * @see CompilerTestScope.evaluate
 */
inline fun runCompilerTest(scope: CompilerTestScope.() -> Unit) {
    val scope = CompilerTestScope()
    scope.scope()
    scope.evaluate()
}