/*
 * Copyright 2026 Karma Krafts
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
import org.jetbrains.kotlin.ir.declarations.IrFile
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
public class IrTypeMatcher<TYPE : IrType> @PublishedApi internal constructor( // @formatter:off
    private val scopeName: String,
    private val depth: Int,
    private val type: TYPE,
    private val parentElement: IrElement,
    override val pluginContext: IrPluginContext
) : IrTypeAwareMatcher() { // @formatter:on
    override val sourceFile: IrFile? by lazy { parentElement.sourceFile() }

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
    public fun type(expected: IrType) {
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
    public fun strictType(expected: IrType) {
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
    public fun typeParameter(name: String) {
        assert(type.isTypeParameter() && (type.classifierOrFail as IrTypeParameterSymbol).owner.name.asString() == name) {
            "Expected type parameter '$name' but got ${type.render()} in:\n\n${parentElement.renderIrTree()}\n"
        }
    }

    /**
     * Asserts that the current type is nullable.
     *
     * @throws AssertionError if the type is not nullable, with a detailed error message showing the IR tree
     */
    public fun nullable() {
        assert(type.isNullable()) {
            "Expected type to be nullable but got ${type.render()} in:\n\n${parentElement.renderIrTree()}\n"
        }
    }

    /**
     * Asserts that the current type is non-null (not nullable).
     *
     * @throws AssertionError if the type is nullable, with a detailed error message showing the IR tree
     */
    public fun nonNull() {
        assert(!type.isNullable()) {
            "Expected type to be nonnull but got ${type.render()} in:\n\n${parentElement.renderIrTree()}\n"
        }
    }

    public fun unit(): Unit = type(types.unitType)
    public fun any(): Unit = type(types.anyType)
    public fun nothing(): Unit = type(types.nothingType)

    public fun string(): Unit = type(types.stringType)
    public fun stringArray(): Unit = type(types.stringType.array())

    public fun function(): Unit = type(types.functionClass.defaultType)
    public fun kFunction(): Unit = type(types.kFunctionClass.defaultType)
    public fun kProperty(): Unit = type(types.kPropertyClass.defaultType)

    public fun byte(): Unit = type(types.byteType)
    public fun short(): Unit = type(types.shortType)
    public fun int(): Unit = type(types.intType)
    public fun long(): Unit = type(types.longType)

    public fun uByte(): Unit = type(type(UnsignedType.UBYTE))
    public fun uShort(): Unit = type(type(UnsignedType.USHORT))
    public fun uInt(): Unit = type(type(UnsignedType.UINT))
    public fun uLong(): Unit = type(type(UnsignedType.ULONG))

    public fun float(): Unit = type(types.floatType)
    public fun double(): Unit = type(types.doubleType)
    public fun char(): Unit = type(types.charType)
    public fun boolean(): Unit = type(types.booleanType)

    public fun byteArray(): Unit = type(types.byteType.array())
    public fun shortArray(): Unit = type(types.shortType.array())
    public fun intArray(): Unit = type(types.intType.array())
    public fun longArray(): Unit = type(types.longType.array())

    public fun uByteArray(): Unit = type(type(UnsignedType.UBYTE).array())
    public fun uShortArray(): Unit = type(type(UnsignedType.USHORT).array())
    public fun uIntArray(): Unit = type(type(UnsignedType.UINT).array())
    public fun uLongArray(): Unit = type(type(UnsignedType.ULONG).array())

    public fun floatArray(): Unit = type(types.floatType.array())
    public fun doubleArray(): Unit = type(types.doubleType.array())
    public fun charArray(): Unit = type(types.charType.array())
    public fun booleanArray(): Unit = type(types.booleanType.array())
}
