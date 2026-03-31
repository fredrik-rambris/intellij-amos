package dev.rambris.amigaamos.lang.amos

import com.intellij.lang.Language
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.codeStyle.lineIndent.LineIndentProvider

class AmosLineIndentProvider : LineIndentProvider {
    override fun isSuitableFor(language: Language?): Boolean = language?.isKindOf(AmosLanguage) == true

    override fun getLineIndent(project: Project, editor: Editor, language: Language?, offset: Int): String? {
        if (language == null || !language.isKindOf(AmosLanguage)) return null

        val document = editor.document
        val currentLine = document.getLineNumber(offset)
        if (currentLine == 0) return ""

        // Collect all lines preceding the new (empty) line
        val lines = (0 until currentLine).map { lineNum ->
            document.getText(TextRange(document.getLineStartOffset(lineNum), document.getLineEndOffset(lineNum)))
        }

        val level = AmosStatementSupport.computeIndentLevelAtLine(lines, currentLine)
        return " ".repeat(level * AmosStatementSupport.blockIndentSize)
    }
}
