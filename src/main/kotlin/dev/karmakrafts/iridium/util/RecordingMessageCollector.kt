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
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi

@PublishedApi
@OptIn(ExperimentalAtomicApi::class)
internal class RecordingMessageCollector @TestOnly constructor(
    private val callback: CompilerMessageCallback = CompilerMessageCallback {}
) : MessageCollector {
    val messages: ConcurrentLinkedQueue<CompilerMessage> = ConcurrentLinkedQueue()
    private var hasErrors: AtomicBoolean = AtomicBoolean(false)

    override fun clear() {
        messages.clear()
        hasErrors.store(false)
    }

    override fun hasErrors(): Boolean = hasErrors.load()

    override fun report(
        severity: CompilerMessageSeverity, message: String, location: CompilerMessageSourceLocation?
    ) {
        val message = CompilerMessage(severity, message, location)
        messages += message
        callback(message)
        if (severity.isError) hasErrors.store(true)
    }

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
}