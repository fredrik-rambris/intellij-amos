package dev.rambris.amigaamos.lang.amos

import com.intellij.lang.cacheBuilder.DefaultWordsScanner
import com.intellij.lang.cacheBuilder.WordsScanner
import com.intellij.lang.findUsages.FindUsagesProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.TokenSet

class AmosFindUsagesProvider : FindUsagesProvider {
    override fun getWordsScanner(): WordsScanner {
        return DefaultWordsScanner(
            AmosLexer(),
            TokenSet.create(
                AmosTokenTypes.identifier,
                AmosTokenTypes.keyword,
                AmosTokenTypes.stringVariable,
                AmosTokenTypes.floatVariable,
                AmosTokenTypes.number
            ),
            TokenSet.create(AmosTokenTypes.comment),
            TokenSet.create(AmosTokenTypes.string)
        )
    }

    override fun canFindUsagesFor(psiElement: PsiElement): Boolean {
        val type = psiElement.node?.elementType ?: return false
        return type == AmosTokenTypes.identifier ||
            type == AmosTokenTypes.keyword ||
            type == AmosTokenTypes.stringVariable ||
            type == AmosTokenTypes.floatVariable ||
            type == AmosTokenTypes.number
    }

    override fun getHelpId(psiElement: PsiElement): String? = null

    override fun getType(element: PsiElement): String {
        val type = element.node?.elementType ?: return "symbol"
        return when {
            type == AmosTokenTypes.number -> "label"
            isProcedureSymbol(element) -> "procedure"
            isLabelSymbol(element) -> "label"
            else -> "variable"
        }
    }

    override fun getDescriptiveName(element: PsiElement): String = element.text

    override fun getNodeText(element: PsiElement, useFullName: Boolean): String = element.text

    private fun isProcedureSymbol(element: PsiElement): Boolean {
        val type = element.node?.elementType
        if (type != AmosTokenTypes.identifier && type != AmosTokenTypes.keyword) {
            return false
        }

        val file = element.containingFile as? AmosFile ?: return false
        return AmosSymbolIndex.build(file.text).procedureDeclarations.containsKey(element.text.uppercase())
    }

    private fun isLabelSymbol(element: PsiElement): Boolean {
        val file = element.containingFile as? AmosFile ?: return false
        val key = if (element.node?.elementType == AmosTokenTypes.number) element.text else element.text.uppercase()
        return AmosSymbolIndex.build(file.text).labelDeclarations.containsKey(key)
    }
}

