package dev.rambris.amigaamos.lang.amal

import com.intellij.lexer.LexerBase
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType

class AmalLexer : LexerBase() {
    private data class PendingToken(val start: Int, val end: Int, val type: IElementType)

    private var buffer: CharSequence = ""
    private var endOffset = 0
    private var tokenStart = 0
    private var tokenEnd = 0
    private var tokenType: IElementType? = null
    private val pendingTokens = ArrayDeque<PendingToken>()

    override fun start(buffer: CharSequence, startOffset: Int, endOffset: Int, initialState: Int) {
        this.buffer = buffer
        this.endOffset = endOffset
        tokenStart = startOffset
        tokenEnd = startOffset
        tokenType = null
        pendingTokens.clear()
        advance()
    }

    override fun getState(): Int = 0

    override fun getTokenType(): IElementType? = tokenType

    override fun getTokenStart(): Int = tokenStart

    override fun getTokenEnd(): Int = tokenEnd

    override fun getBufferSequence(): CharSequence = buffer

    override fun getBufferEnd(): Int = endOffset

    override fun advance() {
        if (pendingTokens.isNotEmpty()) {
            val next = pendingTokens.removeFirst()
            tokenStart = next.start
            tokenEnd = next.end
            tokenType = next.type
            return
        }

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
            tokenType = AmalTokenTypes.string
            return
        }

        if (first.isDigit()) {
            var i = tokenStart + 1
            while (i < endOffset && (buffer[i].isDigit() || buffer[i] == '.')) {
                i++
            }
            tokenEnd = i
            tokenType = AmalTokenTypes.number
            return
        }

        if (isWordStart(first)) {
            var i = tokenStart + 1
            while (i < endOffset && isWordPart(buffer[i])) {
                i++
            }

            val word = buffer.subSequence(tokenStart, i).toString()
            if (i < endOffset && buffer[i] == ':' && AmalVocabulary.isPotentialLabelName(word)) {
                queueSignificantLabel(word, i < endOffset && buffer[i] == ':')
                val next = pendingTokens.removeFirst()
                tokenStart = next.start
                tokenEnd = next.end
                tokenType = next.type
                return
            }

            if (isInstructionPosition(tokenStart)) {
                queueCompactPauseJumpLabel(word)
                if (pendingTokens.isNotEmpty()) {
                    val next = pendingTokens.removeFirst()
                    tokenStart = next.start
                    tokenEnd = next.end
                    tokenType = next.type
                    return
                }
            }

            queueCompactJumpLabel(word)
            if (pendingTokens.isNotEmpty()) {
                val next = pendingTokens.removeFirst()
                tokenStart = next.start
                tokenEnd = next.end
                tokenType = next.type
                return
            }

            val compactPrefixLength = compactInstructionPrefixLength(word)
            tokenEnd = if (compactPrefixLength > 0) tokenStart + compactPrefixLength else i
            val lexeme = buffer.subSequence(tokenStart, tokenEnd).toString()

            val instructionPosition = isInstructionPosition(tokenStart)
            if (queueStyledKeywordLexeme(lexeme, instructionPosition)) {
                val next = pendingTokens.removeFirst()
                tokenStart = next.start
                tokenEnd = next.end
                tokenType = next.type
                return
            }

            if (queueIdentifierWithIgnoredTail(lexeme)) {
                val next = pendingTokens.removeFirst()
                tokenStart = next.start
                tokenEnd = next.end
                tokenType = next.type
                return
            }

            tokenType = classifyWord(lexeme)
            return
        }

        if (first == ',' || first == ';') {
            tokenEnd = tokenStart + 1
            tokenType = AmalTokenTypes.separator
            return
        }

        if (first == '(' || first == ')') {
            tokenEnd = tokenStart + 1
            tokenType = AmalTokenTypes.paren
            return
        }

        if (first == '<' || first == '>') {
            tokenEnd = if (tokenStart + 1 < endOffset && (buffer[tokenStart + 1] == '=' || buffer[tokenStart + 1] == '>')) {
                tokenStart + 2
            } else {
                tokenStart + 1
            }
            tokenType = AmalTokenTypes.operator
            return
        }

        if (first in "=+-*/&|") {
            tokenEnd = tokenStart + 1
            tokenType = AmalTokenTypes.operator
            return
        }

        if (first == ':') {
            tokenEnd = tokenStart + 1
            tokenType = AmalTokenTypes.badCharacter
            return
        }

        tokenEnd = tokenStart + 1
        tokenType = AmalTokenTypes.badCharacter
    }

    private fun classifyWord(word: String): IElementType {
        return when {
            AmalVocabulary.isFullInstruction(word) -> AmalTokenTypes.keyword
            AmalVocabulary.isAbbreviatedInstruction(word) && isInstructionPosition(tokenStart) -> AmalTokenTypes.keyword
            AmalVocabulary.isFunction(word) -> AmalTokenTypes.keyword
            AmalVocabulary.isLongFunctionSpelling(word) -> AmalTokenTypes.keyword
            AmalVocabulary.isRegister(word) -> AmalTokenTypes.register
            AmalVocabulary.isIgnoredText(word) -> AmalTokenTypes.ignoredText
            else -> AmalTokenTypes.identifier
        }
    }

    private fun compactInstructionPrefixLength(word: String): Int {
        if (!isInstructionPosition(tokenStart) || word.length < 2) {
            return 0
        }

        for (length in (word.length - 1) downTo 1) {
            val prefix = word.substring(0, length)
            if (!AmalVocabulary.isFullInstruction(prefix) && !AmalVocabulary.isAbbreviatedInstruction(prefix)) {
                continue
            }

            val nextChar = word[length]
            if (nextChar.isUpperCase() || nextChar.isDigit() || nextChar == '_') {
                return length
            }
        }

        return 0
    }

    private fun queueCompactPauseJumpLabel(word: String) {
        if (!word.startsWith("PJ") || word.length < 2) {
            return
        }

        val firstStart = tokenStart
        pendingTokens.add(PendingToken(firstStart, firstStart + 1, AmalTokenTypes.keyword))
        pendingTokens.add(PendingToken(firstStart + 1, firstStart + 2, AmalTokenTypes.keyword))

        if (word.length > 2) {
            val labelStart = firstStart + 2
            pendingTokens.add(PendingToken(labelStart, labelStart + 1, AmalTokenTypes.identifier))
            if (labelStart + 1 < firstStart + word.length) {
                pendingTokens.add(PendingToken(labelStart + 1, firstStart + word.length, AmalTokenTypes.ignoredText))
            }
        }
    }

    private fun queueCompactJumpLabel(word: String) {
        if (!word.startsWith("J") || word.length < 2) {
            return
        }
        if (!word[1].isUpperCase()) {
            return
        }

        val start = tokenStart
        pendingTokens.add(PendingToken(start, start + 1, AmalTokenTypes.keyword))
        pendingTokens.add(PendingToken(start + 1, start + 2, AmalTokenTypes.identifier))
        if (word.length > 2) {
            pendingTokens.add(PendingToken(start + 2, start + word.length, AmalTokenTypes.ignoredText))
        }
    }

    private fun queueStyledKeywordLexeme(lexeme: String, instructionPosition: Boolean): Boolean {
        if (AmalVocabulary.isRegister(lexeme)) {
            return false
        }

        val match = if (instructionPosition || AmalVocabulary.isFullInstruction(lexeme)) {
            AmalVocabulary.canonicalInstructionMatch(lexeme)
                ?: AmalVocabulary.canonicalFunctionMatch(lexeme)
        } else {
            AmalVocabulary.canonicalFunctionMatch(lexeme)
        } ?: return false

        if (match.strongLength <= 0) {
            return false
        }

        val baseStart = tokenStart
        if (match.leadingIgnoredLength > 0) {
            pendingTokens.add(
                PendingToken(
                    baseStart,
                    baseStart + match.leadingIgnoredLength,
                    AmalTokenTypes.ignoredText
                )
            )
        }

        val strongStart = baseStart + match.leadingIgnoredLength
        val strongEnd = strongStart + match.strongLength
        pendingTokens.add(PendingToken(strongStart, strongEnd, AmalTokenTypes.keyword))

        val continuationEnd = strongEnd + match.continuationLength
        if (match.continuationLength > 0) {
            pendingTokens.add(
                PendingToken(
                    strongEnd,
                    continuationEnd,
                    AmalTokenTypes.keywordContinuation
                )
            )
        }

        val ignoredStart = continuationEnd
        if (ignoredStart < baseStart + lexeme.length) {
            pendingTokens.add(PendingToken(ignoredStart, baseStart + lexeme.length, AmalTokenTypes.ignoredText))
        }

        return true
    }

    private fun queueSignificantLabel(word: String, hasColon: Boolean) {
        val start = tokenStart
        pendingTokens.add(PendingToken(start, start + 1, AmalTokenTypes.identifier))
        if (word.length > 1) {
            pendingTokens.add(PendingToken(start + 1, start + word.length, AmalTokenTypes.ignoredText))
        }
        if (hasColon) {
            pendingTokens.add(PendingToken(start + word.length, start + word.length + 1, AmalTokenTypes.separator))
        }
    }

    private fun queueIdentifierWithIgnoredTail(word: String): Boolean {
        if (word.length < 2 || !word.first().isUpperCase() || !word.drop(1).all { it.isLowerCase() }) {
            return false
        }

        if (AmalVocabulary.isFullInstruction(word) || AmalVocabulary.isLongFunctionSpelling(word) || AmalVocabulary.isRegister(word)) {
            return false
        }

        val start = tokenStart
        pendingTokens.add(PendingToken(start, start + 1, AmalTokenTypes.identifier))
        pendingTokens.add(PendingToken(start + 1, start + word.length, AmalTokenTypes.ignoredText))
        return true
    }

    private fun isInstructionPosition(offset: Int): Boolean {
        var i = offset - 1
        while (i >= 0 && (buffer[i] == ' ' || buffer[i] == '\t')) {
            i--
        }
        if (i < 0) {
            return true
        }
        return buffer[i] == ';' ||
            buffer[i] == '(' ||
            buffer[i] == ':' ||
            buffer[i] == '\n' ||
            buffer[i] == '\r'
    }

    private fun isWordStart(ch: Char): Boolean = ch.isLetter() || ch == '_'

    private fun isWordPart(ch: Char): Boolean = ch.isLetterOrDigit() || ch == '_'
}

