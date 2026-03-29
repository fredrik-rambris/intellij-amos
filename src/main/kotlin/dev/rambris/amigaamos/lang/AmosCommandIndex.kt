package dev.rambris.amigaamos.lang

import java.util.Locale

data class AmosCommand(val name: String, val kind: String)

object AmosCommandIndex {
    private val coreShortKeywords = setOf(
        "IF", "TO", "DO", "ON", "OR", "AND", "NOT", "MOD", "FN", "AS", "AT", "IN", "GR"
    )

    val commands: List<AmosCommand> by lazy {
        val stream = AmosCommandIndex::class.java.classLoader.getResourceAsStream("amos/commands.tsv")
            ?: return@lazy emptyList()

        stream.bufferedReader(Charsets.UTF_8).useLines { lines ->
            lines
                .mapNotNull { line ->
                    val parts = line.split('\t', limit = 2)
                    if (parts.size != 2) return@mapNotNull null
                    val name = parts[0].trim()
                    val kind = parts[1].trim()
                    if (name.isEmpty() || kind.isEmpty()) return@mapNotNull null
                    AmosCommand(name, kind)
                }
                .toList()
        }
    }

    val keywordsByWord: Set<String> by lazy {
        commands
            .asSequence()
            .flatMap { it.name.uppercase(Locale.ROOT).split(Regex("[^A-Z0-9_$#]+")) }
            .filter { token ->
                token.isNotBlank() && (token.length >= 3 || token in coreShortKeywords)
            }
            .toSet()
    }
}
