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
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment

@CompilerTestDsl
class CompileResultAsserter @TestOnly internal constructor() {
    @PublishedApi
    internal val assertions: ArrayList<(CompileResult) -> Unit> = ArrayList()

    @TestOnly
    inline infix fun firMatches(block: FirElementMatcher<FirFile>.() -> Unit) {
        assertions += { result ->
            FirElementMatcher("file", result.firFile).block()
        }
    }

    @TestOnly
    inline infix fun irMatches(block: IrElementMatcher<IrModuleFragment>.() -> Unit) {
        assertions += { result ->
            IrElementMatcher("module", result.module, result.pluginContext).block()
        }
    }

    @TestOnly
    internal fun assert(result: CompileResult) {
        for (assertion in assertions) {
            assertion(result)
        }
    }
}