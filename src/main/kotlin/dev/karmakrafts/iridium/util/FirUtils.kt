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

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.renderer.FirRenderer
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid

fun FirElement.renderFirTree(maxLines: Int = 5): String {
    val lines = FirRenderer.withResolvePhase().renderElementAsString(this).split("\n").take(maxLines)
    val joinedLines = lines.joinToString("\n")
    return if (lines.size == maxLines) "$joinedLines\n[...]"
    else joinedLines
}

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

inline infix fun <reified T : FirElement> FirElement.getChild(crossinline predicate: (T) -> Boolean = { true }): T =
    findChild<T>(predicate)!!

inline infix fun <reified T : FirElement> FirElement.hasChild(crossinline predicate: (T) -> Boolean = { true }): Boolean =
    findChild<T>(predicate) != null