package dev.rambris.amigaamos.lang

import com.intellij.lang.Language
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.codeStyle.lineIndent.LineIndentProvider

class AmosLineIndentProvider : LineIndentProvider {
    override fun isSuitableFor(language: Language?): Boolean = language?.isKindOf(AmosLanguage) == true

    override fun getLineIndent(project: Project, editor: Editor, language: Language?, offset: Int): String? {
        if (language == null || !language.isKindOf(AmosLanguage)) {
            return null
        }

        val document = editor.document
        val line = document.getLineNumber(offset)
        val formatted = AmosCodeStyleFormatter.format(document.text)
        val formattedLines = formatted.splitToSequence(Regex("\\r\\n|\\n|\\r")).toList()
        if (line >= formattedLines.size) {
            return null
        }

        val lineText = formattedLines[line]
        return lineText.takeWhile { it == ' ' || it == '\t' }
    }
}


