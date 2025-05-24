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

package dev.karmakrafts.iridium.util

import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.renderer.FirRenderer
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid

/**
 * Renders the FIR tree structure of this element as a string.
 *
 * This function is useful for debugging and testing purposes, allowing visualization
 * of the FIR tree structure in a human-readable format.
 *
 * @param maxLines The maximum number of lines to include in the output. Default is 5.
 * @return A string representation of the FIR tree, truncated to the specified number of lines.
 *         If truncated, "[...]" is appended to indicate there's more content.
 */
@TestOnly
fun FirElement.renderFirTree(maxLines: Int = 5): String {
    val lines = FirRenderer.withResolvePhase().renderElementAsString(this).split("\n").take(maxLines)
    val joinedLines = lines.joinToString("\n")
    return if (lines.size == maxLines) "$joinedLines\n[...]"
    else joinedLines
}

/**
 * Finds the first child element of the specified type that satisfies the given predicate.
 *
 * This function traverses the FIR tree starting from this element and returns the first
 * child element of type T that matches the provided predicate.
 *
 * @param T The type of element to find.
 * @param predicate A function that determines whether a child element matches the search criteria.
 *                  Default predicate accepts any element of type T.
 * @return The first matching child element, or null if no match is found.
 */
@TestOnly
inline infix fun <reified T : FirElement> FirElement.findChild(crossinline predicate: (T) -> Boolean = { true }): T? {
    var target: T? = null
    accept(object : FirVisitorVoid() {
        override fun visitElement(element: FirElement) {
            if (target != null) return
            element.acceptChildren(this)
            if (element !is T || !predicate(element)) return
            target = element
        }
    })
    return target
}

/**
 * Gets the first child element of the specified type that satisfies the given predicate.
 *
 * This function is similar to [findChild], but it assumes that a matching child element
 * exists and throws a NullPointerException if no match is found.
 *
 * @param T The type of element to get.
 * @param predicate A function that determines whether a child element matches the search criteria.
 *                  Default predicate accepts any element of type T.
 * @return The first matching child element.
 * @throws NullPointerException If no matching child element is found.
 */
@TestOnly
inline infix fun <reified T : FirElement> FirElement.getChild(crossinline predicate: (T) -> Boolean = { true }): T =
    findChild<T>(predicate)!!

/**
 * Checks if this element has a child of the specified type that satisfies the given predicate.
 *
 * This function traverses the FIR tree starting from this element and checks if there is at least
 * one child element of type T that matches the provided predicate.
 *
 * @param T The type of element to check for.
 * @param predicate A function that determines whether a child element matches the search criteria.
 *                  Default predicate accepts any element of type T.
 * @return `true` if a matching child element is found, `false` otherwise.
 */
@TestOnly
inline infix fun <reified T : FirElement> FirElement.hasChild(crossinline predicate: (T) -> Boolean = { true }): Boolean =
    findChild<T>(predicate) != null
