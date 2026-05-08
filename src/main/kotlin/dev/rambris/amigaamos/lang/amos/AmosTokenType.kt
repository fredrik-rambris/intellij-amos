package dev.rambris.amigaamos.lang.amos

import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet

open class AmosTokenType(debugName: String) : IElementType(debugName, AmosLanguage) {
    override fun toString(): String = "AmosTokenType." + super.toString()
}

object AmosTokenTypes {
    val keyword = AmosTokenType("KEYWORD")
    val identifier = AmosTokenType("IDENTIFIER")
    val floatVariable = AmosTokenType("FLOAT_VARIABLE")
    val stringVariable = AmosTokenType("STRING_VARIABLE")
    val number = AmosTokenType("NUMBER")
    val string = AmosTokenType("STRING")
    val comment = AmosTokenType("COMMENT")
    val operator = AmosTokenType("OPERATOR")
    val comma = AmosTokenType("COMMA")
    val lparen = AmosTokenType("LPAREN")
    val rparen = AmosTokenType("RPAREN")
    val lbracket = AmosTokenType("LBRACKET")
    val rbracket = AmosTokenType("RBRACKET")
    val lbrace = AmosTokenType("LBRACE")
    val rbrace = AmosTokenType("RBRACE")
    val badCharacter = TokenType.BAD_CHARACTER

    // Block keyword token types for brace matching
    val ifKeyword = AmosTokenType("IF_KEYWORD")
    val endIfKeyword = AmosTokenType("END_IF_KEYWORD")
    val forKeyword = AmosTokenType("FOR_KEYWORD")
    val nextKeyword = AmosTokenType("NEXT_KEYWORD")
    val whileKeyword = AmosTokenType("WHILE_KEYWORD")
    val wendKeyword = AmosTokenType("WEND_KEYWORD")
    val repeatKeyword = AmosTokenType("REPEAT_KEYWORD")
    val untilKeyword = AmosTokenType("UNTIL_KEYWORD")
    val doKeyword = AmosTokenType("DO_KEYWORD")
    val loopKeyword = AmosTokenType("LOOP_KEYWORD")
    val procedureKeyword = AmosTokenType("PROCEDURE_KEYWORD")
    val endProcKeyword = AmosTokenType("END_PROC_KEYWORD")

    val allBlockKeywords: TokenSet = TokenSet.create(
        ifKeyword, endIfKeyword, forKeyword, nextKeyword,
        whileKeyword, wendKeyword, repeatKeyword, untilKeyword,
        doKeyword, loopKeyword, procedureKeyword, endProcKeyword
    )

    val allParens: TokenSet = TokenSet.create(lparen, rparen, lbracket, rbracket, lbrace, rbrace)
}
