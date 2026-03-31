package dev.rambris.amigaamos.lang.amos

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

        val prevLineStart = document.getLineStartOffset(prevLine)
        val prevLineEnd = document.getLineEndOffset(prevLine)
        val prevLineText = document.getText(TextRange(prevLineStart, prevLineEnd))

        // 1) Format the line we just left (casing, separators, and correct indent).
        val casedAndSpaced = AmosCodeStyleFormatter.formatLine(prevLineText)
        val linesBeforePrev = (0 until prevLine).map { i ->
            document.getText(TextRange(document.getLineStartOffset(i), document.getLineEndOffset(i)))
        }
        // Base level from all lines before prevLine, then subtract 1 if prevLine itself is a closer.
        var prevIndentLevel = AmosStatementSupport.computeIndentLevelAtLine(linesBeforePrev, prevLine)
        val prevKey = AmosStatementSupport.statementKey(casedAndSpaced.trimStart())
        if (prevKey in AmosStatementSupport.closingKeys) {
            prevIndentLevel = (prevIndentLevel - 1).coerceAtLeast(0)
        }
        val prevIndent = " ".repeat(prevIndentLevel * AmosStatementSupport.blockIndentSize)
        val formattedPrev = prevIndent + casedAndSpaced.trimStart()

        WriteCommandAction.runWriteCommandAction(
            file.project,
            "AMOS Format Line",
            null,
            Runnable {
                if (formattedPrev != prevLineText) {
                    document.replaceString(prevLineStart, prevLineEnd, formattedPrev)
                }

                // 2) Enforce indentation for the newly created current line.
                val currentLineStart = document.getLineStartOffset(currentLine)
                val currentLineEnd = document.getLineEndOffset(currentLine)
                val currentLineText = document.getText(TextRange(currentLineStart, currentLineEnd))

                val linesBeforeCurrent = (0 until currentLine).map { i ->
                    document.getText(TextRange(document.getLineStartOffset(i), document.getLineEndOffset(i)))
                }
                val currentIndentLevel = AmosStatementSupport.computeIndentLevelAtLine(linesBeforeCurrent, currentLine)
                val currentIndent = " ".repeat(currentIndentLevel * AmosStatementSupport.blockIndentSize)
                val currentWithoutLeading = currentLineText.trimStart()
                val formattedCurrent = if (currentWithoutLeading.isEmpty()) currentIndent else currentIndent + currentWithoutLeading

                if (formattedCurrent != currentLineText) {
                    document.replaceString(currentLineStart, currentLineEnd, formattedCurrent)
                }

                // 3) If we just opened a block, add the matching closer line below.
                if (currentWithoutLeading.isEmpty()) {
                    val closeStatement = AmosStatementSupport.autoCloseStatementFor(formattedPrev.trimStart())
                    if (closeStatement != null) {
                        val nextLine = currentLine + 1
                        val nextAlreadyMatches = if (nextLine < document.lineCount) {
                            val nextStart = document.getLineStartOffset(nextLine)
                            val nextEnd = document.getLineEndOffset(nextLine)
                            val nextText = document.getText(TextRange(nextStart, nextEnd)).trimStart()
                            nextText.equals(closeStatement, ignoreCase = true)
                        } else {
                            false
                        }

                        if (!nextAlreadyMatches) {
                            val closerIndent = prevIndent
                            val insertionOffset = document.getLineEndOffset(currentLine)
                            val isRepeatBlock = closeStatement.equals("Until", ignoreCase = true)
                            val insertedCloser = if (isRepeatBlock) "Until " else closeStatement
                            document.insertString(insertionOffset, "\n$closerIndent$insertedCloser")
                            if (isRepeatBlock) {
                                // For Repeat..Until, place caret after "Until " ready to type the condition
                                val closerLineNumber = currentLine + 1
                                val closerCaretOffset =
                                    document.getLineStartOffset(closerLineNumber) + closerIndent.length + insertedCloser.length
                                editor.caretModel.moveToOffset(closerCaretOffset)
                            } else {
                                val bodyCaretOffset = document.getLineStartOffset(currentLine) + currentIndent.length
                                editor.caretModel.moveToOffset(bodyCaretOffset)
                            }
                        }
                    }
                }

                PsiDocumentManager.getInstance(file.project).commitDocument(document)
            },
            file
        )

        return Result.Continue
    }
}
