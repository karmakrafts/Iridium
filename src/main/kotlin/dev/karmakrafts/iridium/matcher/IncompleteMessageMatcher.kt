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

import dev.karmakrafts.iridium.CompilerAssertionDsl
import dev.karmakrafts.iridium.util.CompilerMessage
import dev.karmakrafts.iridium.util.CompilerMessageFilter
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity

/**
 * A matcher for compiler messages that provides a DSL for asserting properties of compiler messages.
 * This class is used in compiler tests to verify the content, severity, location, and other properties
 * of compiler messages produced during compilation.
 *
 * The matcher accumulates conditions that are used to filter compiler messages, and provides
 * a human-readable description of these conditions for use in assertion messages.
 */
@CompilerAssertionDsl
class IncompleteMessageMatcher @PublishedApi internal constructor() {
    internal var condition: CompilerMessageFilter = CompilerMessageFilter { true }
    internal val conditions: ArrayList<String> = ArrayList()

    /**
     * Matches any compiler message regardless of content or properties.
     * This is useful when you want to assert that any message exists.
     */
    fun anyMessage() {
        condition += CompilerMessageFilter { true }
        conditions += "match any message"
    }

    /**
     * Matches compiler messages with exactly the specified message text.
     *
     * @param message The exact message text to match
     */
    fun message(message: String) {
        condition += CompilerMessageFilter { it.message == message }
        conditions += "equal '$message'"
    }

    /**
     * Matches compiler messages that contain the specified substring.
     *
     * @param messageSubstring The substring to look for in the message text
     */
    fun messageWith(messageSubstring: String) {
        condition += CompilerMessageFilter { messageSubstring in it.message }
        conditions += "contain '$messageSubstring'"
    }

    /**
     * Matches compiler messages that do not contain the specified substring.
     *
     * @param messageSubstring The substring that should not be present in the message text
     */
    fun messageWithout(messageSubstring: String) {
        condition += CompilerMessageFilter { messageSubstring !in it.message }
        conditions += "do not contain '$messageSubstring'"
    }

    /**
     * Matches compiler messages with the specified severity.
     *
     * @param severity The compiler message severity to match
     */
    fun withSeverity(severity: CompilerMessageSeverity) {
        condition += CompilerMessageFilter { it.severity == severity }
        conditions += "have the severity $severity"
    }

    /**
     * Matches compiler messages with INFO severity.
     * These are informational messages that don't indicate problems.
     */
    fun info() {
        condition += CompilerMessageFilter { it.severity == CompilerMessageSeverity.INFO }
        conditions += "report an information"
    }

    /**
     * Matches compiler messages with LOGGING severity.
     * These are verbose logging messages typically used for debugging.
     */
    fun verbose() {
        condition += CompilerMessageFilter { it.severity == CompilerMessageSeverity.LOGGING }
        conditions += "report a verbose information"
    }

    /**
     * Matches compiler messages with ERROR severity.
     * These indicate compilation errors that prevent successful compilation.
     */
    fun error() {
        condition += CompilerMessageFilter { it.severity.isError }
        conditions += "report an error"
    }

    /**
     * Matches compiler messages with WARNING severity.
     * These indicate potential problems that don't prevent compilation.
     */
    fun warning() {
        condition += CompilerMessageFilter { it.severity.isWarning }
        conditions += "report a warning"
    }

    /**
     * Matches compiler messages that are reported at the specified line number.
     *
     * @param line The line number to match
     */
    fun atLine(line: Int) {
        condition += CompilerMessageFilter { it.location?.line == line }
        conditions += "are at line $line"
    }

    /**
     * Matches compiler messages that are reported at the specified column number.
     *
     * @param column The column number to match
     */
    fun inColumn(column: Int) {
        condition += CompilerMessageFilter { it.location?.column == column }
        conditions += "are in column $column"
    }

    /**
     * Matches compiler messages that are reported from a file with the specified name.
     * This matches files whose path ends with the specified string.
     *
     * @param file The file name or path suffix to match
     */
    fun fromFile(file: String) {
        condition += CompilerMessageFilter { it.location?.path?.endsWith(file) == true }
        conditions += "are in file $file"
    }

    /**
     * Checks if the given compiler message matches all the conditions defined in this matcher.
     * This operator allows the matcher to be used as a function.
     *
     * @param message The compiler message to check
     * @return true if the message matches all conditions, false otherwise
     */
    operator fun invoke(message: CompilerMessage): Boolean = condition(message)
}

/**
 * A type alias for a function that configures an IncompleteMessageMatcher.
 * This is used in the DSL to provide a concise way to specify message matching criteria.
 */
typealias IncompleteMessageMatcherSpec = IncompleteMessageMatcher.() -> Unit
