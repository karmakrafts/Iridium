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

package dev.karmakrafts.iridium.pipeline

import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.js.JsPlatforms
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.platform.konan.NativePlatforms
import org.jetbrains.kotlin.platform.wasm.WasmPlatforms

/**
 * The target for which to compile Kotlin code during a compiler pipeline invocation.
 * Defines environment config and target platform.
 */
enum class CompilerTarget( // @formatter:off
    val environmentConfigFiles: EnvironmentConfigFiles,
    val targetPlatform: TargetPlatform
) { // @formatter:on
    // @formatter:off
    JVM   (EnvironmentConfigFiles.JVM_CONFIG_FILES, JvmPlatforms.defaultJvmPlatform),
    NATIVE(EnvironmentConfigFiles.NATIVE_CONFIG_FILES, NativePlatforms.unspecifiedNativePlatform),
    JS    (EnvironmentConfigFiles.JS_CONFIG_FILES, JsPlatforms.defaultJsPlatform),
    WASM  (EnvironmentConfigFiles.WASM_CONFIG_FILES, WasmPlatforms.wasmJs)
    // @formatter:on
}