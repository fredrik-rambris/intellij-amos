package dev.rambris.amigaamos.lang.amal

object AmalVocabulary {
    data class InstructionStyleMatch(
        val leadingIgnoredLength: Int,
        val strongLength: Int,
        val continuationLength: Int
    )

    private val fullInstructionSpellings = setOf(
        "Anim",
        "AUtotest",
        "Direct",
        "End",
        "eXit",
        "For",
        "If",
        "Jump",
        "Let",
        "Move",
        "Next",
        "On",
        "Pause",
        "PLay",
        "To",
        "Wait"
    )

    private val abbreviatedInstructionSpellings = setOf(
        "A",
        "AU",
        "D",
        "E",
        "F",
        "I",
        "J",
        "L",
        "M",
        "N",
        "O",
        "P",
        "PL",
        "T",
        "W",
        "X"
    )

    private val functionSpellings = setOf(
        "BC",
        "C",
        "J0",
        "J1",
        "K1",
        "K2",
        "SC",
        "VU",
        "XH",
        "XM",
        "XS",
        "YH",
        "YM",
        "YS",
        "Z"
    )

    private val longFunctionSpellings = setOf(
        "BobCol",
        "Col",
        "SpriteCol",
        "XHard",
        "XMouse",
        "YHard",
        "YMouse"
    )

    private val registers = buildSet {
        add("A")
        add("X")
        add("Y")
        for (digit in '0'..'9') {
            add("R$digit")
        }
        for (letter in 'A'..'Z') {
            add("R$letter")
        }
    }

    private data class CanonicalInstruction(
        val full: String,
        val leading: String,
        val abbreviation: String,
        val trailing: String
    )

    private val canonicalInstructions = fullInstructionSpellings
        .map { full ->
            val firstUpper = full.indexOfFirst { it.isUpperCase() }
            val lastUpper = full.indexOfLast { it.isUpperCase() }
            val leading = if (firstUpper > 0) full.substring(0, firstUpper) else ""
            val abbreviation = if (firstUpper >= 0) full.substring(firstUpper, lastUpper + 1) else full
            val trailing = if (lastUpper + 1 < full.length) full.substring(lastUpper + 1) else ""
            CanonicalInstruction(full, leading, abbreviation, trailing)
        }
        .sortedWith(compareByDescending<CanonicalInstruction> { it.abbreviation.length }.thenByDescending { it.full.length })

    private val canonicalFunctions = longFunctionSpellings
        .map { full ->
            val firstUpper = full.indexOfFirst { it.isUpperCase() }
            val lastUpper = full.indexOfLast { it.isUpperCase() }
            val leading = if (firstUpper > 0) full.substring(0, firstUpper) else ""
            val abbreviation = if (firstUpper >= 0) full.substring(firstUpper, lastUpper + 1) else full
            val trailing = if (lastUpper + 1 < full.length) full.substring(lastUpper + 1) else ""
            CanonicalInstruction(full, leading, abbreviation, trailing)
        }
        .sortedWith(compareByDescending<CanonicalInstruction> { it.abbreviation.length }.thenByDescending { it.full.length })

    fun isFullInstruction(word: String): Boolean = word in fullInstructionSpellings

    fun isAbbreviatedInstruction(word: String): Boolean = word in abbreviatedInstructionSpellings

    fun isFunction(word: String): Boolean = word in functionSpellings

    fun isLongFunctionSpelling(word: String): Boolean = word in longFunctionSpellings

    fun isRegister(word: String): Boolean = word in registers

    fun isIgnoredText(word: String): Boolean {
        if (word.isEmpty()) {
            return false
        }
        return word.all { it.isLowerCase() || it.isDigit() || it == '_' }
    }

    fun isPotentialLabelName(word: String): Boolean {
        if (word.isEmpty() || !word.first().isUpperCase()) {
            return false
        }
        return word.drop(1).all { it.isLowerCase() || it.isDigit() || it == '_' }
    }

    fun canonicalInstructionMatch(word: String): InstructionStyleMatch? {
        return canonicalMatch(word, canonicalInstructions)
    }

    fun canonicalFunctionMatch(word: String): InstructionStyleMatch? {
        return canonicalMatch(word, canonicalFunctions)
    }

    private fun canonicalMatch(word: String, candidates: List<CanonicalInstruction>): InstructionStyleMatch? {
        for (instruction in candidates) {
            val abbreviationIndex = word.indexOf(instruction.abbreviation)
            if (abbreviationIndex < 0) {
                continue
            }

            val leadingCandidate = word.substring(0, abbreviationIndex)
            if (leadingCandidate != instruction.leading || !leadingCandidate.all { it.isLowerCase() }) {
                continue
            }

            val continuationCandidate = word.substring(abbreviationIndex + instruction.abbreviation.length)
            val continuationLength = if (instruction.trailing.isNotEmpty() && continuationCandidate.startsWith(instruction.trailing)) {
                instruction.trailing.length
            } else {
                0
            }

            return InstructionStyleMatch(
                leadingIgnoredLength = leadingCandidate.length,
                strongLength = instruction.abbreviation.length,
                continuationLength = continuationLength
            )
        }

        return null
    }
}

