package dev.rambris.amigaamos.lang

import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType
import java.util.Locale

data class AmosStatementSegment(
    val startOffset: Int,
    val endOffset: Int,
    val text: String,
    val startsAtLineStart: Boolean,
    val endsWithColonSeparator: Boolean
)

object AmosStatementSupport {
    private val labelPattern = Regex("""[A-Za-z_][A-Za-z0-9_$#]*|\d+""")
    private val numberedLinePattern = Regex("""^(\d+)([ \t]+)(.+)$""")
    private val symbolicLabelDeclarationPattern = Regex("""^([A-Za-z_][A-Za-z0-9_$#]*)\s*:.*$""")

    fun declaredLabels(source: String): List<String> {
        val labels = linkedSetOf<String>()

        source.lineSequence().forEach { line ->
            val trimmed = line.trimStart()
            if (trimmed.isEmpty()) {
                return@forEach
            }

            val symbolicLabelMatch = symbolicLabelDeclarationPattern.matchEntire(trimmed)
            if (symbolicLabelMatch != null) {
                val label = symbolicLabelMatch.groupValues[1]
                if (isLabelDeclaration(label)) {
                    labels += label.uppercase(Locale.ROOT)
                }
                return@forEach
            }

            val numberedLineMatch = numberedLinePattern.matchEntire(trimmed)
            if (numberedLineMatch != null) {
                labels += numberedLineMatch.groupValues[1]
            }
        }

        return labels.toList()
    }

    fun splitStatements(source: String): List<AmosStatementSegment> {
        val lexer = AmosLexer()
        lexer.start(source)

        val statements = mutableListOf<AmosStatementSegment>()
        var statementStart = 0
        var startsAtLineStart = true

        while (lexer.tokenType != null) {
            val tokenType = lexer.tokenType
            val tokenStart = lexer.tokenStart
            val tokenEnd = lexer.tokenEnd
            val tokenText = source.substring(tokenStart, tokenEnd)

            if (isStatementSeparator(tokenType, tokenText)) {
                if (tokenStart > statementStart) {
                    statements += AmosStatementSegment(
                        startOffset = statementStart,
                        endOffset = tokenStart,
                        text = source.substring(statementStart, tokenStart),
                        startsAtLineStart = startsAtLineStart,
                        endsWithColonSeparator = isColon(tokenType, tokenText)
                    )
                }
                statementStart = tokenEnd
                startsAtLineStart = isLineBreak(tokenType, tokenText)
            }

            lexer.advance()
        }

        if (statementStart < source.length) {
            statements += AmosStatementSegment(
                startOffset = statementStart,
                endOffset = source.length,
                text = source.substring(statementStart),
                startsAtLineStart = startsAtLineStart,
                endsWithColonSeparator = false
            )
        }

        return statements
    }

    fun normalizeForAnalysis(statement: AmosStatementSegment): String {
        return normalizeForAnalysis(statement.text, statement.startsAtLineStart, statement.endsWithColonSeparator)
    }

    fun normalizeForAnalysis(text: String, startsAtLineStart: Boolean, endsWithColonSeparator: Boolean = false): String {
        val trimmed = text.trimStart()
        if (trimmed.isEmpty()) {
            return ""
        }

        if (startsAtLineStart && endsWithColonSeparator && isLabelDeclaration(trimmed)) {
            return ""
        }

        if (startsAtLineStart) {
            val numberedLineMatch = numberedLinePattern.matchEntire(trimmed)
            if (numberedLineMatch != null) {
                return numberedLineMatch.groupValues[3].trimStart()
            }
        }

        return trimmed
    }

    private fun isLabelDeclaration(text: String): Boolean {
        if (!labelPattern.matches(text)) {
            return false
        }

        return text.all(Char::isDigit) || text.uppercase(Locale.ROOT) !in AmosCommandIndex.keywordsByWord
    }

    private fun isStatementSeparator(tokenType: IElementType?, tokenText: String): Boolean {
        return isColon(tokenType, tokenText) || isLineBreak(tokenType, tokenText)
    }

    private fun isColon(tokenType: IElementType?, tokenText: String): Boolean {
        return tokenType == AmosTokenTypes.operator && tokenText == ":"
    }

    private fun isLineBreak(tokenType: IElementType?, tokenText: String): Boolean {
        return tokenType == TokenType.WHITE_SPACE && (tokenText.contains('\n') || tokenText.contains('\r'))
    }
}


