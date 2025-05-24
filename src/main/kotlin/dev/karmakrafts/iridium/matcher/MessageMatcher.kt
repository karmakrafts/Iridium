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

package dev.karmakrafts.iridium.matcher

import dev.karmakrafts.iridium.CompilerTestDsl
import dev.karmakrafts.iridium.util.CompilerMessage

/**
 * A matcher that asserts the number of compiler messages matching specific criteria.
 *
 * This class works in conjunction with [IncompleteMessageMatcher] to provide a fluent DSL
 * for asserting both the content and quantity of compiler messages. After specifying what
 * messages to match using [IncompleteMessageMatcher], this class allows specifying how many
 * such messages are expected.
 *
 * Example usage:
 * ```
 * compiler shouldReport {
 *     message("Type mismatch")
 *     error()
 *     atLine(10)
 * } between 1..3
 * ```
 */
@CompilerTestDsl
@Suppress("NOTHING_TO_INLINE")
class MessageMatcher @PublishedApi internal constructor( // @formatter:off
    private val parentMatcher: IncompleteMessageMatcher
) { // @formatter:on
    /**
     * The range of acceptable message counts. By default, expects at least one message.
     */
    @PublishedApi
    internal var range: IntRange = 1..Int.MAX_VALUE

    /**
     * Specifies that the number of matching messages should be within the given range.
     *
     * @param range The range of acceptable message counts
     */
    inline infix fun between(range: IntRange) {
        this.range = range
    }

    /**
     * Specifies that there should be at least the given number of matching messages.
     *
     * @param count The minimum number of expected messages
     */
    inline infix fun atLeast(count: Int) {
        range = count..range.last
    }

    /**
     * Specifies that there should be at most the given number of matching messages.
     *
     * @param count The maximum number of expected messages
     */
    inline infix fun atMost(count: Int) {
        range = range.first..count
    }

    /**
     * Specifies that there should be exactly the given number of matching messages.
     *
     * @param count The exact number of expected messages
     */
    inline infix fun exactly(count: Int) {
        range = count..count
    }

    /**
     * Checks if the number of matching messages in the given list is within the expected range.
     * If not, an assertion error is thrown with a descriptive message.
     *
     * @param messages The list of compiler messages to check
     * @throws AssertionError if the number of matching messages is not within the expected range
     */
    internal operator fun invoke(messages: List<CompilerMessage>) {
        val count = messages.count(parentMatcher::invoke)
        if (count in range) return

        val message = when {
            range.first != range.last -> if (range.last == Int.MAX_VALUE) "Expected ${range.first} or more compiler reports which"
            else "Expected between ${range.first} and ${range.last} compiler reports which"

            range.first == 0 -> "Expected no compiler reports which"
            else -> "Expected ${range.first} compiler reports which"
        } + ":\n${parentMatcher.conditions.joinToString("\n") { "\t→ $it" }}\n"

        assert(false) { message }
    }
}
