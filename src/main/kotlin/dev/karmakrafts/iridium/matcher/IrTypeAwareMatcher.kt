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

import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.builtins.UnsignedType
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.makeNotNull
import org.jetbrains.kotlin.ir.types.makeNullable
import org.jetbrains.kotlin.ir.types.starProjectedType
import org.jetbrains.kotlin.ir.util.toArrayOrPrimitiveArrayType
import org.jetbrains.kotlin.name.ClassId

/**
 * Abstract base class for matchers that work with Kotlin IR types.
 *
 * This class extends [IrPluginAwareMatcher] and provides utility methods for creating and
 * manipulating IR types in compiler tests. It serves as a foundation for more specialized
 * matchers like [IrTypeMatcher] that need to work with Kotlin's type system in the IR phase.
 *
 * The utility methods allow for:
 * - Creating IR types from class names, ClassId objects, primitive types, and unsigned types
 * - Converting types to array types
 * - Making types nullable or non-null
 */
abstract class IrTypeAwareMatcher : BasicAssertionScope(), IrPluginAwareMatcher {
    /**
     * Creates an IR type from a fully qualified class name.
     *
     * @param name The fully qualified name of the class (e.g., "kotlin.String")
     * @return The IR simple type representing the specified class
     */
    fun type(name: String): IrSimpleType = pluginContext.referenceClass(ClassId.fromString(name))!!.starProjectedType

    /**
     * Creates an IR type from a ClassId.
     *
     * @param id The ClassId object representing the class
     * @return The IR simple type representing the specified class
     */
    fun type(id: ClassId): IrSimpleType = pluginContext.referenceClass(id)!!.starProjectedType

    /**
     * Creates an IR type from a primitive type.
     *
     * @param primitive The PrimitiveType to convert to an IR type
     * @return The IR type representing the specified primitive type
     */
    fun type(primitive: PrimitiveType): IrType = types.primitiveTypeToIrType[primitive]!!

    /**
     * Creates an IR type from an unsigned type.
     *
     * @param unsigned The UnsignedType to convert to an IR type
     * @return The IR type representing the specified unsigned type
     */
    fun type(unsigned: UnsignedType): IrType = pluginContext.referenceClass(unsigned.classId)!!.defaultType

    /**
     * Converts an IR type to its array type.
     *
     * @return The array type of the receiver type
     */
    fun IrType.array(): IrType = toArrayOrPrimitiveArrayType(types)

    /**
     * Makes an IR type nullable.
     *
     * @return The nullable version of the receiver type
     */
    fun IrType.nullable(): IrType = makeNullable()

    /**
     * Makes an IR type non-null.
     *
     * @return The non-null version of the receiver type
     */
    fun IrType.nonNull(): IrType = makeNotNull()
}
