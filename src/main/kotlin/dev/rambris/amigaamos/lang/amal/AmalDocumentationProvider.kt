package dev.rambris.amigaamos.lang.amal

import com.intellij.lang.documentation.AbstractDocumentationProvider
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile

class AmalDocumentationProvider : AbstractDocumentationProvider() {
    private data class WordInfo(val text: String, val start: Int)

    override fun getCustomDocumentationElement(
        editor: Editor,
        file: PsiFile,
        contextElement: PsiElement?,
        targetOffset: Int
    ): PsiElement? {
        if (!isAmalFile(file)) {
            return null
        }
        val resolved = file.findElementAt(targetOffset) ?: contextElement ?: file
        resolved.putUserData(DOC_TARGET_OFFSET, targetOffset)
        return resolved
    }

    override fun generateDoc(element: PsiElement?, originalElement: PsiElement?): String? {
        val sourceElement = originalElement ?: element ?: return null
        val file = sourceElement.containingFile ?: return null
        if (!isAmalFile(file)) {
            return null
        }

        val offset = sourceElement.getUserData(DOC_TARGET_OFFSET) ?: sourceElement.textOffset
        val wordInfo = wordAt(file.text, offset)
        val entry = AmalDocumentationCatalog.resolveAtWord(wordInfo.text, offset - wordInfo.start) ?: return null
        return buildString {
            append("<h2>")
            append(escapeHtml(entry.symbol))
            append("</h2>")
            append("<p><b>kind:</b> ")
            append(escapeHtml(entry.kind))
            append("</p>")
            append("<p>")
            append(escapeHtml(entry.summary))
            append("</p>")
            append("<p><b>documentation:</b> <a href=\"")
            append(escapeHtml(entry.link))
            append("\">")
            append(escapeHtml(entry.link))
            append("</a></p>")
        }
    }

    override fun getQuickNavigateInfo(element: PsiElement?, originalElement: PsiElement?): String? {
        val sourceElement = originalElement ?: element ?: return null
        val file = sourceElement.containingFile ?: return null
        if (!isAmalFile(file)) {
            return null
        }

        val offset = sourceElement.getUserData(DOC_TARGET_OFFSET) ?: sourceElement.textOffset
        val wordInfo = wordAt(file.text, offset)
        val entry = AmalDocumentationCatalog.resolveAtWord(wordInfo.text, offset - wordInfo.start) ?: return null
        return "${entry.symbol} (${entry.kind})"
    }

    private fun wordAt(text: String, offset: Int): WordInfo {
        if (text.isEmpty()) {
            return WordInfo("", 0)
        }

        val safeOffset = offset.coerceIn(0, text.length - 1)
        if (!text[safeOffset].isLetterOrDigit() && text[safeOffset] != '_') {
            return WordInfo("", safeOffset)
        }

        var start = safeOffset
        while (start > 0 && (text[start - 1].isLetterOrDigit() || text[start - 1] == '_')) {
            start--
        }

        var end = safeOffset + 1
        while (end < text.length && (text[end].isLetterOrDigit() || text[end] == '_')) {
            end++
        }

        return WordInfo(text.substring(start, end), start)
    }

    private fun escapeHtml(value: String): String {
        return value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
    }

    private fun isAmalFile(file: PsiFile): Boolean {
        return file.language == AmalLanguage ||
            file.fileType == AmalFileType ||
            file.name.endsWith(".amal", ignoreCase = true)
    }

    companion object {
        private val DOC_TARGET_OFFSET = Key.create<Int>("AMAL_DOC_TARGET_OFFSET")
    }
}




