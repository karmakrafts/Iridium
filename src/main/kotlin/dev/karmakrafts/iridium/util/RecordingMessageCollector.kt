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

package dev.karmakrafts.iridium.util

import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi

/**
 * A message collector implementation that records compiler messages for later processing.
 * This class implements the [MessageCollector] interface from the Kotlin compiler API
 * and provides additional functionality for storing and processing compiler messages.
 *
 * @property callback An optional callback that is invoked whenever a message is reported
 */
@OptIn(ExperimentalAtomicApi::class)
class RecordingMessageCollector internal constructor(
    private val callback: CompilerMessageCallback = CompilerMessageCallback {}
) : MessageCollector {
    /**
     * A thread-safe queue containing all recorded compiler messages.
     */
    val messages: ConcurrentLinkedQueue<CompilerMessage> = ConcurrentLinkedQueue()

    /**
     * Atomic flag indicating whether any error messages have been reported.
     */
    private var hasErrors: AtomicBoolean = AtomicBoolean(false)

    /**
     * Clears all recorded messages and resets the error flag.
     */
    override fun clear() {
        messages.clear()
        hasErrors.store(false)
    }

    /**
     * Checks if any error messages have been reported.
     *
     * @return True if at least one error message has been reported, false otherwise
     */
    override fun hasErrors(): Boolean = hasErrors.load()

    /**
     * Reports a new compiler message.
     * The message is added to the queue, the callback is invoked, and the error flag
     * is updated if the message has error severity.
     *
     * @param severity The severity level of the message
     * @param message The textual content of the message
     * @param location Optional source location information
     */
    override fun report(
        severity: CompilerMessageSeverity, message: String, location: CompilerMessageSourceLocation?
    ) {
        val message = CompilerMessage(severity, message, location)
        messages += message
        callback(message)
        if (severity.isError) hasErrors.store(true)
    }

    /**
     * Prints all recorded error messages to standard error.
     * This method is intended for testing purposes only.
     */
    @TestOnly
    fun printErrors() {
        val errors = messages.filter { it.severity.isError }
        if (errors.isEmpty()) return
        System.err.println("========== RAW COMPILER OUTPUT ==========")
        for (error in errors) {
            val location = error.location
            if (location == null) {
                System.err.println("[${error.severity.name.first()}] ${error.message}")
                continue
            }
            val formattedLocation = "${location.path}:${location.line}:${location.column}"
            System.err.println("[${error.severity.name.first()}] ${error.message} ($formattedLocation)")
        }
        System.err.println("=========================================")
    }

    /**
     * Prints all recorded messages to standard out.
     * This method is intended for testing purposes only.
     */
    @TestOnly
    fun printAll() {
        println("========== RAW COMPILER OUTPUT ==========")
        for (message in messages) {
            val location = message.location
            if (location == null) {
                System.err.println("[${message.severity.name.first()}] ${message.message}")
                continue
            }
            val formattedLocation = "${location.path}:${location.line}:${location.column}"
            System.err.println("[${message.severity.name.first()}] ${message.message} ($formattedLocation)")
        }
        println("=========================================")
    }
}
