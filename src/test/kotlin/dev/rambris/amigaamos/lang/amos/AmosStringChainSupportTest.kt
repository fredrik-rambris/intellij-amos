package dev.rambris.amigaamos.lang.amos

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class AmosStringChainSupportTest {
    @Test
    fun `chain detection ignores blank lines and comments`() {
        val source =
            """
            A$="INit box"
            REM Do main stuff
            A$=A$+"LAbel Main"
            A$=A$+"STuff"

            ' Loop back
            A$=A$+"Jump Main"
            A$=A$+"End"
            """.trimIndent()

        val chains = AmosStringChainSupport.chainsInSource(source)
        assertEquals(1, chains.size)
        assertEquals("A$", chains.single().variableName)
        assertEquals(5, chains.single().partCount)
        assertEquals("INit boxLAbel MainSTuffJump MainEnd", chains.single().joinedContent())
    }

    @Test
    fun `chain detection stops on non-append statement`() {
        val source =
            """
            A$="One"
            A$=A$+"Two"
            Print "Break"
            A$=A$+"Three"
            """.trimIndent()

        val chains = AmosStringChainSupport.chainsInSource(source)
        assertEquals(1, chains.size)
        assertEquals("OneTwo", chains.single().joinedContent())
    }

    @Test
    fun `chain at offset resolves within chain range`() {
        val source =
            """
            A$="One"
            A$=A$+"Two"
            """.trimIndent()

        val onChain = AmosStringChainSupport.chainAtOffset(source, source.indexOf("Two"))
        assertNotNull(onChain)
        assertEquals("OneTwo", onChain.joinedContent())

        val outside = AmosStringChainSupport.chainAtOffset(source, source.length + 1)
        assertNull(outside)
    }

    @Test
    fun `continuation variable is found after chain with comments`() {
        val source =
            """
            Q$="FirstLine"
            Q$=Q$+"Secondline"
            REM context line
            ' another comment

            """.trimIndent() + "\n"

        val variable = AmosStringChainSupport.continuationVariableAtOffset(source, source.length)
        assertEquals("Q$", variable)
    }
}


