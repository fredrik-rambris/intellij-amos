package dev.rambris.amigaamos.lang

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import java.awt.datatransfer.DataFlavor

class AmosPasteAsStringChainIntention : IntentionAction {
    override fun getText(): String = "Paste clipboard as string chain"

    override fun getFamilyName(): String = "Amos string chains"

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile): Boolean {
        if (editor == null || file !is AmosFile) {
            return false
        }

        if (AmosStringChainSupport.continuationVariableAtOffset(file.text, editor.caretModel.offset) == null) {
            return false
        }

        return clipboardText().isNotBlank()
    }

    override fun invoke(project: Project, editor: Editor?, file: PsiFile) {
        if (editor == null || file !is AmosFile) {
            return
        }

        val variableName = AmosStringChainSupport.continuationVariableAtOffset(file.text, editor.caretModel.offset) ?: return
        val text = clipboardText()
        if (text.isBlank()) {
            return
        }

        val normalized = text.replace("\r\n", "\n").replace('\r', '\n')
        val lines = normalized.split('\n')

        val snippet = lines.joinToString(separator = "\n") { line ->
            val escaped = line.replace("\"", "\"\"")
            "$variableName=$variableName+\"$escaped\""
        }

        val caretOffset = editor.caretModel.offset
        editor.document.insertString(caretOffset, snippet)
        editor.caretModel.moveToOffset(caretOffset + snippet.length)
    }

    override fun startInWriteAction(): Boolean = true

    private fun clipboardText(): String {
        val transferable = CopyPasteManager.getInstance().contents ?: return ""
        return runCatching { transferable.getTransferData(DataFlavor.stringFlavor) as? String }
            .getOrNull()
            .orEmpty()
    }
}


