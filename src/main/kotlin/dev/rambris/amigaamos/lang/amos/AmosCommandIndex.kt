package dev.rambris.amigaamos.lang.amos

object AmosCommandIndex {
    private val coreShortKeywords = setOf(
        "IF", "TO", "DO", "ON", "OR", "AND", "NOT", "MOD", "FN", "AS", "AT", "IN", "GR"
    )

    // Lexer keyword vocabulary: all known AMOS tokens classified as keyword tokens.
    val keywordsByWord: Set<String> by lazy {
        runCatching {
            AmosDefinitionRegistry.definitions
                .asSequence()
                .flatMap { it.uppercaseName.split(Regex("[^A-Z0-9_$#]+")) }
                .filter { token -> token.isNotBlank() && (token.length >= 3 || token in coreShortKeywords) }
                .toSet()
        }.getOrDefault(emptySet())
    }
}
