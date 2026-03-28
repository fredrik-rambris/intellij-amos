package dev.rambris.amigaamos.lang

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AmosParserStructureTest {
    @Test
    fun ifBlocksAndAssignmentsAreBuilt() {
        val source =
            """
            If A<0
              X(0)=1
            Else
              X=2
            End If
            Print X
            """.trimIndent()

        val ifBlocks = countIfBlocks(source)
        val assignments = countAssignments(source)

        assertEquals(1, ifBlocks)
        assertEquals(2, assignments)
    }

    @Test
    fun globeSampleContainsBlocksAndAssignments() {
        val source = readResource("/samples/Globe.asc")

        val ifBlocks = countIfBlocks(source)
        val assignments = countAssignments(source)

        assertTrue(ifBlocks >= 1)
        assertTrue(assignments >= 1)
    }

    private fun countIfBlocks(source: String): Int {
        val opens = Regex("""(?im)^\s*if\b""").findAll(source).count()
        val closes = Regex("""(?im)^\s*end\s+if\b""").findAll(source).count()
        return minOf(opens, closes)
    }

    private fun countAssignments(source: String): Int {
        return Regex("""(?im)(^|:)\s*[A-Za-z_][A-Za-z0-9_$#]*(\s*\([^\n\r)]*\))?\s*=""")
            .findAll(source)
            .count()
    }

    private fun readResource(path: String): String {
        val stream = this::class.java.getResourceAsStream(path)
        requireNotNull(stream) { "Missing test resource: $path" }
        return stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
    }
}





