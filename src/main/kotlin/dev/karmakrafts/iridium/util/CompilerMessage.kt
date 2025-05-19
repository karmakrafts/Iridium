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

package dev.karmakrafts.iridium.util

import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation

@ConsistentCopyVisibility
data class CompilerMessage @TestOnly internal constructor( // @formatter:off
    val severity: CompilerMessageSeverity,
    val message: String,
    val location: CompilerMessageSourceLocation?
) // @formatter:on

typealias RawCompilerMessageCallback = (CompilerMessage) -> Unit

@Suppress("NOTHING_TO_INLINE")
@JvmInline
value class CompilerMessageCallback(val value: RawCompilerMessageCallback) {
    @PublishedApi
    @TestOnly
    internal inline operator fun plus(other: CompilerMessageCallback): CompilerMessageCallback =
        CompilerMessageCallback { message ->
            value(message)
            other(message)
        }

    @PublishedApi
    @TestOnly
    internal inline operator fun invoke(message: CompilerMessage) = value(message)
}

typealias RawCompilerMessageFilter = (CompilerMessage) -> Boolean

@Suppress("NOTHING_TO_INLINE")
@JvmInline
value class CompilerMessageFilter(val value: RawCompilerMessageFilter) {
    @PublishedApi
    @TestOnly
    internal inline operator fun plus(other: CompilerMessageFilter): CompilerMessageFilter =
        CompilerMessageFilter { message ->
            value(message) && other(message)
        }

    @PublishedApi
    @TestOnly
    internal inline operator fun invoke(message: CompilerMessage): Boolean = value(message)
}