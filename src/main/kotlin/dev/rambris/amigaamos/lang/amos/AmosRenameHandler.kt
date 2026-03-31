package dev.rambris.amigaamos.lang.amos

import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.refactoring.rename.RenameHandler

class AmosRenameHandler : RenameHandler {
    override fun isAvailableOnDataContext(dataContext: DataContext): Boolean {
        val file = CommonDataKeys.PSI_FILE.getData(dataContext) as? AmosFile ?: return false
        val editor = CommonDataKeys.EDITOR.getData(dataContext) ?: return false
        return AmosRenameSupport.renameAtOffsetPreview(file, editor.caretModel.offset)
    }

    override fun isRenaming(dataContext: DataContext): Boolean {
        return isAvailableOnDataContext(dataContext)
    }

    override fun invoke(project: Project, editor: Editor, file: PsiFile, dataContext: DataContext) {
        val amosFile = file as? AmosFile ?: return
        AmosRenameSupport.promptAndRenameAtCaret(editor, amosFile)
    }

    override fun invoke(project: Project, elements: Array<out PsiElement>, dataContext: DataContext) {
        val first = elements.firstOrNull() ?: return
        val file = first.containingFile as? AmosFile ?: return
        val editor = CommonDataKeys.EDITOR.getData(dataContext) ?: return
        AmosRenameSupport.promptAndRenameAtCaret(editor, file)
    }
}

