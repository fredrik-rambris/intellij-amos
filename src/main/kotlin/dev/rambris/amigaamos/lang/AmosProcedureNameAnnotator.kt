package dev.rambris.amigaamos.lang

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.psi.PsiElement
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
import java.util.Locale

class AmosProcedureNameAnnotator : Annotator {
    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        val file = element.containingFile as? AmosFile ?: return
        val tokenType = element.node?.elementType ?: return
        if (tokenType != AmosTokenTypes.keyword) {
            return
        }

        val text = element.text.trim()
        if (text.isEmpty()) {
            return
        }

        val symbolTable = cachedSymbolTable(file)
        val canonicalName = text.uppercase(Locale.ROOT)
        val declarationOffset = symbolTable.procedureDeclarations[canonicalName] ?: return

        val offset = element.textRange.startOffset
        val resolvedOffset = symbolTable.resolveOffset(file.text, tokenType, text, offset)
        if (offset != declarationOffset && resolvedOffset != declarationOffset) {
            return
        }

        holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
            .range(element)
            .textAttributes(AmosSyntaxHighlighter.IDENTIFIER)
            .create()
    }

    private fun cachedSymbolTable(file: AmosFile): AmosSymbolIndex.SymbolTable {
        return CachedValuesManager.getCachedValue(file) {
            CachedValueProvider.Result.create(
                AmosSymbolIndex.build(file.text),
                file,
                PsiModificationTracker.MODIFICATION_COUNT
            )
        }
    }
}

