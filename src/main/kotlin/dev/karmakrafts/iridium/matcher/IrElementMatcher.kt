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
import dev.karmakrafts.iridium.util.renderIrTree
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

/**
 * A matcher for IR elements that provides a DSL for asserting properties of IR elements in compiler tests.
 *
 * This class extends [IrTypeAwareMatcher] and provides methods for matching and asserting properties
 * of IR elements in the compiler's intermediate representation. It allows for recursive matching
 * of nested elements and verification of element properties.
 *
 * The matcher supports:
 * - Matching child elements with specific predicates
 * - Verifying the presence or absence of specific child elements
 * - Matching IR types within elements
 *
 * Example usage:
 * ```
 * irElement matches {
 *     containsChild<IrCall> { it.symbol.owner.name.asString() == "println" }
 * }
 * ```
 *
 * @param ELEMENT The specific IrElement subclass being matched
 * @property scopeName The name of the current scope for error reporting
 * @property element The IR element being matched
 * @property pluginContext The IR plugin context for accessing compiler services
 * @property depth The current nesting depth for recursive matching
 */
@CompilerTestDsl
class IrElementMatcher<ELEMENT : IrElement> @PublishedApi internal constructor( // @formatter:off
    val scopeName: String,
    val element: ELEMENT,
    override val pluginContext: IrPluginContext,
    @PublishedApi internal val depth: Int = 0
) : IrTypeAwareMatcher() { // @formatter:on
    /**
     * Retrieves a child element of type T that satisfies the given predicate.
     *
     * This method searches through the children of the current element to find the first
     * element of type T that matches the provided predicate. If no matching element is found,
     * or if an error occurs during the search, an AssertionError is thrown with a detailed
     * error message.
     *
     * @param T The type of child element to retrieve
     * @param predicate A function that takes an element of type T and returns a boolean indicating if it matches
     * @return The first child element of type T that satisfies the predicate
     * @throws AssertionError if no matching child is found or if an error occurs during the search
     */
    inline fun <reified T : IrElement> getChild(crossinline predicate: (T) -> Boolean = { true }): T {
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
     * @param T The type of IR element to match
     * @param name The name of the scope for error reporting
     * @param block The lambda containing assertions to apply to the element
     */
    inline fun <reified T : IrElement> T.matches(name: String, block: IrElementMatcher<T>.() -> Unit) {
        val newDepth = depth + 1
        try {
            IrElementMatcher(name, this, pluginContext, newDepth).block()
        } catch (error: Throwable) {
            throw AssertionError("$scopeName (${element::class.java.simpleName}/$depth)\n", error)
        }
    }

    /**
     * Creates a new matcher for the receiver element with an automatically generated scope name.
     *
     * This is a convenience infix function that delegates to [matches] with a generated scope name.
     *
     * @param T The type of IR element to match
     * @param block The lambda containing assertions to apply to the element
     */
    inline infix fun <reified T : IrElement> T.matches(block: IrElementMatcher<T>.() -> Unit) =
        matches("$scopeName-$depth", block)

    /**
     * Creates a type matcher for the receiver IR type.
     *
     * This method allows for matching and asserting properties of IR types within elements.
     *
     * @param T The specific IrType subclass to match
     * @param block The lambda containing assertions to apply to the type
     */
    inline infix fun <reified T : IrType> T.matches(block: IrTypeMatcher<T>.() -> Unit) {
        IrTypeMatcher(this, element, pluginContext).block()
    }

    /**
     * Asserts that the element contains a child of type T that satisfies the given predicate.
     *
     * @param T The type of child element to look for
     * @param predicate A function that takes an element of type T and returns a boolean indicating if it matches
     * @throws AssertionError if no matching child is found, with a detailed error message showing the IR tree
     */
    inline fun <reified T : IrElement> containsChild(crossinline predicate: (T) -> Boolean = { true }) {
        val result = try {
            element.hasChild<T>(predicate)
        } catch (error: Throwable) {
            throw AssertionError("$scopeName (${element::class.java.simpleName}/$depth)\n", error)
        }
        assert(result) {
            "Expected child in element in $scopeName:\n\n${element.renderIrTree()}\n"
        }
    }

    /**
     * Asserts that the element does not contain a child of type T that satisfies the given predicate.
     *
     * @param T The type of child element to check for absence
     * @param predicate A function that takes an element of type T and returns a boolean indicating if it matches
     * @throws AssertionError if a matching child is found, with a detailed error message showing the IR tree
     */
    inline fun <reified T : IrElement> containsNoChild(crossinline predicate: (T) -> Boolean = { true }) {
        val result = try {
            !element.hasChild<T>(predicate)
        } catch (error: Throwable) {
            throw AssertionError("$scopeName (${element::class.java.simpleName}/$depth)\n", error)
        }
        assert(result) {
            "Expected no child in element in $scopeName:\n\n${element.renderIrTree()}\n"
        }
    }
}

/**
 * Asserts that the function's return type matches the given type matcher.
 *
 * @param typeMatcher The lambda containing assertions to apply to the return type
 */
inline fun IrElementMatcher<out IrFunction>.returns(typeMatcher: IrTypeMatcher<IrType>.() -> Unit) {
    IrTypeMatcher(element.returnType, element, pluginContext).typeMatcher()
}

/**
 * Asserts that the function has a value parameter with the given name and type.
 *
 * @param name The name of the parameter to check
 * @param typeMatcher The lambda containing assertions to apply to the parameter's type
 * @throws NullPointerException if no parameter with the given name is found
 */
inline fun IrElementMatcher<out IrFunction>.hasValueParameter( // @formatter:off
    name: String,
    typeMatcher: IrTypeMatcher<IrType>.() -> Unit
) { // @formatter:on
    val type = element.parameters.find { it.kind == IrParameterKind.Regular && it.name.asString() == name }!!.type
    IrTypeMatcher(type, element, pluginContext).typeMatcher()
}

/**
 * Asserts that the function has a value parameter at the given index and type.
 *
 * @param index The zero-based index of the parameter to check
 * @param typeMatcher The lambda containing assertions to apply to the parameter's type
 * @throws IndexOutOfBoundsException if the index is out of bounds
 */
inline fun IrElementMatcher<out IrFunction>.hasValueParameter( // @formatter:off
    index: Int,
    typeMatcher: IrTypeMatcher<IrType>.() -> Unit
) { // @formatter:on
    val type = element.parameters.filter { it.kind == IrParameterKind.Regular }[index].type
    IrTypeMatcher(type, element, pluginContext).typeMatcher()
}

/**
 * Asserts that the element has an annotation of the given type.
 *
 * @param T The type of element being matched, which must be both an IrElement and an IrAnnotationContainer
 * @param type The IR type of the annotation to check for
 * @throws AssertionError if the element doesn't have the specified annotation
 */
fun <T> IrElementMatcher<T>.hasAnnotation(type: IrType) where T : IrElement, T : IrAnnotationContainer {
    assert(element.annotations.any { it.type == type }) {
        "Expected annotation of type ${type.render()} in:\n\n${element.renderIrTree()}\n"
    }
}

/**
 * Asserts that the element has an annotation with the given class ID.
 *
 * @param T The type of element being matched, which must be both an IrElement and an IrAnnotationContainer
 * @param id The ClassId of the annotation to check for
 * @return The result of calling [hasAnnotation] with the IR type corresponding to the given ClassId
 */
fun <T> IrElementMatcher<T>.hasAnnotation(id: ClassId) where T : IrElement, T : IrAnnotationContainer =
    hasAnnotation(type(id))

/**
 * Asserts that the element has a type parameter with the given name.
 *
 * @param name The name of the type parameter to check for
 * @throws AssertionError if the element doesn't have a type parameter with the specified name
 */
fun IrElementMatcher<out IrTypeParametersContainer>.hasTypeParameter(name: String) {
    assert(element.typeParameters.any { it.name.asString() == name }) {
        "Expected type parameter named '$name' in:\n\n${element.renderIrTree()}\n"
    }
}

/**
 * Asserts that the element has a type parameter at the given index.
 *
 * @param index The zero-based index of the type parameter to check for
 * @throws AssertionError if the element doesn't have a type parameter at the specified index
 */
fun IrElementMatcher<out IrTypeParametersContainer>.hasTypeParameter(index: Int) {
    assert(index in element.typeParameters.indices) {
        "Expected type parameter at index '$index' in:\n\n${element.renderIrTree()}\n"
    }
}

/**
 * Asserts that the element has the given name.
 *
 * @param name The name to check for
 * @throws AssertionError if the element doesn't have the specified name
 */
fun IrElementMatcher<out IrDeclarationWithName>.isNamed(name: String) {
    assert(element.name.asString() == name) {
        "No declaration named '$name' in $scopeName:\n\n${element.renderIrTree()}\n"
    }
}
