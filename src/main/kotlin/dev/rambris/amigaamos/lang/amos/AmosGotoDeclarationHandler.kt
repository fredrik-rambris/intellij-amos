package dev.rambris.amigaamos.lang.amos

import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement

class AmosGotoDeclarationHandler : GotoDeclarationHandler {
    override fun getGotoDeclarationTargets(sourceElement: PsiElement?, offset: Int, editor: Editor): Array<PsiElement>? {
        val element = sourceElement ?: return null
        val file = element.containingFile as? AmosFile ?: return null

        val leaf = nearestNavigableLeaf(element) ?: return null
        val tokenType = leaf.node?.elementType ?: return null
        if (!isNavigableToken(tokenType)) {
            return null
        }

        val source = file.text
        val symbolTable = AmosSymbolIndex.build(source)

        // If Ctrl+Click is invoked on a declaration, return usage sites so the chooser can jump to calls/usages.
        val declarationOffset = symbolTable.resolveOffset(source, tokenType, leaf.text, leaf.textRange.startOffset)
        if (declarationOffset == leaf.textRange.startOffset) {
            val usageOffsets = symbolTable.usageOffsetsForDeclaration(source, declarationOffset)
            if (usageOffsets.isNotEmpty()) {
                return usageOffsets
                    .mapNotNull { usageOffset -> file.findElementAt(usageOffset) }
                    .toTypedArray()
                    .takeIf { it.isNotEmpty() }
            }
            return null
        }

        val targetOffset = symbolTable
            .resolveOffset(source, tokenType, leaf.text, leaf.textRange.startOffset)
            ?: return null

        if (targetOffset == leaf.textRange.startOffset) {
            return null
        }

        val target = file.findElementAt(targetOffset) ?: return null
        return arrayOf(target)
    }

    private fun nearestNavigableLeaf(element: PsiElement): PsiElement? {
        val type = element.node?.elementType
        if (type != null && isNavigableToken(type)) {
            return element
        }

        val atOffset = element.containingFile.findElementAt(element.textRange.startOffset)
        val atType = atOffset?.node?.elementType
        if (atOffset != null && atType != null && isNavigableToken(atType)) {
            return atOffset
        }

        return null
    }

    private fun isNavigableToken(type: com.intellij.psi.tree.IElementType): Boolean {
        return type == AmosTokenTypes.identifier ||
            type == AmosTokenTypes.keyword ||
            type == AmosTokenTypes.stringVariable ||
            type == AmosTokenTypes.floatVariable ||
            type == AmosTokenTypes.number
    }
}


