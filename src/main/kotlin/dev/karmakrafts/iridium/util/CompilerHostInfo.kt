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

package dev.karmakrafts.iridium.util

import org.jetbrains.kotlin.config.KotlinCompilerVersion
import org.jetbrains.kotlin.konan.util.ArchiveType
import org.jetbrains.kotlin.konan.util.DependencyDownloader
import org.jetbrains.kotlin.konan.util.DependencyExtractor
import oshi.util.PlatformEnum
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteIfExists
import kotlin.io.path.exists
import kotlin.io.path.isRegularFile
import kotlin.io.path.name
import kotlin.io.path.walk

internal object CompilerHostInfo {
    val osPrefix: String by lazy {
        when (PlatformEnum.getCurrentPlatform()) {
            PlatformEnum.WINDOWS -> "mingw"
            PlatformEnum.MACOS -> "macos"
            else -> "linux"
        }
    }

    val fullArchSuffix: String by lazy {
        when (System.getProperty("os.arch")) {
            "aarch64", "arm64" -> "aarch64"
            else -> "x86_64"
        }
    }

    val shortArchSuffix: String by lazy {
        when (System.getProperty("os.arch")) {
            "aarch64", "arm64" -> "arm64"
            else -> "x64"
        }
    }

    val distributionOsPrefix: String by lazy {
        when (PlatformEnum.getCurrentPlatform()) {
            PlatformEnum.WINDOWS -> "windows"
            PlatformEnum.MACOS -> "macos"
            else -> "linux"
        }
    }

    val konanArchiveType: ArchiveType by lazy {
        when (PlatformEnum.getCurrentPlatform()) {
            PlatformEnum.WINDOWS -> ArchiveType.ZIP
            else -> ArchiveType.TAR_GZ
        }
    }

    private val userHome: Path by lazy { Path.of(System.getProperty("user.home")) }

    val gradleHome: Path by lazy {
        System.getenv("GRADLE_USER_HOME")?.let(Path::of) ?: userHome.resolve(".gradle")
    }

    val konanHome: Path by lazy {
        System.getenv("KONAN_DATA_DIR")?.let(Path::of) ?: userHome.resolve(".konan")
    }

    val konanPrebuiltDir: Path by lazy {
        resolveKonanPrebuilt()
    }

    @Synchronized
    fun resolveGradleKlib(
        moduleName: String,
        fileName: String,
        compilerVersion: String = KotlinCompilerVersion.VERSION,
        gradleHome: Path = this.gradleHome,
        download: (URI, Path) -> Unit = ::downloadDependency
    ): Path {
        val moduleDir = gradleHome.resolve(
            "caches/modules-2/files-2.1/org.jetbrains.kotlin/$moduleName/$compilerVersion"
        )
        if (moduleDir.exists()) {
            moduleDir.walk().firstOrNull { it.isRegularFile() && it.name == fileName }?.let { return it }
        }

        val destination = gradleHome.resolve(
            "caches/iridium/org.jetbrains.kotlin/$moduleName/$compilerVersion/$fileName"
        )
        if (destination.isRegularFile()) return destination

        val source = URI(
            "https://repo.maven.apache.org/maven2/org/jetbrains/kotlin/$moduleName/$compilerVersion/$fileName"
        )
        destination.parent.createDirectories()
        download(source, destination)
        check(destination.isRegularFile()) {
            "Downloaded Kotlin compiler dependency is missing: $destination"
        }
        return destination
    }

    @Synchronized
    fun resolveKonanPrebuilt(
        compilerVersion: String = KotlinCompilerVersion.VERSION,
        distributionOsPrefix: String = this.distributionOsPrefix,
        architectureSuffix: String = fullArchSuffix,
        archiveType: ArchiveType = konanArchiveType,
        konanHome: Path = this.konanHome,
        download: (URI, Path) -> Unit = ::downloadDependency,
        extract: (Path, Path, ArchiveType) -> Unit = ::extractDependency
    ): Path {
        val distributionName = "kotlin-native-prebuilt-$distributionOsPrefix-$architectureSuffix-$compilerVersion"
        val distributionDir = konanHome.resolve(distributionName)
        if (Files.isDirectory(distributionDir.resolve("klib/common/stdlib"))) return distributionDir

        konanHome.createDirectories()
        val archive = konanHome.resolve("cache/$distributionName.${archiveType.fileExtension}")
        val source = URI(
            "https://github.com/JetBrains/kotlin/releases/download/v$compilerVersion/${archive.fileName}"
        )
        archive.parent.createDirectories()
        download(source, archive)
        try {
            extract(archive, konanHome, archiveType)
        }
        finally {
            archive.deleteIfExists()
        }
        check(Files.isDirectory(distributionDir.resolve("klib/common/stdlib"))) {
            "Kotlin/Native distribution was not extracted correctly: $distributionDir"
        }
        return distributionDir
    }

    private fun downloadDependency(source: URI, destination: Path) {
        DependencyDownloader(progressCallback = { _, _, _ -> }).download(source.toURL(), destination.toFile())
    }

    private fun extractDependency(archive: Path, destination: Path, archiveType: ArchiveType) {
        DependencyExtractor().extract(archive.toFile(), destination.toFile(), archiveType)
    }
}