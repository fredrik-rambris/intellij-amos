package dev.rambris.amigaamos.lang.amal

import com.intellij.lexer.Lexer
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.HighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType

class AmalSyntaxHighlighter : SyntaxHighlighterBase() {
    override fun getHighlightingLexer(): Lexer = AmalLexer()

    override fun getTokenHighlights(tokenType: IElementType): Array<TextAttributesKey> {
        return when (tokenType) {
            AmalTokenTypes.keyword -> pack(KEYWORD)
            AmalTokenTypes.keywordContinuation -> pack(KEYWORD_CONTINUATION)
            AmalTokenTypes.register -> pack(REGISTER)
            AmalTokenTypes.label -> pack(LABEL)
            AmalTokenTypes.identifier -> pack(IDENTIFIER)
            AmalTokenTypes.ignoredText -> pack(IGNORED_TEXT)
            AmalTokenTypes.number -> pack(NUMBER)
            AmalTokenTypes.string -> pack(STRING)
            AmalTokenTypes.operator -> pack(OPERATOR)
            AmalTokenTypes.separator -> pack(SEPARATOR)
            AmalTokenTypes.paren -> pack(PAREN)
            TokenType.BAD_CHARACTER -> pack(BAD_CHAR)
            else -> emptyArray()
        }
    }

    companion object {
        val KEYWORD: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
            "AMAL_KEYWORD",
            DefaultLanguageHighlighterColors.KEYWORD
        )
        val KEYWORD_CONTINUATION: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
            "AMAL_KEYWORD_CONTINUATION",
            DefaultLanguageHighlighterColors.FUNCTION_DECLARATION
        )
        val REGISTER: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
            "AMAL_REGISTER",
            DefaultLanguageHighlighterColors.LOCAL_VARIABLE
        )
        val LABEL: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
            "AMAL_LABEL",
            DefaultLanguageHighlighterColors.FUNCTION_DECLARATION
        )
        val IDENTIFIER: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
            "AMAL_IDENTIFIER",
            DefaultLanguageHighlighterColors.LOCAL_VARIABLE
        )
        val IGNORED_TEXT: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
            "AMAL_IGNORED_TEXT",
            DefaultLanguageHighlighterColors.LINE_COMMENT
        )
        val NUMBER: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
            "AMAL_NUMBER",
            DefaultLanguageHighlighterColors.NUMBER
        )
        val STRING: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
            "AMAL_STRING",
            DefaultLanguageHighlighterColors.STRING
        )
        val OPERATOR: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
            "AMAL_OPERATOR",
            DefaultLanguageHighlighterColors.OPERATION_SIGN
        )
        val SEPARATOR: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
            "AMAL_SEPARATOR",
            DefaultLanguageHighlighterColors.COMMA
        )
        val PAREN: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
            "AMAL_PAREN",
            DefaultLanguageHighlighterColors.PARENTHESES
        )
        val BAD_CHAR: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
            "AMAL_BAD_CHAR",
            HighlighterColors.BAD_CHARACTER
        )
    }
}


