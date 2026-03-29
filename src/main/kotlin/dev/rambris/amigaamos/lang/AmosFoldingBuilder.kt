package dev.rambris.amigaamos.lang

import com.intellij.lang.ASTNode
import com.intellij.lang.folding.FoldingBuilderEx
import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement

class AmosFoldingBuilder : FoldingBuilderEx() {
    override fun buildFoldRegions(root: PsiElement, document: Document, quick: Boolean): Array<FoldingDescriptor> {
        return buildFoldRegionsFromSource(root.node, root.text, document).toTypedArray()
    }

    override fun getPlaceholderText(node: ASTNode): String {
        return when (node.elementType) {
            AmosElementTypes.ifBlock -> "if ... end if"
            AmosElementTypes.forBlock -> "for ... next"
            AmosElementTypes.whileBlock -> "while ... wend"
            AmosElementTypes.repeatBlock -> "repeat ... until"
            AmosElementTypes.doBlock -> "do ... loop"
            else -> "..."
        }
    }

    override fun isCollapsedByDefault(node: ASTNode): Boolean = false

    fun buildFoldRegionsFromSource(root: ASTNode, source: String, document: Document): List<FoldingDescriptor> {
        val descriptors = mutableListOf<FoldingDescriptor>()
        val stack = mutableListOf<BlockStart>()

        for (statement in AmosStatementSupport.splitStatements(source)) {
            val normalized = AmosStatementSupport.normalizeForAnalysis(statement)
            when (val key = AmosStatementSupport.statementKey(normalized)) {
                "IF" -> stack += BlockStart("IF", statement.startOffset, placeholderForBlockStart(normalized, "if ... end if"))
                "FOR" -> stack += BlockStart("FOR", statement.startOffset, placeholderForBlockStart(normalized, "for ... next"))
                "WHILE" -> stack += BlockStart("WHILE", statement.startOffset, placeholderForBlockStart(normalized, "while ... wend"))
                "REPEAT" -> stack += BlockStart("REPEAT", statement.startOffset, placeholderForBlockStart(normalized, "repeat ... until"))
                "DO" -> stack += BlockStart("DO", statement.startOffset, placeholderForBlockStart(normalized, "do ... loop"))
                "PROCEDURE" -> stack += BlockStart("PROCEDURE", statement.startOffset, placeholderForBlockStart(normalized, "procedure ... end proc"))
                "END IF" -> closeBlock(root, document, stack, descriptors, "IF", statement.endOffset)
                "NEXT" -> closeBlock(root, document, stack, descriptors, "FOR", statement.endOffset)
                "WEND" -> closeBlock(root, document, stack, descriptors, "WHILE", statement.endOffset)
                "UNTIL" -> closeBlock(root, document, stack, descriptors, "REPEAT", statement.endOffset)
                "LOOP" -> closeBlock(root, document, stack, descriptors, "DO", statement.endOffset)
                "END PROC" -> closeBlock(root, document, stack, descriptors, "PROCEDURE", statement.endOffset)
                else -> key
            }
        }

        return descriptors
    }

    private fun spansMultipleLines(range: TextRange, document: Document): Boolean {
        val startLine = document.getLineNumber(range.startOffset)
        val endLine = document.getLineNumber((range.endOffset - 1).coerceAtLeast(range.startOffset))
        return endLine > startLine
    }

    private fun closeBlock(
        root: ASTNode,
        document: Document,
        stack: MutableList<BlockStart>,
        descriptors: MutableList<FoldingDescriptor>,
        expectedKey: String,
        endOffset: Int
    ) {
        val index = stack.indexOfLast { it.key == expectedKey }
        if (index < 0) {
            return
        }

        val start = stack.removeAt(index)
        val range = TextRange(start.startOffset, endOffset)
        if (!spansMultipleLines(range, document)) {
            return
        }

        descriptors += FoldingDescriptor(root, range, null, start.placeholder)
    }


    private data class BlockStart(
        val key: String,
        val startOffset: Int,
        val placeholder: String
    )

    private fun placeholderForBlockStart(statement: String, fallback: String): String {
        val collapsed = statement.trim().replace(Regex("\\s+"), " ")
        if (collapsed.isEmpty()) {
            return fallback
        }

        return if (collapsed.length <= 80) collapsed else collapsed.take(77) + "..."
    }


    fun buildFoldRegionsFromAst(root: ASTNode, document: Document): List<FoldingDescriptor> {
        return buildFoldRegionsFromSource(root, root.text, document)
    }
}




