package dev.rambris.amigaamos.lang.amos

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import java.awt.datatransfer.StringSelection

class AmosCopyStringChainIntention : IntentionAction {
    override fun getText(): String = "Copy full string chain content"

    override fun getFamilyName(): String = "Amos string chains"

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile): Boolean {
        if (editor == null || file !is AmosFile) {
            return false
        }

        return AmosStringChainSupport.continuationVariableAtOffset(file.text, editor.caretModel.offset) != null
    }

    override fun invoke(project: Project, editor: Editor?, file: PsiFile) {
        if (editor == null || file !is AmosFile) {
            return
        }

        val source = file.text
        val offset = editor.caretModel.offset
        val chain = AmosStringChainSupport.chainAtOffset(source, offset)
            ?: AmosStringChainSupport.chainsInSource(source)
                .filter { offset >= it.endOffset }
                .lastOrNull { AmosStringChainSupport.continuationVariableAtOffset(source, offset) == it.variableName }
            ?: return

        CopyPasteManager.getInstance().setContents(StringSelection(chain.joinedContent()))
    }

    override fun startInWriteAction(): Boolean = false
}


