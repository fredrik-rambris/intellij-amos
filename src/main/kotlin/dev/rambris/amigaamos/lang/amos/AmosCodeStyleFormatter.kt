package dev.rambris.amigaamos.lang.amos

import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType
import java.util.Locale

object AmosCodeStyleFormatter {
    private const val blockIndentSize = AmosStatementSupport.blockIndentSize
    private const val inlineIfStatement = "IF INLINE"
    private val lowerCaseLogicalOperators = setOf("AND", "OR", "MOD")

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
                    } else if (tokenUpper == "LINE$" && previousWordUpper != "COMMAND") {
                        tokenUpper
                    } else if (tokenText == tokenUpper) {
                        tokenText
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
                    } else if (tokenText.any { it.isUpperCase() }) {
                        tokenText
                    } else if (tokenText.uppercase(Locale.ROOT) in lowerCaseLogicalOperators) {
                        tokenText.lowercase(Locale.ROOT)
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
        var separatorCountOnLine = 0
        var lastNonWsType: IElementType? = null
        var i = 0

        while (i < tokens.size) {
            val (type, text) = tokens[i]

            if (type == AmosTokenTypes.operator && text == ":") {
                val hasWhitespaceBeforeColon = i > 0 &&
                    tokens[i - 1].type == TokenType.WHITE_SPACE &&
                    !tokens[i - 1].text.contains('\n') &&
                    !tokens[i - 1].text.contains('\r')
                val isLabelColon = separatorCountOnLine == 0 &&
                    stmtCount == 1 &&
                    lastNonWsType == AmosTokenTypes.identifier &&
                    !hasWhitespaceBeforeColon

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
                separatorCountOnLine++
            } else {
                result.append(text)
                when {
                    type == TokenType.WHITE_SPACE && (text.contains('\n') || text.contains('\r')) -> {
                        stmtCount = 0
                        lastNonWsType = null
                        separatorCountOnLine = 0
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
            val trimmed = line.trim()
            val leadingClosers = countLeadingInlineClosers(trimmed)
            val effectiveIndent = (indentLevel - leadingClosers).coerceAtLeast(0)

            // trimStart for indentation, trimEnd to remove trailing whitespace
            formattedLines += " ".repeat(effectiveIndent * blockIndentSize) + trimmed

            indentLevel = effectiveIndent
            if (statementKey in openingStatements) {
                indentLevel += 1
            }
            val ignoredLeadingClosers = if (statementKey in closingStatements) {
                (leadingClosers - 1).coerceAtLeast(0)
            } else {
                0
            }
            indentLevel += computeAdditionalInlineDelta(trimmed, ignoredLeadingClosers)
            if (indentLevel < 0) {
                indentLevel = 0
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

            // If an opener has its matching closer on the same line (colon-separated inline block),
            // treat it as inline so we don't incorrectly indent subsequent lines.
            val effectiveKey = if (statementKey in openingStatements && segment.endsWithColonSeparator) {
                val lineEnd = lineStarts.getOrElse(line + 1) { source.length }
                val lineText = source.substring(lineStarts[line], lineEnd)
                if (hasMatchingInlineCloser(statementKey, lineText)) inlineIfStatement else statementKey
            } else {
                statementKey
            }

            keyByLine.putIfAbsent(line, effectiveKey)
        }

        return keyByLine
    }

    private fun hasMatchingInlineCloser(statementKey: String, lineText: String): Boolean {
        val pattern = when (statementKey) {
            "IF", "ELSE IF" -> ":\\s*end\\s+if\\b"
            "FOR" -> ":\\s*next\\b"
            "WHILE" -> ":\\s*wend\\b"
            "REPEAT" -> ":\\s*until\\b"
            "DO" -> ":\\s*loop\\b"
            "PROCEDURE" -> ":\\s*end\\s+proc\\b"
            else -> null
        } ?: return false
        return Regex(pattern, RegexOption.IGNORE_CASE).containsMatchIn(lineText)
    }

    private fun computeAdditionalInlineDelta(line: String, ignoredLeadingClosers: Int = 0): Int {
        val segments = AmosStatementSupport.splitStatements(line)
        if (segments.size <= 1) {
            return 0
        }

        val firstNormalized = AmosStatementSupport.normalizeForAnalysis(segments.first())
        val firstRawKey = when {
            AmosStatementSupport.isInlineIfStatement(firstNormalized) -> inlineIfStatement
            else -> AmosStatementSupport.statementKey(firstNormalized)
        }

        var delta = 0
        var remainingIgnoredLeadingClosers = ignoredLeadingClosers
        var scanningLeadingClosers = ignoredLeadingClosers > 0
        val pendingInlineClosers = mutableMapOf<String, Int>()
        if (firstRawKey in openingStatements && hasMatchingInlineCloser(firstRawKey, line)) {
            val closerKey = matchingCloserFor(firstRawKey)
            if (closerKey.isNotEmpty()) {
                pendingInlineClosers[closerKey] = pendingInlineClosers.getOrDefault(closerKey, 0) + 1
            }
        }
        for (segment in segments.drop(1)) {
            val normalized = AmosStatementSupport.normalizeForAnalysis(segment)
            if (normalized.isEmpty()) {
                continue
            }

            val rawKey = when {
                AmosStatementSupport.isInlineIfStatement(normalized) -> inlineIfStatement
                else -> AmosStatementSupport.statementKey(normalized)
            }
            if (rawKey.isEmpty() || rawKey == inlineIfStatement) {
                continue
            }

            val key = when {
                segment.endsWithColonSeparator && rawKey in openingStatements && hasMatchingInlineCloser(rawKey, line) -> inlineIfStatement
                else -> rawKey
            }
            if (key == inlineIfStatement) {
                val closerKey = matchingCloserFor(rawKey)
                if (closerKey.isNotEmpty()) {
                    pendingInlineClosers[closerKey] = pendingInlineClosers.getOrDefault(closerKey, 0) + 1
                }
                continue
            }

            if (scanningLeadingClosers) {
                if (key in closingStatements && remainingIgnoredLeadingClosers > 0) {
                    remainingIgnoredLeadingClosers--
                    continue
                }
                scanningLeadingClosers = false
            }

            val pending = pendingInlineClosers[key] ?: 0
            if (key in closingStatements && pending > 0) {
                if (pending == 1) {
                    pendingInlineClosers.remove(key)
                } else {
                    pendingInlineClosers[key] = pending - 1
                }
                continue
            }

            if (key in closingStatements) {
                delta -= 1
            }
            if (key in openingStatements) {
                delta += 1
            }
        }

        return delta
    }

    private fun matchingCloserFor(openingKey: String): String {
        return when (openingKey) {
            "IF", "ELSE IF" -> "END IF"
            "FOR" -> "NEXT"
            "WHILE" -> "WEND"
            "REPEAT" -> "UNTIL"
            "DO" -> "LOOP"
            "PROCEDURE" -> "END PROC"
            else -> ""
        }
    }

    private fun countLeadingInlineClosers(line: String): Int {
        var count = 0
        for (segment in AmosStatementSupport.splitStatements(line)) {
            val normalized = AmosStatementSupport.normalizeForAnalysis(segment)
            if (normalized.isEmpty()) {
                continue
            }
            val key = AmosStatementSupport.statementKey(normalized)
            if (key in closingStatements) {
                count++
            } else {
                break
            }
        }
        return count
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

















