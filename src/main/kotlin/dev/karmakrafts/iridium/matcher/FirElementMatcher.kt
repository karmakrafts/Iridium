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
import dev.karmakrafts.iridium.util.getChild
import dev.karmakrafts.iridium.util.hasChild
import dev.karmakrafts.iridium.util.renderFirTree
import org.jetbrains.kotlin.fir.FirElement

/**
 * A matcher for FIR (Frontend IR) elements that provides a DSL for asserting properties of FIR elements.
 * This class is used in compiler tests to verify the structure and properties of FIR trees.
 *
 * @param ELEMENT The type of FIR element being matched
 * @param scopeName The name of the scope for this matcher, used in assertion messages
 * @param element The FIR element being matched
 * @param depth The current depth in the FIR tree, used for generating scope names
 */
@CompilerTestDsl
class FirElementMatcher<ELEMENT : FirElement> @PublishedApi internal constructor( // @formatter:off
    val scopeName: String,
    val element: ELEMENT,
    @PublishedApi internal val depth: Int = 0
) { // @formatter:on
    /**
     * Retrieves a child element of type T that satisfies the given predicate.
     *
     * This method searches through the children of the current FIR element to find the first
     * element of type T that matches the provided predicate. If no matching element is found,
     * or if an error occurs during the search, an AssertionError is thrown with a detailed
     * error message.
     *
     * @param T The type of FIR child element to retrieve
     * @param predicate A function that takes an element of type T and returns a boolean indicating if it matches
     * @return The first child element of type T that satisfies the predicate
     * @throws AssertionError if no matching child is found or if an error occurs during the search
     */
    inline fun <reified T : FirElement> getChild(crossinline predicate: (T) -> Boolean = { true }): T {
        return try {
            element.getChild(predicate)
        } catch (error: Throwable) {
            throw AssertionError(
                "Could not get child from element $scopeName (${element::class.java.simpleName}/$depth)\n", error
            )
        }
    }

    /**
     * Creates a new matcher for the receiver element with a specified scope name.
     *
     * This method allows for recursive matching of nested elements by creating a new matcher
     * with an incremented depth value.
     *
     * @param T The type of FIR element to match
     * @param name The name of the scope for error reporting
     * @param block The lambda containing assertions to apply to the element
     */
    inline fun <reified T : FirElement> T.matches(name: String, block: FirElementMatcher<T>.() -> Unit) {
        try {
            FirElementMatcher(name, this, depth + 1).block()
        } catch (error: Throwable) {
            throw AssertionError("$scopeName (${element::class.java.simpleName}/$depth)\n", error)
        }
    }

    /**
     * Creates a new matcher for the receiver element with an automatically generated scope name.
     *
     * This is a convenience infix function that delegates to [matches] with a generated scope name.
     *
     * @param T The type of FIR element to match
     * @param block The lambda containing assertions to apply to the element
     */
    inline infix fun <reified T : FirElement> T.matches(block: FirElementMatcher<T>.() -> Unit) =
        matches("$scopeName-$depth", block)

    /**
     * Asserts that the current FIR element contains a child of the specified type that matches the given predicate.
     * If the assertion fails, it provides a detailed error message including a rendering of the FIR tree.
     *
     * @param T The type of FIR element to look for
     * @param predicate A function that takes a FIR element of type T and returns a boolean indicating if it matches the criteria
     * @throws AssertionError if no matching child is found
     */
    inline fun <reified T : FirElement> containsChild(crossinline predicate: (T) -> Boolean = { true }) {
        val result = try {
            element.hasChild<T>(predicate)
        } catch (error: Throwable) {
            throw AssertionError("$scopeName (${element::class.java.simpleName}/$depth)\n", error)
        }
        assert(result) { "Expected child in element in $scopeName:\n\n${element.renderFirTree()}\n" }
    }

    /**
     * Asserts that the current FIR element does not contain a child of the specified type that matches the given predicate.
     * If the assertion fails, it provides a detailed error message including a rendering of the FIR tree.
     *
     * @param T The type of FIR element to look for
     * @param predicate A function that takes a FIR element of type T and returns a boolean indicating if it matches the criteria
     * @throws AssertionError if a matching child is found
     */
    inline fun <reified T : FirElement> containsNoChild(crossinline predicate: (T) -> Boolean = { true }) {
        val result = try {
            !element.hasChild<T>(predicate)
        } catch (error: Throwable) {
            throw AssertionError("$scopeName (${element::class.java.simpleName}/$depth)\n", error)
        }
        assert(result) { "Expected no child in element in $scopeName:\n\n${element.renderFirTree()}\n" }
    }
}
