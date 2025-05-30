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

package dev.karmakrafts.iridium

import dev.karmakrafts.iridium.matcher.IncompleteMessageMatcher
import dev.karmakrafts.iridium.matcher.IncompleteMessageMatcherSpec
import dev.karmakrafts.iridium.matcher.MessageMatcher
import dev.karmakrafts.iridium.pipeline.CompileResult

/**
 * A class for asserting expectations about compiler diagnostic messages.
 *
 * This class is part of the compiler testing DSL and allows test authors to specify
 * which diagnostic messages should or should not be reported during compilation.
 */
@CompilerAssertionDsl
class CompilerAsserter internal constructor() {
    @PublishedApi
    internal val messageMatchers: ArrayList<MessageMatcher> = ArrayList()

    /**
     * Specifies that a particular diagnostic message should be reported during compilation.
     *
     * @param spec A specification that configures what kind of message to expect
     * @return A [MessageMatcher] that can be further configured with occurrence expectations
     */
    inline infix fun shouldReport(spec: IncompleteMessageMatcherSpec): MessageMatcher {
        val messageMatcher = IncompleteMessageMatcher()
        messageMatcher.spec()
        val matcher = MessageMatcher(messageMatcher)
        messageMatchers += matcher
        return matcher
    }

    /**
     * Specifies that a particular diagnostic message should not be reported during compilation.
     *
     * This is a convenience method equivalent to `shouldReport(spec) exactly 0`.
     *
     * @param spec A specification that configures what kind of message should not appear
     */
    inline infix fun shouldNotReport(crossinline spec: IncompleteMessageMatcherSpec) {
        shouldReport(spec) exactly 0
    }

    internal fun assert(result: CompileResult) {
        for (matcher in messageMatchers) {
            matcher(result.messages)
        }
    }

    fun reset() {
        messageMatchers.clear()
    }
}
