package dev.rambris.amigaamos.lang.amal

import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType

open class AmalTokenType(debugName: String) : IElementType(debugName, AmalLanguage) {
    override fun toString(): String = "AmalTokenType." + super.toString()
}

object AmalTokenTypes {
    val keyword = AmalTokenType("KEYWORD")
    val keywordContinuation = AmalTokenType("KEYWORD_CONTINUATION")
    val register = AmalTokenType("REGISTER")
    val label = AmalTokenType("LABEL")
    val identifier = AmalTokenType("IDENTIFIER")
    val ignoredText = AmalTokenType("IGNORED_TEXT")
    val number = AmalTokenType("NUMBER")
    val string = AmalTokenType("STRING")
    val operator = AmalTokenType("OPERATOR")
    val separator = AmalTokenType("SEPARATOR")
    val paren = AmalTokenType("PAREN")
    val badCharacter = TokenType.BAD_CHARACTER
}

