package dev.rambris.amigaamos.lang

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class AmosReferenceContributorTest : BasePlatformTestCase() {
    private val gotoHandler = AmosGotoDeclarationHandler()

    fun testGotoDeclarationOnProcedureCallResolvesToProcedureDefinition() {
        myFixture.configureByText(
            "sample.asc",
            """
            IN<caret>IT
            Procedure INIT
              Print "hello"
            End Proc
            """.trimIndent()
        )

        val source = myFixture.file.findElementAt(myFixture.caretOffset)
        assertNotNull(source)
        val targets = gotoHandler.getGotoDeclarationTargets(source, myFixture.caretOffset, myFixture.editor)

        assertNotNull(targets)
        assertEquals(1, targets!!.size)
        assertEquals("INIT", targets.single().text.uppercase())
        assertTrue(targets.single().textRange.startOffset > myFixture.caretOffset)
    }

    fun testGotoDeclarationOnVariableUsageResolvesToFirstDeclaration() {
        myFixture.configureByText(
            "sample.asc",
            """
            Dim M$(5)
            Print M<caret>$(1)
            """.trimIndent()
        )

        val source = myFixture.file.findElementAt(myFixture.caretOffset)
        assertNotNull(source)
        val targets = gotoHandler.getGotoDeclarationTargets(source, myFixture.caretOffset, myFixture.editor)

        assertNotNull(targets)
        assertEquals(1, targets!!.size)
        assertEquals("M$", targets.single().text.uppercase())
        assertEquals(4, targets.single().textRange.startOffset)
    }

    fun testGotoDeclarationOnGotoLabelResolvesToLabelDeclaration() {
        myFixture.configureByText(
            "sample.asc",
            """
            MAINMENU:
            Goto MAIN<caret>MENU
            """.trimIndent()
        )

        val source = myFixture.file.findElementAt(myFixture.caretOffset)
        assertNotNull(source)
        val table = AmosSymbolIndex.build(myFixture.file.text)
        assertTrue("labels=${table.labelDeclarations}", table.labelDeclarations.containsKey("MAINMENU"))
        val resolved = table.resolveOffset(
            myFixture.file.text,
            source!!.node.elementType,
            source.text,
            source.textRange.startOffset
        )
        assertEquals(0, resolved)
        val targets = gotoHandler.getGotoDeclarationTargets(source, myFixture.caretOffset, myFixture.editor)

        assertNotNull(
            "source='${source.text}' type=${source.node.elementType} offset=${source.textRange.startOffset} resolved=$resolved",
            targets
        )
        assertEquals(1, targets!!.size)
        assertEquals("MAINMENU", targets.single().text.uppercase())
        assertEquals(0, targets.single().textRange.startOffset)
    }

    fun testGotoDeclarationOnProcedureDeclarationReturnsUsages() {
        myFixture.configureByText(
            "sample.asc",
            """
            Proc INIT
            INIT
            Procedure IN<caret>IT
              Print "hello"
            End Proc
            """.trimIndent()
        )

        val source = myFixture.file.findElementAt(myFixture.caretOffset)
        assertNotNull(source)
        val targets = gotoHandler.getGotoDeclarationTargets(source, myFixture.caretOffset, myFixture.editor)

        assertNotNull(targets)
        assertEquals(2, targets!!.size)
        assertTrue(targets.all { it.text.equals("INIT", ignoreCase = true) })
        assertTrue(targets.all { it.textRange.startOffset < source!!.textRange.startOffset })
    }
}












