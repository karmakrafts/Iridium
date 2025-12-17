/*
 * Copyright 2025 Karma Krafts
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

import dev.karmakrafts.iridium.CompilerAssertionDsl
import dev.karmakrafts.iridium.util.renderIrTree
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.builtins.UnsignedType
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classifierOrFail
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.util.isNullable
import org.jetbrains.kotlin.ir.util.isTypeParameter
import org.jetbrains.kotlin.ir.util.render

/**
 * A matcher for IR types that provides a DSL for asserting properties of IR types in compiler tests.
 *
 * This class is used to verify that IR types in the compiler's intermediate representation match
 * expected types. It provides methods for checking against common Kotlin types (primitives, arrays,
 * standard library types) as well as custom type parameters.
 *
 * Example usage:
 * ```
 * type matches {
 *     int() // Asserts the type is Int
 * }
 * ```
 *
 * @param TYPE The specific IrType subclass being matched
 */

@OptIn(UnsafeDuringIrConstructionAPI::class)
@CompilerAssertionDsl
class IrTypeMatcher<TYPE : IrType> @PublishedApi internal constructor( // @formatter:off
    private val scopeName: String,
    private val depth: Int,
    private val type: TYPE,
    private val parentElement: IrElement,
    override val pluginContext: IrPluginContext
) : IrTypeAwareMatcher() { // @formatter:on
    override val assertionContext: String by lazy {
        "Assertion failed in $scopeName (${type::class.java.simpleName}/$depth)\n${
            type.render()
        }"
    }

    /**
     * Asserts that the underlying type of the current type matches the expected type.
     * This performs a comparison on the type ignoring nullability.
     *
     * @param expected The expected IR type to match against
     * @throws AssertionError if the types don't match, with a detailed error message showing the IR tree
     */
    fun type(expected: IrType) {
        assert(type.type == expected) {
            "Expected type ${expected.render()} but got ${type.render()} in:\n\n${parentElement.renderIrTree()}\n"
        }
    }

    /**
     * Asserts that the current type strictly equals the expected type.
     * This performs a direct equality check on the type including nullability.
     *
     * @param expected The expected IR type to strictly match against
     * @throws AssertionError if the types don't match exactly, with a detailed error message showing the IR tree
     */
    fun strictType(expected: IrType) {
        assert(type == expected) {
            "Expected type ${expected.render()} but got ${type.render()} in:\n\n${parentElement.renderIrTree()}\n"
        }
    }

    /**
     * Asserts that the current type is a type parameter with the specified name.
     *
     * @param name The expected name of the type parameter
     * @throws AssertionError if the type is not a type parameter or has a different name
     */
    fun typeParameter(name: String) {
        assert(type.isTypeParameter() && (type.classifierOrFail as IrTypeParameterSymbol).owner.name.asString() == name) {
            "Expected type parameter '$name' but got ${type.render()} in:\n\n${parentElement.renderIrTree()}\n"
        }
    }

    /**
     * Asserts that the current type is nullable.
     *
     * @throws AssertionError if the type is not nullable, with a detailed error message showing the IR tree
     */
    fun nullable() {
        assert(type.isNullable()) {
            "Expected type to be nullable but got ${type.render()} in:\n\n${parentElement.renderIrTree()}\n"
        }
    }

    /**
     * Asserts that the current type is non-null (not nullable).
     *
     * @throws AssertionError if the type is nullable, with a detailed error message showing the IR tree
     */
    fun nonNull() {
        assert(!type.isNullable()) {
            "Expected type to be nonnull but got ${type.render()} in:\n\n${parentElement.renderIrTree()}\n"
        }
    }

    fun unit() = type(types.unitType)
    fun any() = type(types.anyType)
    fun nothing() = type(types.nothingType)

    fun string() = type(types.stringType)
    fun stringArray() = type(types.stringType.array())

    fun function() = type(types.functionClass.defaultType)
    fun kFunction() = type(types.kFunctionClass.defaultType)
    fun kProperty() = type(types.kPropertyClass.defaultType)

    fun byte() = type(types.byteType)
    fun short() = type(types.shortType)
    fun int() = type(types.intType)
    fun long() = type(types.longType)

    fun uByte() = type(type(UnsignedType.UBYTE))
    fun uShort() = type(type(UnsignedType.USHORT))
    fun uInt() = type(type(UnsignedType.UINT))
    fun uLong() = type(type(UnsignedType.ULONG))

    fun float() = type(types.floatType)
    fun double() = type(types.doubleType)
    fun char() = type(types.charType)
    fun boolean() = type(types.booleanType)

    fun byteArray() = type(types.byteType.array())
    fun shortArray() = type(types.shortType.array())
    fun intArray() = type(types.intType.array())
    fun longArray() = type(types.longType.array())

    fun uByteArray() = type(type(UnsignedType.UBYTE).array())
    fun uShortArray() = type(type(UnsignedType.USHORT).array())
    fun uIntArray() = type(type(UnsignedType.UINT).array())
    fun uLongArray() = type(type(UnsignedType.ULONG).array())

    fun floatArray() = type(types.floatType.array())
    fun doubleArray() = type(types.doubleType.array())
    fun charArray() = type(types.charType.array())
    fun booleanArray() = type(types.booleanType.array())
}
