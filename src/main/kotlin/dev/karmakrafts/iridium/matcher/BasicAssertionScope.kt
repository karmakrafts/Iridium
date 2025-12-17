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

/**
 * Base interface for assertion scopes in compiler testing.
 * Provides context information for assertions to improve error messages.
 */
@CompilerAssertionDsl
abstract class BasicAssertionScope internal constructor() {
    /**
     * The context string that will be included in assertion error messages.
     */
    abstract val assertionContext: String

    /**
     * Asserts that the receiver object equals the expected value.
     *
     * @param T The type of objects being compared
     * @param expected The value that the receiver should equal
     * @throws AssertionError if the receiver does not equal the expected value
     */
    infix fun <T> T.shouldBe(expected: T) {
        if (this == expected) return
        throw AssertionError("${assertionContext}\n\nExpected <$expected> but got <$this>")
    }

    /**
     * Asserts that the receiver object does not equal the unexpected value.
     *
     * @param T The type of objects being compared
     * @param unexpected The value that the receiver should not equal
     * @throws AssertionError if the receiver equals the unexpected value
     */
    infix fun <T> T.shouldNotBe(unexpected: T) {
        if (this != unexpected) return
        throw AssertionError("${assertionContext}\n\nDid not expect <$unexpected> but got <$this>")
    }

    /**
     * Asserts that the receiver collection contains all elements from the expected collection.
     *
     * @param T The type of elements in the collections
     * @param expected The collection whose elements should all be present in the receiver
     * @throws AssertionError if the receiver does not contain all elements from the expected collection
     */
    infix fun <T> Collection<T>.shouldContain(expected: Collection<T>) {
        if (containsAll(expected)) return
        throw AssertionError("${assertionContext}\n\nExpected <$this> to contain <$expected>")
    }

    /**
     * Asserts that the receiver collection does not contain all elements from the unexpected collection.
     *
     * @param T The type of elements in the collections
     * @param unexpected The collection whose elements should not all be present in the receiver
     * @throws AssertionError if the receiver contains all elements from the unexpected collection
     */
    infix fun <T> Collection<T>.shouldNotContain(unexpected: Collection<T>) {
        if (!containsAll(unexpected)) return
        throw AssertionError("${assertionContext}\n\nDid not expect <$this> to contain <$unexpected>")
    }
}