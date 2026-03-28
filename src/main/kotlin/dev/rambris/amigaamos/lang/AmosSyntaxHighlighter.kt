package dev.rambris.amigaamos.lang

import com.intellij.lexer.Lexer
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.HighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType
import com.intellij.ui.JBColor
import java.awt.Font

class AmosSyntaxHighlighter : SyntaxHighlighterBase() {
    override fun getHighlightingLexer(): Lexer = AmosLexer()

    override fun getTokenHighlights(tokenType: IElementType): Array<TextAttributesKey> {
        return when (tokenType) {
            AmosTokenTypes.keyword -> pack(KEYWORD)
            AmosTokenTypes.identifier -> pack(IDENTIFIER)
            AmosTokenTypes.floatVariable -> pack(FLOAT_VARIABLE)
            AmosTokenTypes.stringVariable -> pack(STRING_VARIABLE)
            AmosTokenTypes.number -> pack(NUMBER)
            AmosTokenTypes.string -> pack(STRING)
            AmosTokenTypes.comment -> pack(COMMENT)
            AmosTokenTypes.operator -> pack(OPERATOR)
            AmosTokenTypes.comma -> pack(COMMA)
            AmosTokenTypes.paren -> pack(PAREN)
            TokenType.BAD_CHARACTER -> pack(BAD_CHAR)
            else -> EMPTY
        }
    }

    companion object {
        private val integerVariableTextAttributes = TextAttributes(
            JBColor(0x2F6FFF, 0x7FB9FF),
            null,
            null,
            null,
            Font.PLAIN
        )

        private val stringVariableTextAttributes = TextAttributes(
            JBColor(0x2EA043, 0x56D364),
            null,
            null,
            null,
            Font.PLAIN
        )

        private val realVariableTextAttributes = TextAttributes(
            JBColor(0xD97706, 0xF59E0B),
            null,
            null,
            null,
            Font.PLAIN
        )

        val KEYWORD: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
            "AMOS_KEYWORD",
            DefaultLanguageHighlighterColors.KEYWORD
        )
        val IDENTIFIER: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
            "AMOS_IDENTIFIER",
            integerVariableTextAttributes
        )
        val FLOAT_VARIABLE: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
            "AMOS_FLOAT_VARIABLE",
            realVariableTextAttributes
        )
        val STRING_VARIABLE: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
            "AMOS_STRING_VARIABLE",
            stringVariableTextAttributes
        )
        val NUMBER: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
            "AMOS_NUMBER",
            DefaultLanguageHighlighterColors.NUMBER
        )
        val STRING: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
            "AMOS_STRING",
            DefaultLanguageHighlighterColors.STRING
        )
        val COMMENT: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
            "AMOS_COMMENT",
            DefaultLanguageHighlighterColors.LINE_COMMENT
        )
        val OPERATOR: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
            "AMOS_OPERATOR",
            DefaultLanguageHighlighterColors.OPERATION_SIGN
        )
        val COMMA: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
            "AMOS_COMMA",
            DefaultLanguageHighlighterColors.COMMA
        )
        val PAREN: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
            "AMOS_PAREN",
            DefaultLanguageHighlighterColors.PARENTHESES
        )
        val BAD_CHAR: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
            "AMOS_BAD_CHAR",
            HighlighterColors.BAD_CHARACTER
        )
    }
}
