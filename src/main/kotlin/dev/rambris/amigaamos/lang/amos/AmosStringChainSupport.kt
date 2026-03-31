package dev.rambris.amigaamos.lang.amos

import java.util.Locale

data class AmosStringChain(
    val variableName: String,
    val startOffset: Int,
    val endOffset: Int,
    val parts: List<String>
) {
    val partCount: Int = parts.size

    fun joinedContent(): String = parts.joinToString(separator = "")
}

object AmosStringChainSupport {
    private val initialAssignmentPattern = Regex(
        pattern = """^([A-Za-z_][A-Za-z0-9_]*\$)(?:\s*\([^\n\r)]*\))?\s*=\s*\"([^\"]*)\"\s*$"""
    )
    private val appendAssignmentPattern = Regex(
        pattern = """^([A-Za-z_][A-Za-z0-9_]*\$)(?:\s*\([^\n\r)]*\))?\s*=\s*([A-Za-z_][A-Za-z0-9_]*\$)(?:\s*\([^\n\r)]*\))?\s*\+\s*\"([^\"]*)\"\s*$"""
    )

    fun chainsInSource(source: String): List<AmosStringChain> {
        val statements = AmosStatementSupport.splitStatements(source)
        val result = mutableListOf<AmosStringChain>()
        var index = 0

        while (index < statements.size) {
            val normalized = AmosStatementSupport.normalizeForAnalysis(statements[index])
            val headMatch = initialAssignmentPattern.matchEntire(normalized)
            if (headMatch == null) {
                index++
                continue
            }

            val variableName = headMatch.groupValues[1].uppercase(Locale.ROOT)
            val parts = mutableListOf(headMatch.groupValues[2])
            var cursor = index + 1
            var lastAppendIndex = -1

            while (cursor < statements.size) {
                val nextNormalized = AmosStatementSupport.normalizeForAnalysis(statements[cursor])
                if (isIgnorable(nextNormalized)) {
                    cursor++
                    continue
                }

                val appendMatch = appendAssignmentPattern.matchEntire(nextNormalized)
                if (appendMatch == null) {
                    break
                }

                val assignedVar = appendMatch.groupValues[1].uppercase(Locale.ROOT)
                val referencedVar = appendMatch.groupValues[2].uppercase(Locale.ROOT)
                if (assignedVar != variableName || referencedVar != variableName) {
                    break
                }

                parts += appendMatch.groupValues[3]
                lastAppendIndex = cursor
                cursor++
            }

            if (lastAppendIndex >= 0) {
                result += AmosStringChain(
                    variableName = variableName,
                    startOffset = statements[index].startOffset,
                    endOffset = statements[lastAppendIndex].endOffset,
                    parts = parts
                )
                index = lastAppendIndex + 1
                continue
            }

            index++
        }

        return result
    }

    fun chainAtOffset(source: String, offset: Int): AmosStringChain? {
        return chainsInSource(source).firstOrNull { chain ->
            offset in chain.startOffset..chain.endOffset
        }
    }

    fun continuationVariableAtOffset(source: String, offset: Int): String? {
        val clampedOffset = offset.coerceIn(0, source.length)

        chainAtOffset(source, clampedOffset)?.let { return it.variableName }

        return chainsInSource(source)
            .asSequence()
            .filter { clampedOffset >= it.endOffset }
            .filter { chain -> hasOnlyIgnorableContent(source.substring(chain.endOffset, clampedOffset)) }
            .lastOrNull()
            ?.variableName
    }

    fun continuationSnippet(variableName: String): String {
        return "$variableName=$variableName+\"\""
    }

    private fun isIgnorable(normalizedStatement: String): Boolean {
        if (normalizedStatement.isBlank()) {
            return true
        }

        if (normalizedStatement.startsWith("'")) {
            return true
        }

        val uppercase = normalizedStatement.uppercase(Locale.ROOT)
        return uppercase == "REM" || uppercase.startsWith("REM ") || uppercase.startsWith("REM\t")
    }

    private fun hasOnlyIgnorableContent(text: String): Boolean {
        if (text.isBlank()) {
            return true
        }

        return AmosStatementSupport.splitStatements(text).all { statement ->
            isIgnorable(AmosStatementSupport.normalizeForAnalysis(statement))
        }
    }
}


