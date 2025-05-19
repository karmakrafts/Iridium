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
import org.jetbrains.annotations.TestOnly

@CompilerTestDsl
@Suppress("NOTHING_TO_INLINE")
class MessageMatcher @PublishedApi @TestOnly internal constructor( // @formatter:off
    private val parentMatcher: IncompleteMessageMatcher
) { // @formatter:on
    @PublishedApi
    @get:TestOnly
    @set:TestOnly
    internal var range: IntRange = 1..Int.MAX_VALUE

    @TestOnly
    inline infix fun between(range: IntRange) {
        this.range = range
    }

    @TestOnly
    inline infix fun atLeast(count: Int) {
        range = count..range.last
    }

    @TestOnly
    inline infix fun atMost(count: Int) {
        range = range.first..count
    }

    @TestOnly
    inline infix fun exactly(count: Int) {
        range = count..count
    }

    @TestOnly
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