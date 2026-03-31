package dev.rambris.amigaamos.lang.amos

import com.intellij.lexer.LexerBase
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType
import java.util.Locale

class AmosLexer : LexerBase() {
    private var buffer: CharSequence = ""
    private var endOffset = 0
    private var tokenStart = 0
    private var tokenEnd = 0
    private var tokenType: IElementType? = null

    override fun start(buffer: CharSequence, startOffset: Int, endOffset: Int, initialState: Int) {
        this.buffer = buffer
        this.endOffset = endOffset
        tokenStart = startOffset
        tokenEnd = startOffset
        tokenType = null
        advance()
    }

    override fun getState(): Int = 0

    override fun getTokenType(): IElementType? = tokenType

    override fun getTokenStart(): Int = tokenStart

    override fun getTokenEnd(): Int = tokenEnd

    override fun getBufferSequence(): CharSequence = buffer

    override fun getBufferEnd(): Int = endOffset

    override fun advance() {
        if (tokenEnd >= endOffset) {
            tokenType = null
            return
        }

        tokenStart = tokenEnd
        val first = buffer[tokenStart]

        if (first.isWhitespace()) {
            var i = tokenStart + 1
            while (i < endOffset && buffer[i].isWhitespace()) {
                i++
            }
            tokenEnd = i
            tokenType = TokenType.WHITE_SPACE
            return
        }

        if (first == '\'' && isLineStart(tokenStart)) {
            var i = tokenStart + 1
            while (i < endOffset && buffer[i] != '\n' && buffer[i] != '\r') {
                i++
            }
            tokenEnd = i
            tokenType = AmosTokenTypes.comment
            return
        }

        if (first == '"') {
            var i = tokenStart + 1
            while (i < endOffset) {
                val ch = buffer[i]
                if (ch == '"') {
                    i++
                    break
                }
                i++
            }
            tokenEnd = i
            tokenType = AmosTokenTypes.string
            return
        }

        if (first.isDigit()) {
            var i = tokenStart + 1
            while (i < endOffset && (buffer[i].isDigit() || buffer[i] == '.')) {
                i++
            }
            tokenEnd = i
            tokenType = AmosTokenTypes.number
            return
        }

        if (first == '$' && tokenStart + 1 < endOffset && buffer[tokenStart + 1].isHexDigit()) {
            var i = tokenStart + 2
            while (i < endOffset && buffer[i].isHexDigit()) {
                i++
            }
            tokenEnd = i
            tokenType = AmosTokenTypes.number
            return
        }

        if (first == '%' && tokenStart + 1 < endOffset && (buffer[tokenStart + 1] == '0' || buffer[tokenStart + 1] == '1')) {
            var i = tokenStart + 2
            while (i < endOffset && (buffer[i] == '0' || buffer[i] == '1')) {
                i++
            }
            tokenEnd = i
            tokenType = AmosTokenTypes.number
            return
        }

        if (first.isLetter() || first == '_') {
            var i = tokenStart + 1
            while (i < endOffset && isIdentifierPart(buffer[i])) {
                i++
            }

            val word = buffer.subSequence(tokenStart, i).toString().uppercase(Locale.ROOT)
            if (word == "REM" && isStatementStart(tokenStart)) {
                while (i < endOffset && buffer[i] != '\n' && buffer[i] != '\r') {
                    i++
                }
                tokenEnd = i
                tokenType = AmosTokenTypes.comment
                return
            }

            tokenEnd = i
            tokenType = when {
                word in AmosCommandIndex.keywordsByWord -> AmosTokenTypes.keyword
                word.endsWith("$") -> AmosTokenTypes.stringVariable
                word.endsWith("#") -> AmosTokenTypes.floatVariable
                else -> AmosTokenTypes.identifier
            }
            return
        }

        if (first == ',' || first == ';') {
            tokenEnd = tokenStart + 1
            tokenType = AmosTokenTypes.comma
            return
        }

        if (first == '(' || first == ')') {
            tokenEnd = tokenStart + 1
            tokenType = AmosTokenTypes.paren
            return
        }

        if (first in "=+-*/<>:^.&:") {
            tokenEnd = tokenStart + 1
            tokenType = AmosTokenTypes.operator
            return
        }

        tokenEnd = tokenStart + 1
        tokenType = AmosTokenTypes.badCharacter
    }

    private fun isIdentifierPart(ch: Char): Boolean {
        return ch.isLetterOrDigit() || ch == '_' || ch == '$' || ch == '#'
    }

    private fun Char.isHexDigit(): Boolean {
        return this in '0'..'9' || this in 'a'..'f' || this in 'A'..'F'
    }

    private fun isStatementStart(offset: Int): Boolean {
        var i = offset - 1
        while (i >= 0 && (buffer[i] == ' ' || buffer[i] == '\t')) {
            i--
        }
        if (i < 0) {
            return true
        }
        return buffer[i] == '\n' || buffer[i] == '\r' || buffer[i] == ':'
    }

    private fun isLineStart(offset: Int): Boolean {
        var i = offset - 1
        while (i >= 0 && (buffer[i] == ' ' || buffer[i] == '\t')) {
            i--
        }
        if (i < 0) {
            return true
        }
        return buffer[i] == '\n' || buffer[i] == '\r'
    }
}

