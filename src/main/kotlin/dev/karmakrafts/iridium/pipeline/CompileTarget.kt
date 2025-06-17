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

package dev.karmakrafts.iridium.pipeline

import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.config.jvmTarget
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.js.JsPlatforms
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.platform.konan.NativePlatforms
import oshi.PlatformEnum
import oshi.SystemInfo

private fun getNativeHostTarget(): KonanTarget {
    val arch = System.getProperty("os.arch")
    fun isAmd64(): Boolean = "x86_64" in arch || "amd64" in arch
    fun isArm64(): Boolean = "arm64" in arch || "aarch64" in arch
    fun isArm32(): Boolean = "arm" == arch || "armv7" in arch || "arm32" in arch
    return when (SystemInfo.getCurrentPlatform()) {
        PlatformEnum.LINUX -> when {
            isAmd64() -> KonanTarget.LINUX_X64
            isArm64() -> KonanTarget.LINUX_ARM64
            isArm32() -> KonanTarget.LINUX_ARM32_HFP
            else -> error("Unsupported Linux host platform")
        }

        PlatformEnum.MACOS -> when {
            isAmd64() -> KonanTarget.MACOS_X64
            isArm64() -> KonanTarget.MACOS_ARM64
            else -> error("Unsupported macOS host platform")
        }

        PlatformEnum.WINDOWS -> KonanTarget.MINGW_X64
        else -> error("Unsupported host platform")
    }
}

/**
 * Represents a target platform for Kotlin test compilations.
 */
enum class CompileTarget(private val platformProvider: (CompilerConfiguration) -> TargetPlatform) {
    // @formatter:off
    JVM     ({ JvmPlatforms.jvmPlatformByTargetVersion(it.jvmTarget ?: JvmTarget.DEFAULT) }),
    JS      ({ JsPlatforms.defaultJsPlatform }),
    NATIVE  ({ NativePlatforms.nativePlatformBySingleTarget(getNativeHostTarget()) });
    // @formatter:on

    internal operator fun invoke(config: CompilerConfiguration): TargetPlatform = platformProvider(config)
}
