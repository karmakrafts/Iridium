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

import dev.karmakrafts.iridium.matcher.FirElementMatcher
import dev.karmakrafts.iridium.matcher.IrElementMatcher
import dev.karmakrafts.iridium.pipeline.CompileResult
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment

/**
 * A class for asserting expectations about the structure of compilation results.
 *
 * This class is part of the compiler testing DSL and allows test authors to verify
 * the structure of FIR (Frontend IR) and IR (Intermediate Representation) elements
 * produced during compilation.
 */
@CompilerAssertionDsl
class CompileResultAsserter internal constructor() {
    @PublishedApi
    internal val assertions: ArrayList<(CompileResult) -> Unit> = ArrayList()

    /**
     * Specifies assertions about the FIR (Frontend IR) elements in the compilation result.
     *
     * @param block A lambda that configures matchers for FIR elements
     */
    inline infix fun firMatches(block: FirElementMatcher<FirFile>.() -> Unit) {
        assertions += { result ->
            FirElementMatcher("file", result.firFile).block()
        }
    }

    /**
     * Specifies assertions about the IR (Intermediate Representation) elements in the compilation result.
     *
     * @param block A lambda that configures matchers for IR elements
     */
    inline infix fun irMatches(block: IrElementMatcher<IrModuleFragment>.() -> Unit) {
        assertions += { result ->
            IrElementMatcher("module", result.module, result.pluginContext).block()
        }
    }

    internal fun assert(result: CompileResult) {
        for (assertion in assertions) {
            assertion(result)
        }
    }

    internal fun reset() {
        assertions.clear()
    }
}
