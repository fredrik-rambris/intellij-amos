package dev.rambris.amigaamos.lang

import com.intellij.codeInsight.AutoPopupController
import com.intellij.codeInsight.editorActions.TypedHandlerDelegate
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile

class AmosTypedHandler : TypedHandlerDelegate() {
    override fun beforeCharTyped(c: Char, project: Project, editor: Editor, file: PsiFile, fileType: FileType): Result {
        if (c != '\t' || file !is AmosFile) {
            return Result.CONTINUE
        }

        val offset = editor.caretModel.offset
        val variableName = AmosStringChainSupport.continuationVariableAtOffset(file.text, offset) ?: return Result.CONTINUE
        val document = editor.document
        val lineNumber = document.getLineNumber(offset)
        val lineStartOffset = document.getLineStartOffset(lineNumber)
        val lineEndOffset = document.getLineEndOffset(lineNumber)
        val lineText = document.charsSequence.subSequence(lineStartOffset, lineEndOffset).toString()
        if (lineText.isNotBlank()) {
            return Result.CONTINUE
        }

        val indentation = lineText.take(offset - lineStartOffset)
        val snippet = indentation + AmosStringChainSupport.continuationSnippet(variableName)
        document.replaceString(lineStartOffset, lineEndOffset, snippet)
        editor.caretModel.moveToOffset(lineStartOffset + snippet.length - 1)
        return Result.STOP
    }

    override fun charTyped(c: Char, project: Project, editor: Editor, file: PsiFile): Result {
        if (c != '\n' && c != '\r') {
            return Result.CONTINUE
        }

        if (file !is AmosFile) {
            return Result.CONTINUE
        }

        val offset = editor.caretModel.offset
        val variable = AmosStringChainSupport.continuationVariableAtOffset(file.text, offset)
        if (variable != null) {
            AutoPopupController.getInstance(project).scheduleAutoPopup(editor)
        }

        return Result.CONTINUE
    }
}


