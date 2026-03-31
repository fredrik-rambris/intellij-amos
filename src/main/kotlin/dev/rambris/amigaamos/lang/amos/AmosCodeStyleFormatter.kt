package dev.rambris.amigaamos.lang.amos

import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType
import java.util.Locale

object AmosCodeStyleFormatter {
    private const val blockIndentSize = AmosStatementSupport.blockIndentSize
    private const val inlineIfStatement = "IF INLINE"
    private val lowerCaseLogicalOperators = setOf("AND", "OR")

    private val openingStatements = AmosStatementSupport.openingKeys
    private val closingStatements = AmosStatementSupport.closingKeys

    fun format(source: String): String {
        if (source.isEmpty()) {
            return source
        }

        val cased = normalizeTokenCasing(source)
        val spaced = normalizeColonSpacing(cased)
        return applyBlockIndentation(spaced)
    }

    /** Format a single line in isolation: casing and colon spacing only, no indentation change. */
    fun formatLine(line: String): String {
        if (line.isEmpty()) return line
        val cased = normalizeTokenCasing(line)
        return normalizeColonSpacing(cased).trimEnd()
    }

    private fun normalizeTokenCasing(source: String): String {
        val lexer = AmosLexer()
        lexer.start(source)
        val result = StringBuilder(source.length)
        var previousWordUpper: String? = null
        var inProcedureCallList = false

        while (lexer.tokenType != null) {
            val tokenType = lexer.tokenType
            val tokenText = source.substring(lexer.tokenStart, lexer.tokenEnd)
            val currentWordUpper = if (
                tokenType == AmosTokenTypes.keyword ||
                tokenType == AmosTokenTypes.identifier ||
                tokenType == AmosTokenTypes.stringVariable ||
                tokenType == AmosTokenTypes.floatVariable
            ) {
                tokenText.uppercase(Locale.ROOT)
            } else {
                null
            }

            val replacement = when (tokenType) {
                AmosTokenTypes.keyword -> {
                    val tokenUpper = tokenText.uppercase(Locale.ROOT)
                    if (tokenUpper in lowerCaseLogicalOperators) {
                        tokenText.lowercase(Locale.ROOT)
                    } else if ((tokenUpper.endsWith("$") || tokenUpper.endsWith("#")) && !isFunctionCallLike(source, lexer.tokenEnd)) {
                        // Avoid turning typed variable names like LINE$ into command-style camel case.
                        tokenText.uppercase(Locale.ROOT)
                    } else if (previousWordUpper == "PROCEDURE" || inProcedureCallList) {
                        tokenText.uppercase(Locale.ROOT)
                    } else {
                        tokenText.toAmosCamelCase()
                    }
                }
                AmosTokenTypes.identifier,
                AmosTokenTypes.stringVariable,
                AmosTokenTypes.floatVariable -> {
                    if (inProcedureCallList) {
                        tokenText.uppercase(Locale.ROOT)
                    } else {
                        tokenText.uppercase(Locale.ROOT)
                    }
                }
                TokenType.BAD_CHARACTER,
                TokenType.WHITE_SPACE,
                AmosTokenTypes.number,
                AmosTokenTypes.string,
                AmosTokenTypes.comment,
                AmosTokenTypes.operator,
                AmosTokenTypes.comma,
                AmosTokenTypes.paren,
                null -> tokenText
                else -> tokenText
            }

            result.append(replacement)

            inProcedureCallList = when {
                currentWordUpper == "PROC" -> true
                tokenType == TokenType.WHITE_SPACE && (tokenText.contains('\n') || tokenText.contains('\r')) -> false
                tokenType == AmosTokenTypes.operator && tokenText == ":" -> false
                tokenType == TokenType.BAD_CHARACTER && (tokenText == "[" || tokenText == "]") -> false
                inProcedureCallList && tokenType == AmosTokenTypes.comma -> true
                inProcedureCallList && tokenType == TokenType.WHITE_SPACE -> true
                inProcedureCallList && currentWordUpper != null -> true
                inProcedureCallList -> false
                else -> false
            }

            if (currentWordUpper != null) {
                previousWordUpper = currentWordUpper
            }
            lexer.advance()
        }

        return result.toString()
    }

    private fun normalizeColonSpacing(source: String): String {
        val lexer = AmosLexer()
        lexer.start(source)

        data class Tok(val type: IElementType?, val text: String)

        val tokens = mutableListOf<Tok>()
        while (lexer.tokenType != null) {
            tokens += Tok(lexer.tokenType, source.substring(lexer.tokenStart, lexer.tokenEnd))
            lexer.advance()
        }

        val result = StringBuilder(source.length + 32)
        var stmtCount = 0
        var lastNonWsType: IElementType? = null
        var i = 0

        while (i < tokens.size) {
            val (type, text) = tokens[i]

            if (type == AmosTokenTypes.operator && text == ":") {
                val isLabelColon = stmtCount == 1 && lastNonWsType == AmosTokenTypes.identifier

                // Remove any trailing horizontal whitespace already in buffer
                while (result.isNotEmpty() && (result.last() == ' ' || result.last() == '\t')) {
                    result.deleteCharAt(result.length - 1)
                }

                if (isLabelColon) {
                    result.append(':')
                } else {
                    // Ensure one space before the colon (but not at start of line)
                    if (result.isNotEmpty() && result.last() != '\n' && result.last() != '\r') {
                        result.append(' ')
                    }
                    result.append(':')
                    // Skip following horizontal whitespace and emit exactly one space
                    val ni = i + 1
                    if (ni < tokens.size && tokens[ni].type == TokenType.WHITE_SPACE &&
                        !tokens[ni].text.contains('\n') && !tokens[ni].text.contains('\r')
                    ) {
                        i = ni
                    }
                    result.append(' ')
                }

                stmtCount = 0
                lastNonWsType = null
            } else {
                result.append(text)
                when {
                    type == TokenType.WHITE_SPACE && (text.contains('\n') || text.contains('\r')) -> {
                        stmtCount = 0
                        lastNonWsType = null
                    }

                    type != TokenType.WHITE_SPACE && type != null -> {
                        stmtCount++
                        lastNonWsType = type
                    }
                }
            }
            i++
        }

        return result.toString()
    }

    private fun applyBlockIndentation(source: String): String {
        val newline = detectLineSeparator(source)

        // Split and drop the trailing empty string that a trailing newline produces.
        // "a\nb\n".split('\n') → ["a","b",""] — the "" is the split artefact, not a real blank line.
        var lines = source.splitToSequence(Regex("\\r\\n|\\n|\\r")).toList()
        val hasTrailingLineBreak = lines.isNotEmpty() && lines.last().isEmpty()
        if (hasTrailingLineBreak) lines = lines.dropLast(1)

        val lineStarts = computeLineStarts(source)
        val statementKeyByLine = computeStatementKeyByLine(source, lineStarts)

        var indentLevel = 0
        val formattedLines = ArrayList<String>(lines.size)
        for ((lineIndex, line) in lines.withIndex()) {
            if (line.isBlank()) {
                formattedLines += ""
                continue
            }

            val statementKey = statementKeyByLine[lineIndex].orEmpty()
            val effectiveIndent = if (statementKey in closingStatements) {
                (indentLevel - 1).coerceAtLeast(0)
            } else {
                indentLevel
            }

            // trimStart for indentation, trimEnd to remove trailing whitespace
            val trimmed = line.trim()
            formattedLines += " ".repeat(effectiveIndent * blockIndentSize) + trimmed

            indentLevel = effectiveIndent
            if (statementKey in openingStatements) {
                indentLevel += 1
            }
        }

        // Remove trailing empty lines
        while (formattedLines.isNotEmpty() && formattedLines.last().isEmpty()) {
            formattedLines.removeAt(formattedLines.size - 1)
        }

        val rebuilt = formattedLines.joinToString(newline)
        return if (hasTrailingLineBreak) rebuilt + newline else rebuilt
    }

    private fun computeStatementKeyByLine(source: String, lineStarts: List<Int>): Map<Int, String> {
        val lineByOffset = buildLineByOffset(lineStarts)
        val keyByLine = mutableMapOf<Int, String>()

        AmosStatementSupport.splitStatements(source).forEach { segment ->
            if (!segment.startsAtLineStart) {
                return@forEach
            }

            val normalized = AmosStatementSupport.normalizeForAnalysis(segment)
            val statementKey = when {
                AmosStatementSupport.isInlineIfStatement(normalized) -> inlineIfStatement
                else -> AmosStatementSupport.statementKey(normalized)
            }
            if (statementKey.isEmpty()) {
                return@forEach
            }

            val line = lineByOffset(segment.startOffset)
            keyByLine.putIfAbsent(line, statementKey)
        }

        return keyByLine
    }

    private fun computeLineStarts(source: String): List<Int> {
        val starts = mutableListOf(0)
        var index = 0
        while (index < source.length) {
            when (source[index]) {
                '\n' -> starts += index + 1
                '\r' -> {
                    if (index + 1 < source.length && source[index + 1] == '\n') {
                        index++
                    }
                    starts += index + 1
                }
            }
            index++
        }
        return starts
    }

    private fun buildLineByOffset(lineStarts: List<Int>): (Int) -> Int {
        return { offset ->
            val insertion = lineStarts.binarySearch(offset)
            when {
                insertion >= 0 -> insertion
                else -> (-insertion - 2).coerceAtLeast(0)
            }
        }
    }

    private fun detectLineSeparator(source: String): String {
        return when {
            source.contains("\r\n") -> "\r\n"
            source.contains('\r') -> "\r"
            else -> "\n"
        }
    }

    private fun isFunctionCallLike(source: String, offset: Int): Boolean {
        var i = offset
        while (i < source.length && (source[i] == ' ' || source[i] == '\t')) {
            i++
        }
        return i < source.length && source[i] == '('
    }

    private fun String.toAmosCamelCase(): String {
        if (isEmpty()) {
            return this
        }

        val suffix = when {
            endsWith("$") || endsWith("#") -> takeLast(1)
            else -> ""
        }
        val core = if (suffix.isNotEmpty()) dropLast(1) else this

        val casedCore = core.split('/').joinToString("/") { segment ->
            if (segment.isEmpty()) {
                segment
            } else {
                segment.lowercase(Locale.ROOT).replaceFirstChar { ch ->
                    if (ch.isLowerCase()) ch.titlecase(Locale.ROOT) else ch.toString()
                }
            }
        }

        return casedCore + suffix
    }
}

















