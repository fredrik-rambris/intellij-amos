package dev.rambris.amigaamos.lang.amal

import java.util.Locale

data class AmalDocEntry(
    val symbol: String,
    val kind: String,
    val summary: String,
    val link: String
)

object AmalDocumentationCatalog {
    private const val MANUAL_BASE = "https://amospromanual.dev/07-06-amal.html"

    private val entriesBySymbol = linkedMapOf(
        // Instructions and structures
        "MOVE" to entry("Move", "instruction", "Move an object by horizontal and vertical offsets over a number of steps.", "amali-move"),
        "ANIM" to entry("Anim", "instruction", "Animate an object by cycling through image and delay pairs.", "amali-anim"),
        "JUMP" to entry("Jump", "instruction", "Jump to a label in the AMAL program.", "amali-jump"),
        "LET" to entry("Let", "instruction", "Assign a value to an AMAL register.", "amali-let"),
        "IF" to entry("If", "structure", "Evaluate a test and jump when the result is true.", "amals-if"),
        "FOR" to entry("For", "structure", "Start a counted loop in AMAL.", "amals-for"),
        "TO" to entry("To", "structure", "Loop range separator used by FOR ... TO ... NEXT.", "amals-to"),
        "NEXT" to entry("Next", "structure", "End a FOR loop and continue until the limit is reached.", "amals-next"),
        "AUTOTEST" to entry("AUtotest", "instruction", "Define a test list that runs before the main AMAL stream.", "amali-autotest"),
        "DIRECT" to entry("Direct", "instruction", "Set which main program part runs after an Autotest.", "amali-direct"),
        "END" to entry("End", "instruction", "Terminate the AMAL program.", "amali-end"),
        "EXIT" to entry("eXit", "instruction", "Exit the current Autotest and re-enter the AMAL program.", "amali-exit"),
        "ON" to entry("On", "instruction", "Activate the main program after a Wait command.", "i-on-amal"),
        "PAUSE" to entry("Pause", "instruction", "Pause execution until the next vertical blank.", "amali-pause"),
        "WAIT" to entry("Wait", "instruction", "Freeze the main AMAL program and execute only the Autotest.", "i-wait-amal"),
        "PLAY" to entry("PLay", "instruction", "Play a movement path stored in an AMAL bank.", "amali-play"),

        // Functions
        "BC" to entry("BC", "function", "Bob collision test function.", "amalf-bc"),
        "C" to entry("C", "function", "Return collision status after BC/SC tests.", "amalf-c"),
        "J0" to entry("J0", "function", "Read right joystick status bit-map.", "amalf-j0"),
        "J1" to entry("J1", "function", "Read left joystick status bit-map.", "amalf-j1"),
        "K1" to entry("K1", "function", "Return left mouse key state.", "amalf-k1"),
        "K2" to entry("K2", "function", "Return right mouse key state.", "amalf-k2"),
        "SC" to entry("SC", "function", "Sprite collision test function.", "amalf-sc"),
        "VU" to entry("VU", "function", "Sample audio channel intensity.", "fn-vu-amal"),
        "XH" to entry("XH", "function", "Convert screen X coordinate to hardware X coordinate.", "amalf-xh"),
        "XM" to entry("XM", "function", "Return mouse X coordinate in hardware coordinates.", "amalf-xm"),
        "XS" to entry("XS", "function", "Convert hardware X coordinate to screen X coordinate.", "amalf-xs"),
        "YH" to entry("YH", "function", "Convert screen Y coordinate to hardware Y coordinate.", "amalf-yh"),
        "YM" to entry("YM", "function", "Return mouse Y coordinate in hardware coordinates.", "amalf-ym"),
        "YS" to entry("YS", "function", "Convert hardware Y coordinate to screen Y coordinate.", "amalf-ys"),
        "Z" to entry("Z", "function", "Return a pseudo-random value using the given bit mask.", "amalf-z")
    )

    private val aliases = mapOf(
        "A" to "ANIM",
        "AU" to "AUTOTEST",
        "D" to "DIRECT",
        "E" to "END",
        "F" to "FOR",
        "I" to "IF",
        "J" to "JUMP",
        "L" to "LET",
        "M" to "MOVE",
        "N" to "NEXT",
        "O" to "ON",
        "P" to "PAUSE",
        "PL" to "PLAY",
        "T" to "TO",
        "W" to "WAIT",
        "X" to "EXIT",
        // Common compact shorthand used in sample programs.
        "JL" to "JUMP",
        // Canonical long function spellings.
        "BOBCOL" to "BC",
        "COL" to "C",
        "SPRITECOL" to "SC",
        "XHARD" to "XH",
        "XMOUSE" to "XM",
        "YHARD" to "YH",
        "YMOUSE" to "YM"
    )

    private val canonicalLongFormsBySymbol = mapOf(
        "JUMP" to "JUMP",
        "LET" to "LET",
        "MOVE" to "MOVE",
        "PAUSE" to "PAUSE",
        "WAIT" to "WAIT",
        "ANIM" to "ANIM",
        "AUTOTEST" to "AUTOTEST",
        "DIRECT" to "DIRECT",
        "END" to "END",
        "EXIT" to "EXIT",
        "FOR" to "FOR",
        "IF" to "IF",
        "NEXT" to "NEXT",
        "ON" to "ON",
        "PLAY" to "PLAY",
        "TO" to "TO",
        "XM" to "XMOUSE",
        "YM" to "YMOUSE",
        "XH" to "XHARD",
        "YH" to "YHARD",
        "BC" to "BOBCOL",
        "SC" to "SPRITECOL",
        "C" to "COL"
    )

    private val docSymbolsByLengthDesc = (entriesBySymbol.keys + aliases.keys)
        .distinct()
        .sortedByDescending { it.length }

    fun resolveAtWord(word: String, caretIndexInWord: Int = 0): AmalDocEntry? {
        if (word.isBlank()) {
            return null
        }

        val normalized = word.uppercase(Locale.ROOT)

        if (normalized.startsWith("PJ") && normalized.length >= 2) {
            return when (caretIndexInWord) {
                0 -> resolveExact("P")
                1 -> resolveExact("J")
                else -> null
            }
        }

        resolveExact(normalized)?.let { return it }

        val prefixed = resolveByPrefix(normalized, caretIndexInWord) ?: return null
        return resolveExact(prefixed)
    }

    private fun resolveExact(symbol: String): AmalDocEntry? {
        val canonical = aliases[symbol] ?: symbol
        return entriesBySymbol[canonical]
    }

    private fun resolveByPrefix(word: String, caretIndexInWord: Int): String? {
        for (symbol in docSymbolsByLengthDesc) {
            if (!word.startsWith(symbol) || word.length == symbol.length) {
                continue
            }

            if (caretIndexInWord >= symbol.length) {
                val longForm = canonicalLongFormsBySymbol[symbol]
                if (longForm == null || !word.startsWith(longForm) || caretIndexInWord >= longForm.length) {
                    continue
                }
            }

            val nextChar = word[symbol.length]
            if (nextChar.isUpperCase() || nextChar.isDigit() || nextChar == '_') {
                return symbol
            }
        }
        return null
    }

    private fun entry(symbol: String, kind: String, summary: String, anchor: String): AmalDocEntry {
        return AmalDocEntry(symbol, kind, summary, "$MANUAL_BASE#$anchor")
    }
}



