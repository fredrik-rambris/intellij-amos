package dev.rambris.amigaamos.lang.amal

import com.intellij.psi.tree.IElementType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AmalLexerTest {
    @Test
    fun `single letter commands are recognized in compact forms`() {
        val source = "A 0,(1,2);L X=330;LY=Z(120)+20;M0,240,20;FR1=1TR0"
        val tokens = tokenize(source)

        assertTrue(tokens.any { it.first == AmalTokenTypes.keyword && it.second == "A" })
        assertTrue(tokens.any { it.first == AmalTokenTypes.keyword && it.second == "L" })
        assertTrue(tokens.any { it.first == AmalTokenTypes.keyword && it.second == "M" })
        assertTrue(tokens.any { it.first == AmalTokenTypes.keyword && it.second == "F" })
    }

    @Test
    fun `concatenated AMAL examples keep one letter commands as keywords`() {
        val source = readResource("/amal/test.amal")
        val tokens = tokenize(source)

        assertTrue(tokens.count { it.first == AmalTokenTypes.keyword && it.second == "L" } > 5)
        assertTrue(tokens.count { it.first == AmalTokenTypes.keyword && it.second == "M" } > 5)
    }

    @Test
    fun `pjl is tokenized as pause jump label`() {
        val source = "PJLinguini"
        val tokens = tokenize(source).filterNot { it.first == com.intellij.psi.TokenType.WHITE_SPACE }

        assertEquals(4, tokens.size)
        assertEquals(AmalTokenTypes.keyword, tokens[0].first)
        assertEquals("P", tokens[0].second)
        assertEquals(AmalTokenTypes.keyword, tokens[1].first)
        assertEquals("J", tokens[1].second)
        assertEquals(AmalTokenTypes.identifier, tokens[2].first)
        assertEquals("L", tokens[2].second)
        assertEquals(AmalTokenTypes.ignoredText, tokens[3].first)
        assertEquals("inguini", tokens[3].second)
    }

    @Test
    fun `long instruction is split into strong and continuation tokens`() {
        val source = "Move"
        val tokens = tokenize(source).filterNot { it.first == com.intellij.psi.TokenType.WHITE_SPACE }

        assertEquals(2, tokens.size)
        assertEquals(AmalTokenTypes.keyword, tokens[0].first)
        assertEquals("M", tokens[0].second)
        assertEquals(AmalTokenTypes.keywordContinuation, tokens[1].first)
        assertEquals("ove", tokens[1].second)
    }

    @Test
    fun `non matching lowercase suffix after instruction is ignored text`() {
        val source = "Moops"
        val tokens = tokenize(source).filterNot { it.first == com.intellij.psi.TokenType.WHITE_SPACE }

        assertEquals(2, tokens.size)
        assertEquals(AmalTokenTypes.keyword, tokens[0].first)
        assertEquals("M", tokens[0].second)
        assertEquals(AmalTokenTypes.ignoredText, tokens[1].first)
        assertEquals("oops", tokens[1].second)
    }

    @Test
    fun `canonical mixed case exit uses ignored leading lowercase then strong and continuation`() {
        val source = "eXit"
        val tokens = tokenize(source).filterNot { it.first == com.intellij.psi.TokenType.WHITE_SPACE }

        assertEquals(3, tokens.size)
        assertEquals(AmalTokenTypes.ignoredText, tokens[0].first)
        assertEquals("e", tokens[0].second)
        assertEquals(AmalTokenTypes.keyword, tokens[1].first)
        assertEquals("X", tokens[1].second)
        assertEquals(AmalTokenTypes.keywordContinuation, tokens[2].first)
        assertEquals("it", tokens[2].second)
    }

    @Test
    fun `non canonical exit spelling does not get canonical continuation`() {
        val source = "Exit"
        val tokens = tokenize(source).filterNot { it.first == com.intellij.psi.TokenType.WHITE_SPACE }

        assertEquals(2, tokens.size)
        assertEquals(AmalTokenTypes.keyword, tokens[0].first)
        assertEquals("E", tokens[0].second)
        assertEquals(AmalTokenTypes.ignoredText, tokens[1].first)
        assertEquals("xit", tokens[1].second)
    }

    @Test
    fun `canonical ymouse highlights significant letters and continuation`() {
        val source = "YMouse"
        val tokens = tokenize(source).filterNot { it.first == com.intellij.psi.TokenType.WHITE_SPACE }

        assertEquals(2, tokens.size)
        assertEquals(AmalTokenTypes.keyword, tokens[0].first)
        assertEquals("YM", tokens[0].second)
        assertEquals(AmalTokenTypes.keywordContinuation, tokens[1].first)
        assertEquals("ouse", tokens[1].second)
    }

    @Test
    fun `non canonical ymonkey keeps only significant prefix`() {
        val source = "YMonkey"
        val tokens = tokenize(source).filterNot { it.first == com.intellij.psi.TokenType.WHITE_SPACE }

        assertEquals(2, tokens.size)
        assertEquals(AmalTokenTypes.keyword, tokens[0].first)
        assertEquals("YM", tokens[0].second)
        assertEquals(AmalTokenTypes.ignoredText, tokens[1].first)
        assertEquals("onkey", tokens[1].second)
    }

    @Test
    fun `demo line uses significant tokens for instructions and labels`() {
        val source = "Label: Pause Jump Loop"
        val tokens = tokenize(source).filterNot { it.first == com.intellij.psi.TokenType.WHITE_SPACE }

        assertEquals(listOf(
            AmalTokenTypes.identifier to "L",
            AmalTokenTypes.ignoredText to "abel",
            AmalTokenTypes.separator to ":",
            AmalTokenTypes.keyword to "P",
            AmalTokenTypes.keywordContinuation to "ause",
            AmalTokenTypes.keyword to "J",
            AmalTokenTypes.keywordContinuation to "ump",
            AmalTokenTypes.identifier to "L",
            AmalTokenTypes.ignoredText to "oop"
        ), tokens)
    }

    @Test
    fun `demo text tokenizes pjl as two keywords plus label and ignored tail`() {
        val source = """
Label: Pause Jump Loop
Let Y=YMouse
Let Y=YMonkey
PJLinguini
Move R0,R1,RZ
""".trimIndent()

        val tokens = tokenize(source).filterNot { it.first == com.intellij.psi.TokenType.WHITE_SPACE }
        val expectedWindow = listOf(
            AmalTokenTypes.keyword to "P",
            AmalTokenTypes.keyword to "J",
            AmalTokenTypes.identifier to "L",
            AmalTokenTypes.ignoredText to "inguini"
        )

        val hasWindow = tokens.windowed(expectedWindow.size).any { it == expectedWindow }
        assertTrue(hasWindow, tokens.joinToString(" | ") { "${it.first}:${it.second}" })
    }

    @Test
    fun `js and jl are tokenized as compact jump plus label`() {
        val source = "L: L Y=Y+2;P;If Y>242 JS JL;S:"
        val tokens = tokenize(source).filterNot { it.first == com.intellij.psi.TokenType.WHITE_SPACE }

        val expectedJs = listOf(
            AmalTokenTypes.keyword to "J",
            AmalTokenTypes.identifier to "S"
        )
        val expectedJl = listOf(
            AmalTokenTypes.keyword to "J",
            AmalTokenTypes.identifier to "L"
        )

        assertTrue(tokens.windowed(expectedJs.size).any { it == expectedJs }, tokens.joinToString(" | ") { "${it.first}:${it.second}" })
        assertTrue(tokens.windowed(expectedJl.size).any { it == expectedJl }, tokens.joinToString(" | ") { "${it.first}:${it.second}" })
    }

    private fun tokenize(source: String): List<Pair<IElementType, String>> {
        val lexer = AmalLexer()
        lexer.start(source)

        val result = mutableListOf<Pair<IElementType, String>>()
        while (lexer.tokenType != null) {
            val type = lexer.tokenType
            if (type != null) {
                result += type to source.substring(lexer.tokenStart, lexer.tokenEnd)
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













