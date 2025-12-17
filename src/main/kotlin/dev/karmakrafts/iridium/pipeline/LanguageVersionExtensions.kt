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

import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.config.ApiVersion
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl

/**
 * Creates a [LanguageVersionSettingsImpl] with the given language version and API version.
 *
 * This extension function provides a convenient way to create language version settings
 * by combining a language version with an API version using an infix notation.
 *
 * Example usage:
 * ```
 * val settings = LanguageVersion.KOTLIN_1_9 withApi ApiVersion.KOTLIN_1_9
 * ```
 *
 * @param version The API version to use
 * @return A new [LanguageVersionSettingsImpl] with the specified language and API versions
 */
@TestOnly
infix fun LanguageVersion.withApi(version: ApiVersion): LanguageVersionSettingsImpl = LanguageVersionSettingsImpl(
    languageVersion = this, apiVersion = version
)
