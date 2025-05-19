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
import dev.karmakrafts.iridium.util.renderIrTree
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.builtins.UnsignedType
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classifierOrFail
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.util.isTypeParameter
import org.jetbrains.kotlin.ir.util.render

@OptIn(UnsafeDuringIrConstructionAPI::class)
@CompilerTestDsl
class IrTypeMatcher<TYPE : IrType> @PublishedApi @TestOnly internal constructor( // @formatter:off
    private val type: TYPE,
    private val parentElement: IrElement,
    override val pluginContext: IrPluginContext
) : IrTypeAwareMatcher() { // @formatter:on
    fun type(expected: IrType) {
        assert(type == expected) {
            "Expected type ${expected.render()} but got ${type.render()} in:\n\n${parentElement.renderIrTree()}\n"
        }
    }

    fun typeParameter(name: String) {
        assert(type.isTypeParameter() && (type.classifierOrFail as IrTypeParameterSymbol).owner.name.asString() == name) {
            "Expected type parameter '$name' but got ${type.render()} in:\n\n${parentElement.renderIrTree()}\n"
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