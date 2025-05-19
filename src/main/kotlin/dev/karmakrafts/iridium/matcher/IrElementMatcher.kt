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
import dev.karmakrafts.iridium.util.renderIrTree
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrAnnotationContainer
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithName
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.declarations.IrTypeParametersContainer
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.name.ClassId

@CompilerTestDsl
class IrElementMatcher<ELEMENT : IrElement> @PublishedApi @TestOnly internal constructor( // @formatter:off
    val scopeName: String,
    val element: ELEMENT,
    override val pluginContext: IrPluginContext,
    @PublishedApi internal val depth: Int = 0
) : IrTypeAwareMatcher() { // @formatter:on
    inline fun <reified T : IrElement> T.matches(name: String, block: IrElementMatcher<T>.() -> Unit) {
        IrElementMatcher(name, this, pluginContext, depth + 1).block()
    }

    inline fun <reified T : IrType> T.matches(block: IrTypeMatcher<T>.() -> Unit) {
        IrTypeMatcher(this, element, pluginContext).block()
    }

    inline infix fun <reified T : IrElement> T.matches(block: IrElementMatcher<T>.() -> Unit) =
        matches("$scopeName-$depth", block)

    inline fun <reified T : IrElement> containsChild(crossinline predicate: (T) -> Boolean = { true }) {
        assert(element.hasChild<T>(predicate)) {
            "Expected child in element in $scopeName:\n\n${element.renderIrTree()}\n"
        }
    }

    inline fun <reified T : IrElement> containsNoChild(crossinline predicate: (T) -> Boolean = { true }) {
        assert(!element.hasChild<T>(predicate)) {
            "Expected no child in element in $scopeName:\n\n${element.renderIrTree()}\n"
        }
    }
}

@CompilerTestDsl
inline fun IrElementMatcher<out IrFunction>.returns(typeMatcher: IrTypeMatcher<IrType>.() -> Unit) {
    IrTypeMatcher(element.returnType, element, pluginContext).typeMatcher()
}

@CompilerTestDsl
inline fun IrElementMatcher<out IrFunction>.hasValueParameter(
    name: String, typeMatcher: IrTypeMatcher<IrType>.() -> Unit
) {
    val type = element.parameters.find { it.kind == IrParameterKind.Regular && it.name.asString() == name }!!.type
    IrTypeMatcher(type, element, pluginContext).typeMatcher()
}

@CompilerTestDsl
inline fun IrElementMatcher<out IrFunction>.hasValueParameter(
    index: Int, typeMatcher: IrTypeMatcher<IrType>.() -> Unit
) {
    val type = element.parameters.filter { it.kind == IrParameterKind.Regular }[index].type
    IrTypeMatcher(type, element, pluginContext).typeMatcher()
}

@CompilerTestDsl
fun <T> IrElementMatcher<T>.hasAnnotation(type: IrType) where T : IrElement, T : IrAnnotationContainer {
    assert(element.annotations.any { it.type == type }) {
        "Expected annotation of type ${type.render()} in:\n\n${element.renderIrTree()}\n"
    }
}

@CompilerTestDsl
fun <T> IrElementMatcher<T>.hasAnnotation(id: ClassId) where T : IrElement, T : IrAnnotationContainer =
    hasAnnotation(type(id))

@CompilerTestDsl
fun IrElementMatcher<out IrTypeParametersContainer>.hasTypeParameter(name: String) {
    assert(element.typeParameters.any { it.name.asString() == name }) {
        "Expected type parameter named '$name' in:\n\n${element.renderIrTree()}\n"
    }
}

@CompilerTestDsl
fun IrElementMatcher<out IrTypeParametersContainer>.hasTypeParameter(index: Int) {
    assert(index in element.typeParameters.indices) {
        "Expected type parameter at index '$index' in:\n\n${element.renderIrTree()}\n"
    }
}

@CompilerTestDsl
fun IrElementMatcher<out IrDeclarationWithName>.isNamed(name: String) {
    assert(element.name.asString() == name) {
        "No declaration named '$name' in $scopeName:\n\n${element.renderIrTree()}\n"
    }
}