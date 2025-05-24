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

import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoot
import org.jetbrains.kotlin.config.CompilerConfiguration
import java.io.File

/**
 * Adds a JVM classpath root to the compiler configuration based on the location of a specified type.
 *
 * This extension function determines the location of the JAR file or directory containing
 * the specified type and adds it to the classpath of the compiler configuration.
 * This is useful for ensuring that the compiler can find and use classes from specific libraries.
 *
 * Example usage:
 * ```
 * compilerConfiguration.addJvmClasspathRootByType<MyClass>()
 * ```
 *
 * @param T The type whose location should be added to the classpath
 */
@TestOnly
inline fun <reified T : Any> CompilerConfiguration.addJvmClasspathRootByType() {
    addJvmClasspathRoot(File(T::class.java.protectionDomain.codeSource.location.toURI()))
}
