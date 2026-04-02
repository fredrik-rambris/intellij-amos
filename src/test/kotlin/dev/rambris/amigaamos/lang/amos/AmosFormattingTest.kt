package dev.rambris.amigaamos.lang.amos

import kotlin.test.Test
import kotlin.test.assertEquals

class AmosFormattingTest {
    @Test
    fun formatterKeepsMnQuitStableAfterReformat() {
        val source = readResource("/MN_QUIT.Asc")
        val formatted = AmosCodeStyleFormatter.format(source)

        val normalizedSource = normalizeForComparison(source)
        val normalizedFormatted = normalizeForComparison(formatted)

        assertEquals(
            normalizedSource,
            normalizedFormatted,
            buildLineDiffMessage(normalizedSource, normalizedFormatted, maxDiffLines = 100),
        )
    }

    @Test
    fun formatterKeepsBnkCreateStableAfterReformat() {
        val source = readResource("/BNK_CREATE.Asc")
        val formatted = AmosCodeStyleFormatter.format(source)

        val normalizedSource = normalizeForComparison(source)
        val normalizedFormatted = normalizeForComparison(formatted)

        assertEquals(
            normalizedSource,
            normalizedFormatted,
            buildLineDiffMessage(normalizedSource, normalizedFormatted, maxDiffLines = 100),
        )
    }

    @Test
    fun formatterAppliesAmosCasingAndIndentation() {
        val source = """
            for x=1 to 10
            print x
            next x
        """.trimIndent()

        val expected = """
            For X=1 To 10
               Print X
            Next X
        """.trimIndent()

        assertEquals(expected, AmosCodeStyleFormatter.format(source))
    }

    @Test
    fun formatterIndentsBlockBodiesByThreeSpaces() {
        val source = """
            if a=1
            print a
            end if
        """.trimIndent()

        val expected = """
            If A=1
               Print A
            End If
        """.trimIndent()

        assertEquals(expected, AmosCodeStyleFormatter.format(source))
    }

    @Test
    fun formatterDoesNotIndentAfterSingleLineIfThen() {
        val source = """
            do
            if x>0 then x=x+1 : print x
            print "done"
            loop
        """.trimIndent()

        val expected = """
            Do
               If X>0 Then X=X+1 : Print X
               Print "done"
            Loop
        """.trimIndent()

        assertEquals(expected, AmosCodeStyleFormatter.format(source))
    }

    @Test
    fun formatterPascalCasesShortGrKeyword() {
        val source = "gr writing 2"
        val expected = "Gr Writing 2"

        assertEquals(expected, AmosCodeStyleFormatter.format(source))
    }

    @Test
    fun formatterKeepsProcedureDeclarationNamesUppercase() {
        val source = """
            procedure line
            gr writing 2
            end proc
        """.trimIndent()

        val expected = """
            Procedure LINE
               Gr Writing 2
            End Proc
        """.trimIndent()

        assertEquals(expected, AmosCodeStyleFormatter.format(source))
    }

    @Test
    fun formatterAddsSpacesAroundStatementSeparatorColon() {
        val source = "pen 14:paper 11"
        val expected = "Pen 14 : Paper 11"
        assertEquals(expected, AmosCodeStyleFormatter.format(source))
    }

    @Test
    fun formatterPreservesExistingSpacesAroundSeparatorColon() {
        val source = "pen 14 : paper 11"
        val expected = "Pen 14 : Paper 11"
        assertEquals(expected, AmosCodeStyleFormatter.format(source))
    }

    @Test
    fun formatterNormalizesMultipleSeparatorColonsOnOneLine() {
        val source = "pen 14:paper 11:locate 1,24"
        val expected = "Pen 14 : Paper 11 : Locate 1,24"
        assertEquals(expected, AmosCodeStyleFormatter.format(source))
    }

    @Test
    fun formatterDoesNotAddSpacesAroundLabelColon() {
        val source = "MAINMENU: goto MAINMENU"
        val expected = "MAINMENU: Goto MAINMENU"
        assertEquals(expected, AmosCodeStyleFormatter.format(source))
    }

    @Test
    fun formatterLabelColonFollowedBySeparatorColon() {
        val source = """
            MAINMENU: print "Hello":goto MAINMENU
        """.trimIndent()
        val expected = """
            MAINMENU: Print "Hello" : Goto MAINMENU
        """.trimIndent()
        assertEquals(expected, AmosCodeStyleFormatter.format(source))
    }

    @Test
    fun formatterKeepsKeywordProcedureNamesUppercaseInOnProcList() {
        val source = "else on m proc phill,line,sircle,rectangle"
        val expected = "Else On M Proc PHILL,LINE,SIRCLE,RECTANGLE"

        assertEquals(expected, AmosCodeStyleFormatter.format(source))
    }

    @Test
    fun formatterIsIdempotentWithTrailingNewline() {
        val source = "print x\n"
        val once = AmosCodeStyleFormatter.format(source)
        val twice = AmosCodeStyleFormatter.format(once)
        assertEquals(once, twice)
        assertEquals("Print X\n", once)
    }

    @Test
    fun formatterRemovesTrailingBlankLines() {
        val source = "print x\n\n\n"
        val result = AmosCodeStyleFormatter.format(source)
        assertEquals("Print X\n", result)
    }

    @Test
    fun formatterRemovesTrailingWhitespaceOnLines() {
        val source = "print x   \nprint y  "
        val result = AmosCodeStyleFormatter.format(source)
        assertEquals("Print X\nPrint Y", result)
    }

    @Test
    fun formatterKeepsLogicalOperatorsLowercase() {
        val source = "if x=1 and y=2 or z=3"
        val result = AmosCodeStyleFormatter.format(source)
        assertEquals("If X=1 and Y=2 or Z=3", result)
    }

    @Test
    fun formatterKeepsLineStringVariableUppercase() {
        val source = "line$=line$+\"!\""
        val result = AmosCodeStyleFormatter.format(source)
        assertEquals("LINE$=LINE$+\"!\"", result)
    }

    // --- helpers ---

    private fun normalizeForComparison(text: String): String {
        val lines = text
            .replace("\r\n", "\n")
            .replace('\r', '\n')
            .split('\n')
            .map { it.trimEnd(' ', '\t') }
            .toMutableList()
        while (lines.isNotEmpty() && lines.last().isEmpty()) lines.removeAt(lines.lastIndex)
        return lines.joinToString("\n")
    }

    private fun buildLineDiffMessage(expected: String, actual: String, maxDiffLines: Int): String {
        val expectedLines = expected.split('\n')
        val actualLines = actual.split('\n')
        val maxLineCount = maxOf(expectedLines.size, actualLines.size)
        val lines = mutableListOf<String>()
        var diffCount = 0
        for (lineIndex in 0 until maxLineCount) {
            val expectedLine = expectedLines.getOrNull(lineIndex)
            val actualLine = actualLines.getOrNull(lineIndex)
            if (expectedLine == actualLine) continue
            diffCount++
            if (diffCount > maxDiffLines) continue
            val markerColumn = firstMismatchColumn(expectedLine.orEmpty(), actualLine.orEmpty())
            lines += "line ${lineIndex + 1}:"
            lines += "  expected: ${expectedLine?.let { "'$it'" } ?: "<missing>"}"
            lines += "  actual  : ${actualLine?.let { "'$it'" } ?: "<missing>"}"
            lines += "  marker  : ${" ".repeat(markerColumn)}^"
        }
        if (diffCount == 0) return "No differing lines."
        val header = "Found $diffCount differing line(s). Showing up to $maxDiffLines."
        return buildString {
            appendLine(header)
            lines.forEach { appendLine(it) }
        }.trimEnd()
    }

    private fun firstMismatchColumn(expected: String, actual: String): Int {
        val minLength = minOf(expected.length, actual.length)
        for (index in 0 until minLength) {
            if (expected[index] != actual[index]) return index
        }
        return minLength
    }

    private fun readResource(path: String): String {
        val stream = this::class.java.getResourceAsStream(path)
        requireNotNull(stream) { "Missing test resource: $path" }
        return stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
    }
}
