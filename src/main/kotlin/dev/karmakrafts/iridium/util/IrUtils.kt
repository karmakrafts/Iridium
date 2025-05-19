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

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.util.DumpIrTreeVisitor
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid

fun IrElement.renderIrTree(maxLines: Int = 5): String {
    val builder = StringBuilder()
    accept(DumpIrTreeVisitor(builder), "")
    val lines = builder.toString().split("\n").take(maxLines)
    val joinedLines = lines.joinToString("\n")
    return if (lines.size == maxLines) "$joinedLines\n[...]"
    else joinedLines
}

inline infix fun <reified T : IrElement> IrElement.findChild(crossinline predicate: (T) -> Boolean): T? {
    var target: T? = null
    acceptVoid(object : IrVisitorVoid() {
        override fun visitElement(element: IrElement) {
            if (target != null) return
            element.acceptChildrenVoid(this)
            if (element !is T || !predicate(element)) return
            target = element
        }
    })
    return target
}

inline infix fun <reified T : IrElement> IrElement.getChild(crossinline predicate: (T) -> Boolean): T =
    findChild<T>(predicate)!!

inline infix fun <reified T : IrElement> IrElement.hasChild(crossinline predicate: (T) -> Boolean): Boolean =
    findChild<T>(predicate) != null