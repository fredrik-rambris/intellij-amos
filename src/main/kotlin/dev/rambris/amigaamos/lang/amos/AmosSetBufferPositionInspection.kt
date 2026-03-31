package dev.rambris.amigaamos.lang.amos

import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiFile
import java.util.Locale

class AmosSetBufferPositionInspection : LocalInspectionTool() {
    override fun getDisplayName(): String = "Set Buffer should be first instruction"

    override fun getGroupDisplayName(): String = "AMOS"

    override fun getShortName(): String = "AmosSetBufferPosition"

    override fun checkFile(file: PsiFile, manager: InspectionManager, isOnTheFly: Boolean): Array<ProblemDescriptor>? {
        if (file !is AmosFile) {
            return null
        }

        val source = file.text
        val statements = AmosStatementSupport.splitStatements(source)
        val problems = mutableListOf<ProblemDescriptor>()
        var sawExecutable = false

        statements.forEach { range ->
            val raw = source.substring(range.startOffset, range.endOffset)
            val normalized = AmosStatementSupport.normalizeForAnalysis(range)
            val leadingTrim = raw.indexOf(normalized)
                .takeIf { it >= 0 }
                ?: raw.indexOfFirst { !it.isWhitespace() }.let { if (it < 0) raw.length else it }
            val text = normalized
            if (text.isEmpty()) {
                return@forEach
            }

            if (isRemStatement(text)) {
                return@forEach
            }

            if (isSetBufferStatement(text) && sawExecutable) {
                val problemRange = TextRange(range.startOffset + leadingTrim, range.endOffset)
                problems += manager.createProblemDescriptor(
                    file,
                    problemRange,
                    "Set Buffer must be the first instruction (except Rem lines)",
                    ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                    isOnTheFly
                )
            }

            sawExecutable = true
        }

        return problems.toTypedArray()
    }

    private fun isSetBufferStatement(text: String): Boolean {
        val normalized = text.uppercase(Locale.ROOT)
        return normalized.startsWith("SET BUFFER")
    }

    private fun isRemStatement(text: String): Boolean {
        val normalized = text.uppercase(Locale.ROOT)
        return normalized.startsWith("REM") || text.startsWith("'")
    }
}




