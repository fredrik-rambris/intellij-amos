package dev.rambris.amigaamos.lang.amos

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class AmosEnterHandlerDelegateTest : BasePlatformTestCase() {

    fun testEnterAfterForInsertsNextWithLoopVariable() {
        myFixture.configureByText("sample.asc", "for x=1 to 100<caret>")

        myFixture.type("\n")

        myFixture.checkResult(
            """
            For X=1 To 100
               <caret>
            Next X
            """.trimIndent()
        )
    }

    fun testEnterAfterProcedureInsertsEndProc() {
        myFixture.configureByText("sample.asc", "procedure funstuff<caret>")

        myFixture.type("\n")

        myFixture.checkResult(
            """
            Procedure FUNSTUFF
               <caret>
            End Proc
            """.trimIndent()
        )
    }

    fun testEnterAfterWhileInsertsWend() {
        myFixture.configureByText("sample.asc", "while x<10<caret>")

        myFixture.type("\n")

        myFixture.checkResult(
            """
            While X<10
               <caret>
            Wend
            """.trimIndent()
        )
    }

    fun testEnterAfterRepeatInsertsUntilAndPlacesCaretAfterIt() {
        myFixture.configureByText("sample.asc", "repeat<caret>")

        myFixture.type("\n")

        // Body line keeps its 3-space indent; caret lands after "Until " ready to type condition
        myFixture.checkResult("Repeat\n   \nUntil <caret>")
    }

    fun testEnterAfterRepeatConditionLandsOnNextLine() {
        myFixture.configureByText("sample.asc", "Repeat\n   \nUntil X>10<caret>")

        myFixture.type("\n")

        myFixture.checkResult("Repeat\n   \nUntil X>10\n<caret>")
    }
}
