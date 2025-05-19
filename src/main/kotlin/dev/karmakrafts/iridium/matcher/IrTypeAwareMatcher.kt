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

abstract class IrTypeAwareMatcher : IrPluginAwareMatcher {
    fun type(name: String): IrSimpleType = pluginContext.referenceClass(ClassId.fromString(name))!!.starProjectedType
    fun type(id: ClassId): IrSimpleType = pluginContext.referenceClass(id)!!.starProjectedType
    fun type(primitive: PrimitiveType): IrType = types.primitiveTypeToIrType[primitive]!!
    fun type(unsigned: UnsignedType): IrType = pluginContext.referenceClass(unsigned.classId)!!.defaultType

    fun IrType.array(): IrType = toArrayOrPrimitiveArrayType(types)
    fun IrType.nullable(): IrType = makeNullable()
    fun IrType.nonNull(): IrType = makeNotNull()
}