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

import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.diagnostics.DiagnosticContext
import org.jetbrains.kotlin.diagnostics.DiagnosticUtils
import org.jetbrains.kotlin.diagnostics.KtDiagnostic
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.diagnostics.impl.BaseDiagnosticsCollector
import org.jetbrains.kotlin.diagnostics.rendering.RootDiagnosticRendererFactory

internal class DelegatingDiagnosticsReporter(
    val messageCollector: MessageCollector
) : BaseDiagnosticsCollector() {
    override val diagnostics: List<KtDiagnostic>
        get() = emptyList()

    override val diagnosticsByFilePath: Map<String?, List<KtDiagnostic>>
        get() = emptyMap()

    override val hasErrors: Boolean
        get() = messageCollector.hasErrors()

    override val rawReporter: RawReporter = RawReporter { message, severity ->
        messageCollector.report(severity, message)
    }

    override fun report(
        diagnostic: KtDiagnostic?, context: DiagnosticContext
    ) {
        if (diagnostic == null) return
        val severity = when (diagnostic.severity) {
            Severity.INFO -> CompilerMessageSeverity.INFO
            Severity.WARNING -> CompilerMessageSeverity.WARNING
            Severity.ERROR -> CompilerMessageSeverity.ERROR
            Severity.FIXED_WARNING -> CompilerMessageSeverity.FIXED_WARNING
        }
        val message = RootDiagnosticRendererFactory(diagnostic).render(diagnostic)
        val location = diagnostic.textRanges.firstOrNull()?.let { range ->
            val lineAndColumn = DiagnosticUtils.getLineAndColumnInPsiFile(diagnostic.psiElement.containingFile, range)
            CompilerMessageLocation.create(
                path = context.containingFilePath,
                line = lineAndColumn.line,
                column = lineAndColumn.column,
                lineContent = lineAndColumn.lineContent
            )
        }
        messageCollector.report(severity, message, location)
    }
}