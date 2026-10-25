/*
 * Copyright 2026 Karma Krafts
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

import org.jetbrains.kotlin.KtPsiSourceElement
import org.jetbrains.kotlin.KtSourceFile
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.diagnostics.DiagnosticContext
import org.jetbrains.kotlin.diagnostics.KtDiagnostic
import org.jetbrains.kotlin.diagnostics.KtDiagnosticWithSource
import org.jetbrains.kotlin.diagnostics.PsiDiagnosticUtils
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.diagnostics.impl.BaseDiagnosticsCollector

internal class DelegatingDiagnosticsReporter(
    val messageCollector: MessageCollector
) : BaseDiagnosticsCollector() {
    override val diagnostics: List<KtDiagnostic>
        get() = emptyList()

    override val diagnosticsByFile: Map<KtSourceFile?, List<KtDiagnostic>>
        get() = emptyMap()

    override val hasErrors: Boolean
        get() = messageCollector.hasErrors()

    override val hasWarningsForWError: Boolean
        get() = false

    override fun report( // @formatter:off
        diagnostic: KtDiagnostic?,
        context: DiagnosticContext
    ) { // @formatter:on
        if (diagnostic == null) return
        val severity = when (diagnostic.severity) {
            Severity.INFO -> CompilerMessageSeverity.INFO
            Severity.WARNING -> CompilerMessageSeverity.WARNING
            Severity.ERROR -> CompilerMessageSeverity.ERROR
            Severity.FIXED_WARNING -> CompilerMessageSeverity.FIXED_WARNING
            Severity.STRONG_WARNING -> CompilerMessageSeverity.STRONG_WARNING
        }
        val sourceElement = (diagnostic as? KtDiagnosticWithSource)?.element as? KtPsiSourceElement
        if (sourceElement == null) {
            messageCollector.report(severity, diagnostic.renderMessage())
            return
        }
        val location = diagnostic.textRanges.firstOrNull()?.let { range ->
            val lineAndColumn = PsiDiagnosticUtils.offsetToLineAndColumn(
                sourceElement.psi.containingFile.viewProvider.document, range.startOffset
            )
            CompilerMessageLocation.create(
                path = context.containingFilePath,
                line = lineAndColumn.line,
                column = lineAndColumn.column,
                lineContent = lineAndColumn.lineContent
            )
        }
        messageCollector.report(severity, diagnostic.renderMessage(), location)
    }
}