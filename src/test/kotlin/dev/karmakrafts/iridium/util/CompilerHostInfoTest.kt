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

import org.jetbrains.kotlin.konan.util.ArchiveType
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CompilerHostInfoTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `Download missing Gradle KLIB`() {
        val gradleHome = tempDir.resolve("gradle")
        var downloadedUri: URI? = null
        var downloadedPath: Path? = null

        val result = CompilerHostInfo.resolveGradleKlib(
            moduleName = "kotlin-stdlib-js",
            fileName = "kotlin-stdlib-js-test.klib",
            compilerVersion = "test",
            gradleHome = gradleHome
        ) { uri, destination ->
            downloadedUri = uri
            downloadedPath = destination
            destination.parent.createDirectories()
            destination.writeText("klib")
        }

        assertEquals(
            URI("https://repo.maven.apache.org/maven2/org/jetbrains/kotlin/kotlin-stdlib-js/test/kotlin-stdlib-js-test.klib"),
            downloadedUri
        )
        assertEquals(
            gradleHome.resolve("caches/iridium/org.jetbrains.kotlin/kotlin-stdlib-js/test/kotlin-stdlib-js-test.klib"),
            downloadedPath
        )
        assertEquals(downloadedPath, result)
        assertTrue(Files.isRegularFile(result))
    }

    @Test
    fun `Reuse KLIB from Gradle module cache`() {
        val gradleHome = tempDir.resolve("gradle")
        val cachedKlib = gradleHome.resolve(
            "caches/modules-2/files-2.1/org.jetbrains.kotlin/kotlin-stdlib-js/test/hash/kotlin-stdlib-js-test.klib"
        )
        cachedKlib.parent.createDirectories()
        cachedKlib.writeText("klib")

        val result = CompilerHostInfo.resolveGradleKlib(
            moduleName = "kotlin-stdlib-js",
            fileName = "kotlin-stdlib-js-test.klib",
            compilerVersion = "test",
            gradleHome = gradleHome
        ) { _, _ -> error("Cached dependencies must not be downloaded again") }

        assertEquals(cachedKlib, result)
    }

    @Test
    fun `Download and extract missing Konan distribution`() {
        val konanHome = tempDir.resolve("konan")
        val distributionName = "kotlin-native-prebuilt-linux-x86_64-test"
        var downloadedUri: URI? = null
        var archivePath: Path? = null

        val result = CompilerHostInfo.resolveKonanPrebuilt(
            compilerVersion = "test",
            distributionOsPrefix = "linux",
            architectureSuffix = "x86_64",
            archiveType = ArchiveType.TAR_GZ,
            konanHome = konanHome,
            download = { uri, destination ->
                downloadedUri = uri
                archivePath = destination
                destination.parent.createDirectories()
                destination.writeText("archive")
            },
            extract = { archive, destination, type ->
                assertEquals(archivePath, archive)
                assertEquals(ArchiveType.TAR_GZ, type)
                destination.resolve(distributionName).resolve("klib/common/stdlib").createDirectories()
            })

        assertEquals(
            URI("https://github.com/JetBrains/kotlin/releases/download/vtest/$distributionName.tar.gz"), downloadedUri
        )
        assertEquals(konanHome.resolve(distributionName), result)
        assertTrue(Files.isDirectory(result.resolve("klib/common/stdlib")))
        assertFalse(Files.exists(requireNotNull(archivePath)))
    }

    @Test
    fun `Reject incomplete Konan distribution`() {
        val error = assertFailsWith<IllegalStateException> {
            CompilerHostInfo.resolveKonanPrebuilt(
                compilerVersion = "test",
                distributionOsPrefix = "linux",
                architectureSuffix = "x86_64",
                archiveType = ArchiveType.TAR_GZ,
                konanHome = tempDir.resolve("konan"),
                download = { _, destination ->
                    destination.parent.createDirectories()
                    destination.writeText("archive")
                },
                extract = { _, _, _ -> })
        }

        assertTrue(error.message.orEmpty().contains("Kotlin/Native distribution"))
    }
}