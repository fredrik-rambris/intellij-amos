package dev.rambris.amigaamos.lang.amos

import com.intellij.formatting.Alignment
import com.intellij.formatting.Block
import com.intellij.formatting.ChildAttributes
import com.intellij.formatting.FormattingModelProvider
import com.intellij.formatting.FormattingModel
import com.intellij.formatting.FormattingModelBuilder
import com.intellij.formatting.Indent
import com.intellij.formatting.SpacingBuilder
import com.intellij.formatting.Wrap
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.formatter.common.AbstractBlock

class AmosFormattingModelBuilder : FormattingModelBuilder {
    override fun createModel(element: PsiElement, settings: CodeStyleSettings): FormattingModel {
        val spacingBuilder = SpacingBuilder(settings, AmosLanguage)
        val rootBlock = AmosFormattingBlock(
            node = element.node,
            wrap = null,
            alignment = null,
            spacingBuilder = spacingBuilder
        )

        return FormattingModelProvider.createFormattingModelForPsiFile(
            element.containingFile,
            rootBlock,
            settings
        )
    }

    private class AmosFormattingBlock(
        node: ASTNode,
        wrap: Wrap?,
        alignment: Alignment?,
        private val spacingBuilder: SpacingBuilder
    ) : AbstractBlock(node, wrap, alignment) {
        override fun buildChildren(): List<Block> {
            val children = mutableListOf<Block>()
            var child = myNode.firstChildNode
            while (child != null) {
                if (child.textRange.length > 0 && !child.textRange.isEmpty) {
                    children += AmosFormattingBlock(child, null, null, spacingBuilder)
                }
                child = child.treeNext
            }
            return children
        }

        override fun getIndent(): Indent? {
            val parent = myNode.treeParent ?: return Indent.getNoneIndent()
            val parentType = parent.elementType
            if (parentType !in blockElementTypes) {
                return Indent.getNoneIndent()
            }

            val statementSiblings = generateSequence(parent.firstChildNode) { it.treeNext }
                .filter { it.elementType == AmosElementTypes.statement || it.elementType == AmosElementTypes.assignmentStatement }
                .toList()

            val first = statementSiblings.firstOrNull()
            val last = statementSiblings.lastOrNull()
            return if (myNode == first || myNode == last) {
                Indent.getNoneIndent()
            } else {
                Indent.getSpaceIndent(3)
            }
        }

        override fun getSpacing(child1: Block?, child2: Block): com.intellij.formatting.Spacing? {
            return spacingBuilder.getSpacing(this, child1, child2)
        }

        override fun isLeaf(): Boolean = myNode.firstChildNode == null

        override fun getChildAttributes(newChildIndex: Int): ChildAttributes {
            return ChildAttributes(Indent.getSpaceIndent(3), null)
        }
    }

    companion object {
        private val blockElementTypes = setOf(
            AmosElementTypes.ifBlock,
            AmosElementTypes.forBlock,
            AmosElementTypes.whileBlock,
            AmosElementTypes.repeatBlock,
            AmosElementTypes.doBlock
        )
    }
}


