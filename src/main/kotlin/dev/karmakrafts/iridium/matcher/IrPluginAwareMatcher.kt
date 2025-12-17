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

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.IrBuiltIns

/**
 * Base interface for matchers that need access to the Kotlin IR plugin context.
 *
 * This interface provides access to the IR plugin context and built-in IR types,
 * which are essential for working with Kotlin's IR (Intermediate Representation)
 * in compiler plugins and tests. It serves as a foundation for more specialized
 * matchers like [IrTypeAwareMatcher] and [IrElementMatcher].
 *
 * Implementations of this interface can use the plugin context to access compiler
 * services and the IR built-ins to work with Kotlin's type system in the IR phase.
 */
interface IrPluginAwareMatcher {
    /**
     * The IR plugin context that provides access to compiler services and type references.
     */
    val pluginContext: IrPluginContext

    /**
     * Convenient access to IR built-in types.
     *
     * This property delegates to [IrPluginContext.irBuiltIns] and provides access to
     * built-in Kotlin types in their IR representation.
     */
    val types: IrBuiltIns get() = pluginContext.irBuiltIns
}
