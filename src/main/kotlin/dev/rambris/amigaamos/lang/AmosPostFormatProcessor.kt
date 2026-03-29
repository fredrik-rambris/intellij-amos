package dev.rambris.amigaamos.lang

import com.intellij.openapi.util.TextRange
import com.intellij.openapi.application.WriteAction
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.impl.source.codeStyle.PostFormatProcessor

class AmosPostFormatProcessor : PostFormatProcessor {
    override fun processElement(source: PsiElement, settings: CodeStyleSettings): PsiElement {
        return source
    }

    override fun processText(source: PsiFile, rangeToReformat: TextRange, settings: CodeStyleSettings): TextRange {
        if (source !is AmosFile) {
            return rangeToReformat
        }

        applyFormatting(source)
        return TextRange(0, source.textLength)
    }

    private fun applyFormatting(file: AmosFile) {
        val documentManager = PsiDocumentManager.getInstance(file.project)
        val document = documentManager.getDocument(file) ?: return
        val original = document.text
        val formatted = AmosCodeStyleFormatter.format(original)
        if (formatted == original) {
            return
        }

        WriteAction.run<RuntimeException> {
            document.replaceString(0, document.textLength, formatted)
            documentManager.commitDocument(document)
        }
    }
}


