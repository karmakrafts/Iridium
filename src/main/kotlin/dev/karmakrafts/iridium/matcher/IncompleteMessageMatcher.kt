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
import dev.karmakrafts.iridium.util.CompilerMessageFilter
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity

@CompilerTestDsl
class IncompleteMessageMatcher @PublishedApi @TestOnly internal constructor() {
    internal var condition: CompilerMessageFilter = CompilerMessageFilter { true }
    internal val conditions: ArrayList<String> = ArrayList()

    @TestOnly
    fun anyMessage() {
        condition += CompilerMessageFilter { true }
        conditions += "match any message"
    }

    @TestOnly
    fun message(message: String) {
        condition += CompilerMessageFilter { it.message == message }
        conditions += "equal '$message'"
    }

    @TestOnly
    fun messageWith(messageSubstring: String) {
        condition += CompilerMessageFilter { messageSubstring in it.message }
        conditions += "contain '$messageSubstring'"
    }

    @TestOnly
    fun messageWithout(messageSubstring: String) {
        condition += CompilerMessageFilter { messageSubstring !in it.message }
        conditions += "do not contain '$messageSubstring'"
    }

    @TestOnly
    fun withSeverity(severity: CompilerMessageSeverity) {
        condition += CompilerMessageFilter { it.severity == severity }
        conditions += "have the severity $severity"
    }

    @TestOnly
    fun info() {
        condition += CompilerMessageFilter { it.severity == CompilerMessageSeverity.INFO }
        conditions += "report an information"
    }

    @TestOnly
    fun verbose() {
        condition += CompilerMessageFilter { it.severity == CompilerMessageSeverity.LOGGING }
        conditions += "report a verbose information"
    }

    @TestOnly
    fun error() {
        condition += CompilerMessageFilter { it.severity.isError }
        conditions += "report an error"
    }

    @TestOnly
    fun warning() {
        condition += CompilerMessageFilter { it.severity.isWarning }
        conditions += "report a warning"
    }

    @TestOnly
    fun atLine(line: Int) {
        condition += CompilerMessageFilter { it.location?.line == line }
        conditions += "are at line $line"
    }

    @TestOnly
    fun inColumn(column: Int) {
        condition += CompilerMessageFilter { it.location?.column == column }
        conditions += "are in column $column"
    }

    @TestOnly
    fun fromFile(file: String) {
        condition += CompilerMessageFilter { it.location?.path?.endsWith(file) == true }
        conditions += "are in file $file"
    }

    @TestOnly
    operator fun invoke(message: CompilerMessage): Boolean = condition(message)
}

typealias IncompleteMessageMatcherSpec = IncompleteMessageMatcher.() -> Unit