package dev.rambris.amigaamos.lang

import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType

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
    val paren = AmosTokenType("PAREN")
    val badCharacter = TokenType.BAD_CHARACTER
}
