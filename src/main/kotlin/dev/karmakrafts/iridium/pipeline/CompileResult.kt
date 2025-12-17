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

package dev.karmakrafts.iridium.pipeline

import dev.karmakrafts.iridium.util.CompilerMessage
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment

/**
 * Represents the result of a Kotlin compilation process.
 *
 * This class contains all the artifacts and information produced during compilation,
 * including the original source code, the FIR (Frontend IR) representation,
 * the IR (Intermediate Representation) module, compilation messages, and the plugin context.
 */
@ConsistentCopyVisibility
@CompilerPipelineDsl
data class CompileResult @PublishedApi internal constructor(
    /**
     * The original Kotlin source code that was compiled.
     */
    val source: String,

    /**
     * The FIR (Frontend IR) file representation of the compiled code.
     */
    val firFile: FirFile,

    /**
     * The IR module fragment containing the compiled code.
     */
    val module: IrModuleFragment,

    /**
     * List of compiler messages (warnings, errors, etc.) generated during compilation.
     */
    val messages: List<CompilerMessage>,

    /**
     * The IR plugin context that can be used by compiler plugins.
     */
    val pluginContext: IrPluginContext
)
