package dev.rambris.amigaamos.lang.amos

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class AmosRenameSupportTest : BasePlatformTestCase() {
    fun testRenameVariableRenamesDeclarationAndUsages() {
        val file = myFixture.configureByText(
            "sample.asc",
            """
            Dim M$(5)
            Print M$<caret>(1)
            """.trimIndent()
        )

        assertTrue(AmosRenameSupport.renameAtOffsetPreview(file, myFixture.caretOffset))

        val renamed = AmosRenameSupport.renameAtOffset(file, myFixture.caretOffset, "NAME$")

        assertTrue(renamed)
        myFixture.checkResult(
            """
            Dim NAME$(5)
            Print NAME$(1)
            """.trimIndent()
        )
    }

    fun testRenameProcedureRenamesDefinitionAndCalls() {
        val file = myFixture.configureByText(
            "sample.asc",
            """
            Proc IN<caret>IT
            Procedure INIT
              Print "hello"
            End Proc
            """.trimIndent()
        )

        val renamed = AmosRenameSupport.renameAtOffset(file, myFixture.caretOffset, "SETUP")

        assertTrue(renamed)
        myFixture.checkResult(
            """
            Proc SETUP
            Procedure SETUP
              Print "hello"
            End Proc
            """.trimIndent()
        )
    }

    fun testRenameLabelRenamesDeclarationAndGotoUsages() {
        val file = myFixture.configureByText(
            "sample.asc",
            """
            MAINMENU:
            Goto MAIN<caret>MENU
            """.trimIndent()
        )

        val renamed = AmosRenameSupport.renameAtOffset(file, myFixture.caretOffset, "START")

        assertTrue(renamed)
        myFixture.checkResult(
            """
            START:
            Goto START
            """.trimIndent()
        )
    }

    fun testRenameKeywordProcedureRenamesDefinitionAndCalls() {
        val file = myFixture.configureByText(
            "sample.asc",
            """
            Proc LI<caret>NE
            Procedure LINE
              Print "hello"
            End Proc
            """.trimIndent()
        )

        val renamed = AmosRenameSupport.renameAtOffset(file, myFixture.caretOffset, "DRAWLINE")

        assertTrue(renamed)
        myFixture.checkResult(
            """
            Proc DRAWLINE
            Procedure DRAWLINE
              Print "hello"
            End Proc
            """.trimIndent()
        )
    }
}



