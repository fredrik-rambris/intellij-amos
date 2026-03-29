package dev.rambris.amigaamos.lang

import com.intellij.codeInsight.editorActions.enter.EnterHandlerDelegate
import com.intellij.codeInsight.editorActions.enter.EnterHandlerDelegate.Result
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.actionSystem.EditorActionHandler
import com.intellij.openapi.util.Ref
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile

class AmosEnterHandlerDelegate : EnterHandlerDelegate {

    override fun preprocessEnter(
        file: PsiFile,
        editor: Editor,
        caretOffset: Ref<Int>,
        caretAdvance: Ref<Int>,
        dataContext: DataContext,
        originalHandler: EditorActionHandler?
    ): Result = Result.Continue

    override fun postProcessEnter(
        file: PsiFile,
        editor: Editor,
        dataContext: DataContext
    ): Result {
        if (file !is AmosFile) return Result.Continue

        val document = editor.document
        val caretOffset = editor.caretModel.offset
        val currentLine = document.getLineNumber(caretOffset)
        val prevLine = currentLine - 1
        if (prevLine < 0) return Result.Continue

        val lineStart = document.getLineStartOffset(prevLine)
        val lineEnd = document.getLineEndOffset(prevLine)
        val lineText = document.getText(TextRange(lineStart, lineEnd))

        val formatted = AmosCodeStyleFormatter.formatLine(lineText)
        if (formatted == lineText) return Result.Continue

        WriteCommandAction.runWriteCommandAction(file.project, "AMOS Format Line", null, {
            document.replaceString(lineStart, lineEnd, formatted)
            PsiDocumentManager.getInstance(file.project).commitDocument(document)
        })

        return Result.Continue
    }
}
