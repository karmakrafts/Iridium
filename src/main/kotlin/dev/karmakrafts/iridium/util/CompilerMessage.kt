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

import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation

/**
 * Represents a compiler message with severity, content, and optional source location.
 * This class is used to encapsulate compiler diagnostic information in a structured way.
 *
 * @property severity The severity level of the compiler message (e.g., ERROR, WARNING, INFO)
 * @property message The textual content of the compiler message
 * @property location Optional source location information where the message originated
 */
@ConsistentCopyVisibility
data class CompilerMessage internal constructor( // @formatter:off
    val severity: CompilerMessageSeverity,
    val message: String,
    val location: CompilerMessageSourceLocation?
) // @formatter:on

/**
 * A function type that receives a [CompilerMessage] and performs some action with it.
 * Used as a raw callback mechanism for compiler message handling.
 */
typealias RawCompilerMessageCallback = (CompilerMessage) -> Unit

/**
 * A wrapper around [RawCompilerMessageCallback] that provides additional functionality.
 * This value class allows for composition of callbacks through the plus operator.
 *
 * @property value The underlying raw callback function
 */
@Suppress("NOTHING_TO_INLINE")
@JvmInline
value class CompilerMessageCallback(val value: RawCompilerMessageCallback) {
    @PublishedApi
    internal inline operator fun plus(other: CompilerMessageCallback): CompilerMessageCallback =
        CompilerMessageCallback { message ->
            value(message)
            other(message)
        }

    @PublishedApi
    internal inline operator fun invoke(message: CompilerMessage) = value(message)
}

/**
 * A function type that evaluates a [CompilerMessage] and returns a boolean result.
 * Used as a filtering mechanism for compiler messages.
 */
typealias RawCompilerMessageFilter = (CompilerMessage) -> Boolean

/**
 * A wrapper around [RawCompilerMessageFilter] that provides additional functionality.
 * This value class allows for composition of filters through the plus operator,
 * where multiple filters are combined with logical AND.
 *
 * @property value The underlying raw filter function
 */
@Suppress("NOTHING_TO_INLINE")
@JvmInline
value class CompilerMessageFilter(val value: RawCompilerMessageFilter) {
    @PublishedApi
    internal inline operator fun plus(other: CompilerMessageFilter): CompilerMessageFilter =
        CompilerMessageFilter { message ->
            value(message) && other(message)
        }

    @PublishedApi
    internal inline operator fun invoke(message: CompilerMessage): Boolean = value(message)
}
