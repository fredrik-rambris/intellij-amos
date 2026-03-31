package dev.rambris.amigaamos.lang.amos

import com.intellij.lang.PsiBuilder
import com.intellij.lang.PsiParser
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType
import java.util.Locale

class AmosParser : PsiParser {
    override fun parse(root: IElementType, builder: PsiBuilder) = builder.mark().apply {
        parseSequence(builder, emptySet())
        done(root)
    }.let { builder.treeBuilt }

    private fun parseSequence(builder: PsiBuilder, terminators: Set<String>) {
        while (!builder.eof()) {
            skipLeadingTrivia(builder)
            if (builder.eof()) {
                return
            }

            val key = statementKey(builder)
            if (key in terminators) {
                return
            }

            when (key) {
                "IF" -> parseIfBlock(builder)
                "FOR" -> parseSimpleBlock(builder, AmosElementTypes.forBlock, setOf("NEXT"))
                "WHILE" -> parseSimpleBlock(builder, AmosElementTypes.whileBlock, setOf("WEND"))
                "REPEAT" -> parseSimpleBlock(builder, AmosElementTypes.repeatBlock, setOf("UNTIL"))
                "DO" -> parseSimpleBlock(builder, AmosElementTypes.doBlock, setOf("LOOP"))
                else -> parseStatement(builder)
            }
        }
    }

    private fun parseIfBlock(builder: PsiBuilder) {
        val block = builder.mark()
        parseStatement(builder)
        parseSequence(builder, setOf("ELSE", "ELSE IF", "END IF"))

        while (!builder.eof()) {
            val key = statementKey(builder)
            if (key == "ELSE" || key == "ELSE IF") {
                parseStatement(builder)
                parseSequence(builder, setOf("ELSE", "ELSE IF", "END IF"))
                continue
            }
            break
        }

        if (!builder.eof() && statementKey(builder) == "END IF") {
            parseStatement(builder)
        }

        block.done(AmosElementTypes.ifBlock)
    }

    private fun parseSimpleBlock(builder: PsiBuilder, blockType: IElementType, terminators: Set<String>) {
        val block = builder.mark()
        parseStatement(builder)
        parseSequence(builder, terminators)

        if (!builder.eof() && statementKey(builder) in terminators) {
            parseStatement(builder)
        }

        block.done(blockType)
    }

    private fun parseStatement(builder: PsiBuilder) {
        if (builder.eof()) {
            return
        }

        val statement = builder.mark()
        val isAssignment = isAssignmentStatement(builder)
        while (!builder.eof() && !isStatementSeparator(builder)) {
            builder.advanceLexer()
        }
        statement.done(if (isAssignment) AmosElementTypes.assignmentStatement else AmosElementTypes.statement)

        consumeSeparator(builder)
    }

    private fun isAssignmentStatement(builder: PsiBuilder): Boolean {
        val lookahead = builder.mark()
        skipInlineTrivia(builder)
        skipOptionalLineNumberLabel(builder)

        if (!isVariableToken(builder.tokenType)) {
            lookahead.rollbackTo()
            return false
        }
        builder.advanceLexer()

        skipInlineTrivia(builder)
        skipArrayIndexAccess(builder)
        skipInlineTrivia(builder)
        val isAssignment = builder.tokenType == AmosTokenTypes.operator && builder.tokenText == "="
        lookahead.rollbackTo()
        return isAssignment
    }

    private fun skipArrayIndexAccess(builder: PsiBuilder) {
        if (builder.tokenType != AmosTokenTypes.paren || builder.tokenText != "(") {
            return
        }

        var depth = 0
        while (!builder.eof()) {
            if (builder.tokenType == AmosTokenTypes.paren) {
                when (builder.tokenText) {
                    "(" -> depth++
                    ")" -> {
                        depth--
                        builder.advanceLexer()
                        if (depth <= 0) {
                            return
                        }
                        continue
                    }
                }
            }
            builder.advanceLexer()
        }
    }

    private fun isVariableToken(type: IElementType?): Boolean {
        return type == AmosTokenTypes.identifier || type == AmosTokenTypes.stringVariable || type == AmosTokenTypes.floatVariable
    }

    private fun isWholeNumberToken(builder: PsiBuilder): Boolean {
        val text = builder.tokenText ?: return false
        return builder.tokenType == AmosTokenTypes.number && text.all(Char::isDigit)
    }

    private fun statementKey(builder: PsiBuilder): String {
        val lookahead = builder.mark()
        skipInlineTrivia(builder)
        skipOptionalLineNumberLabel(builder)

        val first = readKeyword(builder)
        if (first.isEmpty()) {
            lookahead.rollbackTo()
            return ""
        }

        val second = if (first == "END" || first == "ELSE") {
            readKeyword(builder)
        } else {
            ""
        }

        lookahead.rollbackTo()
        return if (second.isEmpty()) first else "$first $second"
    }

    private fun skipInlineTrivia(builder: PsiBuilder) {
        while (!builder.eof() && isInlineWhitespace(builder)) {
            builder.advanceLexer()
        }
    }

    private fun skipOptionalLineNumberLabel(builder: PsiBuilder) {
        if (!isWholeNumberToken(builder)) {
            return
        }

        val lookahead = builder.mark()
        builder.advanceLexer()
        if (!builder.eof() && isInlineWhitespace(builder)) {
            skipInlineTrivia(builder)
            if (!builder.eof()) {
                lookahead.drop()
                return
            }
        }

        lookahead.rollbackTo()
    }

    private fun readKeyword(builder: PsiBuilder): String {
        while (!builder.eof() && isInlineWhitespace(builder)) {
            builder.advanceLexer()
        }
        val text = builder.tokenText ?: return ""
        if (!isWordToken(builder.tokenType)) {
            return ""
        }
        builder.advanceLexer()
        return text.uppercase(Locale.ROOT)
    }

    private fun isWordToken(type: IElementType?): Boolean {
        return type == AmosTokenTypes.keyword ||
            type == AmosTokenTypes.identifier ||
            type == AmosTokenTypes.stringVariable ||
            type == AmosTokenTypes.floatVariable
    }

    private fun skipLeadingTrivia(builder: PsiBuilder) {
        while (!builder.eof()) {
            when {
                isColon(builder) -> builder.advanceLexer()
                isWhitespace(builder) -> builder.advanceLexer()
                else -> return
            }
        }
    }

    private fun consumeSeparator(builder: PsiBuilder) {
        if (builder.eof()) {
            return
        }

        if (isColon(builder)) {
            builder.advanceLexer()
            while (!builder.eof() && isInlineWhitespace(builder)) {
                builder.advanceLexer()
            }
        }

        if (!builder.eof() && isLineBreakWhitespace(builder)) {
            builder.advanceLexer()
        }
    }

    private fun isStatementSeparator(builder: PsiBuilder): Boolean {
        return isColon(builder) || isLineBreakWhitespace(builder)
    }

    private fun isColon(builder: PsiBuilder): Boolean {
        return builder.tokenType == AmosTokenTypes.operator && builder.tokenText == ":"
    }

    private fun isWhitespace(builder: PsiBuilder): Boolean {
        return builder.tokenType == TokenType.WHITE_SPACE
    }

    private fun isInlineWhitespace(builder: PsiBuilder): Boolean {
        if (!isWhitespace(builder)) {
            return false
        }
        val text = builder.tokenText ?: return false
        return !text.contains('\n') && !text.contains('\r')
    }

    private fun isLineBreakWhitespace(builder: PsiBuilder): Boolean {
        if (!isWhitespace(builder)) {
            return false
        }
        val text = builder.tokenText ?: return false
        return text.contains('\n') || text.contains('\r')
    }
}






