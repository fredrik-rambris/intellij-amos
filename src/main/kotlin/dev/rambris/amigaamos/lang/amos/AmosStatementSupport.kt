package dev.rambris.amigaamos.lang.amos

import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType
import java.util.Locale
import kotlin.math.max

data class AmosStatementSegment(
    val startOffset: Int,
    val endOffset: Int,
    val text: String,
    val startsAtLineStart: Boolean,
    val endsWithColonSeparator: Boolean
)

object AmosStatementSupport {
    const val blockIndentSize = 3

    val openingKeys = setOf("IF", "FOR", "WHILE", "REPEAT", "DO", "PROCEDURE", "ELSE", "ELSE IF")
    val closingKeys = setOf("END IF", "NEXT", "WEND", "UNTIL", "LOOP", "END PROC", "ELSE", "ELSE IF")

    private val labelPattern = Regex("""[A-Za-z_][A-Za-z0-9_$#]*|\d+""")
    private val numberedLinePattern = Regex("""^(\d+)([ \t]+)(.+)$""")
    private val symbolicLabelDeclarationPattern = Regex("""^([A-Za-z_][A-Za-z0-9_$#]*)\s*:.*$""")
    private val thenWordPattern = Regex("""\bTHEN\b""")
    private val forIndexPattern = Regex("""^FOR\s+([A-Z_][A-Z0-9_$#]*)\s*=""", RegexOption.IGNORE_CASE)

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

    fun statementKey(text: String): String {
        val normalized = text.trimStart().uppercase(Locale.ROOT)
        return when {
            normalized.startsWith("END PROC") -> "END PROC"
            normalized.startsWith("PROCEDURE") -> "PROCEDURE"
            normalized.startsWith("END IF") -> "END IF"
            normalized.startsWith("ELSE IF") -> "ELSE IF"
            normalized.startsWith("IF") -> "IF"
            normalized.startsWith("ELSE") -> "ELSE"
            normalized.startsWith("FOR") -> "FOR"
            normalized.startsWith("NEXT") -> "NEXT"
            normalized.startsWith("WHILE") -> "WHILE"
            normalized.startsWith("WEND") -> "WEND"
            normalized.startsWith("REPEAT") -> "REPEAT"
            normalized.startsWith("UNTIL") -> "UNTIL"
            normalized.startsWith("DO") -> "DO"
            normalized.startsWith("LOOP") -> "LOOP"
            else -> ""
        }
    }

    /**
     * Scans [lines] from index 0 up to (but not including) [targetLine] and returns the
     * indent level that [targetLine] should start at.
     */
    fun computeIndentLevelAtLine(lines: List<String>, targetLine: Int): Int {
        var level = 0
        for (i in 0 until minOf(targetLine, lines.size)) {
            val key = statementKey(lines[i].trimStart())
            if (key in closingKeys) level = max(0, level - 1)
            if (key in openingKeys) level++
        }
        return level
    }

    fun isInlineIfStatement(text: String): Boolean {
        val normalized = text.trimStart().uppercase(Locale.ROOT)
        if (!normalized.startsWith("IF")) {
            return false
        }

        val thenMatch = thenWordPattern.find(normalized) ?: return false
        val tail = normalized.substring(thenMatch.range.last + 1).trimStart()
        return tail.isNotEmpty()
    }

    fun autoCloseStatementFor(text: String): String? {
        val normalized = text.trimStart()
        val key = statementKey(normalized)
        return when (key) {
            "FOR" -> {
                val indexName = extractForIndexName(normalized)
                if (indexName == null) "Next" else "Next $indexName"
            }
            "IF" -> if (isInlineIfStatement(normalized)) null else "End If"
            "WHILE" -> "Wend"
            "REPEAT" -> "Until"
            "DO" -> "Loop"
            "PROCEDURE" -> "End Proc"
            else -> null
        }
    }

    private fun extractForIndexName(text: String): String? {
        val match = forIndexPattern.find(text) ?: return null
        return match.groupValues[1].uppercase(Locale.ROOT)
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
