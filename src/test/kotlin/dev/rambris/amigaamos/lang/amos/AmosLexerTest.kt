package dev.rambris.amigaamos.lang.amos

import com.intellij.psi.tree.IElementType
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AmosLexerTest {
    @Test
    fun `globe sample contains expected keyword and hex tokens`() {
        val source = readResource("/samples/Globe.asc")
        val tokens = tokenize(source)

        assertTrue(tokens.any { it.first == AmosTokenTypes.keyword && it.second.equals("screen", ignoreCase = true) })
        assertTrue(tokens.any { it.first == AmosTokenTypes.keyword && it.second.equals("iff", ignoreCase = true) })
        assertTrue(tokens.any { it.first == AmosTokenTypes.number && it.second.startsWith("$") })
    }

    @Test
    fun `rem and apostrophe comments are lexed as comments`() {
        val source = "print \"ok\" : rem inline\n' line comment\n"
        val tokens = tokenize(source)

        assertTrue(tokens.any { it.first == AmosTokenTypes.comment && it.second.startsWith("rem", ignoreCase = true) })
        assertTrue(tokens.any { it.first == AmosTokenTypes.comment && it.second.startsWith("'") })
    }

    @Test
    fun `apostrophe after colon is not a valid comment marker`() {
        val source = "print \"ok\" : ' illegal\n"
        val tokens = tokenize(source)

        assertFalse(tokens.any { it.first == AmosTokenTypes.comment && it.second.startsWith("'") })
    }

    @Test
    fun `typed variables and bare bones commands are recognized`() {
        val source = """
            name$="amos"
            real_number#=3.14
            score=100
            set buffer 13
            def fn x(a,b,c)=a+b+c
            print fn x(1,2,3)
            dim artist$(99),price#(99),year(99)
        """.trimIndent()

        val tokens = tokenize(source)

        assertTrue(tokens.any { it.first == AmosTokenTypes.stringVariable && it.second.equals("name$", ignoreCase = true) })
        assertTrue(tokens.any { it.first == AmosTokenTypes.floatVariable && it.second.equals("real_number#", ignoreCase = true) })
        assertTrue(tokens.any { it.first == AmosTokenTypes.identifier && it.second.equals("score", ignoreCase = true) })
        assertTrue(tokens.any { it.first == AmosTokenTypes.keyword && it.second.equals("set", ignoreCase = true) })
        assertTrue(tokens.any { it.first == AmosTokenTypes.keyword && it.second.equals("buffer", ignoreCase = true) })
        assertTrue(tokens.any { it.first == AmosTokenTypes.keyword && it.second.equals("def", ignoreCase = true) })
        assertTrue(tokens.any { it.first == AmosTokenTypes.keyword && it.second.equals("fn", ignoreCase = true) })
        assertTrue(tokens.any { it.first == AmosTokenTypes.keyword && it.second.equals("dim", ignoreCase = true) })
    }

    @Test
    fun `chapter 5 1 control and system commands are recognized`() {
        val source = """
            wait 100
            end
            stop
            edit
            direct
            break off
            break on
            system
            print free
        """.trimIndent()

        val tokens = tokenize(source)

        assertTrue(tokens.any { it.first == AmosTokenTypes.keyword && it.second.equals("wait", ignoreCase = true) })
        assertTrue(tokens.any { it.first == AmosTokenTypes.keyword && it.second.equals("end", ignoreCase = true) })
        assertTrue(tokens.any { it.first == AmosTokenTypes.keyword && it.second.equals("stop", ignoreCase = true) })
        assertTrue(tokens.any { it.first == AmosTokenTypes.keyword && it.second.equals("edit", ignoreCase = true) })
        assertTrue(tokens.any { it.first == AmosTokenTypes.keyword && it.second.equals("direct", ignoreCase = true) })
        assertTrue(tokens.any { it.first == AmosTokenTypes.keyword && it.second.equals("break", ignoreCase = true) })
        assertTrue(tokens.any { it.first == AmosTokenTypes.keyword && it.second.equals("off", ignoreCase = true) })
        assertTrue(tokens.any { it.first == AmosTokenTypes.keyword && it.second.equals("on", ignoreCase = true) })
        assertTrue(tokens.any { it.first == AmosTokenTypes.keyword && it.second.equals("system", ignoreCase = true) })
        assertTrue(tokens.any { it.first == AmosTokenTypes.keyword && it.second.equals("free", ignoreCase = true) })
    }

    @Test
    fun `single letter names stay variables and hash names stay float variables`() {
        val source = "A=44\nX=44\nGG#=4.34\n"
        val tokens = tokenize(source)

        assertTrue(tokens.any { it.first == AmosTokenTypes.identifier && it.second == "A" })
        assertTrue(tokens.any { it.first == AmosTokenTypes.identifier && it.second == "X" })
        assertTrue(tokens.any { it.first == AmosTokenTypes.floatVariable && it.second == "GG#" })
        assertFalse(tokens.any { it.first == AmosTokenTypes.keyword && (it.second == "A" || it.second == "X") })
    }

    @Test
    fun `labels and colon separators are lexed without bad characters`() {
        val source = "MAINMENU: Print \"Hello\": Goto MAINMENU\n5 Print \"Again\"\nGoto 5\nGoto \"MAINMENU\"\nGoto LEFT$(A$,1)\n"
        val tokens = tokenize(source)

        assertTrue(tokens.count { it.first == AmosTokenTypes.operator && it.second == ":" } >= 2)
        assertTrue(tokens.any { it.first == AmosTokenTypes.identifier && it.second == "MAINMENU" })
        assertTrue(tokens.any { it.first == AmosTokenTypes.number && it.second == "5" })
        assertTrue(tokens.any { it.first == AmosTokenTypes.string && it.second == "\"MAINMENU\"" })
        assertFalse(tokens.any { it.first == AmosTokenTypes.badCharacter })
    }

    private fun tokenize(source: String): List<Pair<IElementType, String>> {
        val lexer = AmosLexer()
        lexer.start(source)

        val result = mutableListOf<Pair<IElementType, String>>()
        while (lexer.tokenType != null) {
            val type = lexer.tokenType
            if (type != null) {
                val text = source.substring(lexer.tokenStart, lexer.tokenEnd)
                result += type to text
            }
            lexer.advance()
        }

        return result
    }

    private fun readResource(path: String): String {
        val stream = this::class.java.getResourceAsStream(path)
        requireNotNull(stream) { "Missing test resource: $path" }
        return stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
    }
}




