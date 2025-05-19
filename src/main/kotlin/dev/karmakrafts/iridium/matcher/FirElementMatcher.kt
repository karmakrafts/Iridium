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

package dev.karmakrafts.iridium.matcher

import dev.karmakrafts.iridium.CompilerTestDsl
import dev.karmakrafts.iridium.util.hasChild
import dev.karmakrafts.iridium.util.renderFirTree
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.fir.FirElement

@CompilerTestDsl
class FirElementMatcher<ELEMENT : FirElement> @PublishedApi @TestOnly internal constructor( // @formatter:off
    val scopeName: String,
    val element: ELEMENT,
    @PublishedApi internal val depth: Int = 0
) { // @formatter:on
    inline fun <reified T : FirElement> T.matches(name: String, block: FirElementMatcher<T>.() -> Unit) {
        FirElementMatcher(name, this, depth + 1).block()
    }

    inline infix fun <reified T : FirElement> T.matches(block: FirElementMatcher<T>.() -> Unit) =
        matches("$scopeName-$depth", block)

    inline fun <reified T : FirElement> containsChild(crossinline predicate: (T) -> Boolean = { true }) {
        assert(element.hasChild<T>(predicate)) { "Expected child in element in $scopeName:\n\n${element.renderFirTree()}\n" }
    }

    inline fun <reified T : FirElement> containsNoChild(crossinline predicate: (T) -> Boolean = { true }) {
        assert(!element.hasChild<T>(predicate)) { "Expected no child in element in $scopeName:\n\n${element.renderFirTree()}\n" }
    }
}