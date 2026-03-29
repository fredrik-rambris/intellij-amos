package dev.rambris.amigaamos.lang

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class AmosCompletionContributorTest : BasePlatformTestCase() {
    fun testVariableCompletionIncludesPreviouslyAssignedName() {
        myFixture.configureByText(
            "sample.asc",
            """
            A=44
            X=44
            B=33
            C=33
            A=33
            GG#=4.34
            THIS_IS_A_VARIABLE_TOO=444

            print THI<caret>
            """.trimIndent()
        )

        val lookupElements = myFixture.completeBasic()
        assertNotNull(lookupElements)
        assertContainsElements(lookupElements!!.map { it.lookupString }, "THIS_IS_A_VARIABLE_TOO")
    }

    fun testStringFunctionCompletionInsertsParentheses() {
        myFixture.configureByText(
            "sample.asc",
            "print MID<caret>"
        )

        val lookupElements = myFixture.completeBasic()
        assertNotNull(lookupElements)
        myFixture.lookup.currentItem = lookupElements!!.first { it.lookupString == "MID$" }
        myFixture.finishLookup('\n')

        myFixture.checkResult("print Mid$(<caret>)")
    }

    fun testInstructionCompletionInsertsCamelCaseWithTrailingSpace() {
        myFixture.configureByText(
            "sample.asc",
            "pri<caret>"
        )

        val lookupElements = myFixture.completeBasic()
        assertNotNull(lookupElements)
        myFixture.lookup.currentItem = lookupElements!!.first { it.lookupString == "PRINT" }
        myFixture.finishLookup('\n')

        myFixture.checkResult("Print <caret>")
    }

    fun testFunctionWithoutParensCompletionInsertsFormattedNameWithSpace() {
        myFixture.configureByText(
            "sample.asc",
            "x=tim<caret>"
        )

        val lookupElements = myFixture.completeBasic()
        assertNotNull(lookupElements)
        myFixture.lookup.currentItem = lookupElements!!.first { it.lookupString == "TIMER" }
        myFixture.finishLookup('\n')

        myFixture.checkResult("x=Timer <caret>")
    }

    fun testMidFirstParameterSuggestsStringVariables() {
        myFixture.configureByText(
            "sample.asc",
            """
            nvar$="name"
            nvar#=3.14
            nvar=44
            print MID$(<caret>)
            """.trimIndent()
        )

        val items = myFixture.completeBasic()
        assertNotNull(items)
        val lookupStrings = items!!.map { it.lookupString }
        assertContainsElements(lookupStrings, "NVAR$", "MID$", "LEFT$", "RIGHT$")
        assertFalse(lookupStrings.contains("NVAR"))
        assertFalse(lookupStrings.contains("NVAR#"))
    }

    fun testMidSecondParameterSuggestsWholeNumberVariables() {
        myFixture.configureByText(
            "sample.asc",
            """
            nvar$="name"
            nvar#=3.14
            nvar=44
            print MID$(NVAR$,<caret>)
            """.trimIndent()
        )

        val items = myFixture.completeBasic()
        assertNotNull(items)
        val lookupStrings = items!!.map { it.lookupString }
        assertContainsElements(lookupStrings, "NVAR", "LEN", "ASC", "INSTR")
        assertFalse(lookupStrings.contains("NVAR$"))
        assertFalse(lookupStrings.contains("NVAR#"))
        assertFalse(lookupStrings.contains("LEFT$"))
    }

    fun testWaitParameterSuggestsNumericOnly() {
        myFixture.configureByText(
            "sample.asc",
            """
            nvar$="name"
            nvar#=3.14
            nvar=44
            wait <caret>
            """.trimIndent()
        )

        val items = myFixture.completeBasic()
        assertNotNull(items)
        val lookupStrings = items!!.map { it.lookupString }
        assertContainsElements(lookupStrings, "NVAR", "LEN", "ASC", "INSTR")
        assertFalse(lookupStrings.contains("NVAR$"))
        assertFalse(lookupStrings.contains("LEFT$"))
    }

    fun testInkSecondParameterSuggestsNumericOnly() {
        myFixture.configureByText(
            "sample.asc",
            """
            nvar$="name"
            nvar#=3.14
            nvar=44
            ink 1,<caret>
            """.trimIndent()
        )

        val items = myFixture.completeBasic()
        assertNotNull(items)
        val lookupStrings = items!!.map { it.lookupString }
        assertContainsElements(lookupStrings, "NVAR", "LEN", "ASC")
        assertFalse(lookupStrings.contains("NVAR$"))
        assertFalse(lookupStrings.contains("MID$"))
    }

    fun testDrawKeywordParameterSuggestsToKeyword() {
        myFixture.configureByText(
            "sample.asc",
            """
            draw 50,50 <caret>
            """.trimIndent()
        )

        val items = myFixture.completeBasic()
        assertNotNull(items)
        val lookupStrings = items!!.map { it.lookupString }
        assertContainsElements(lookupStrings, "To")
        assertEquals(listOf("To"), lookupStrings.distinct())
    }

    fun testDrawCoordinatesAfterKeywordSuggestIntegers() {
        myFixture.configureByText(
            "sample.asc",
            """
            nvar$="name"
            nvar#=3.14
            nvar=44
            draw To <caret>
            """.trimIndent()
        )

        val items = myFixture.completeBasic()
        assertNotNull(items)
        val lookupStrings = items!!.map { it.lookupString }
        assertContainsElements(lookupStrings, "NVAR", "LEN", "ASC")
        assertFalse(lookupStrings.contains("NVAR$"))
        assertFalse(lookupStrings.contains("MID$"))
    }

    fun testNumberedLineStillOffersInstructionCompletion() {
        myFixture.configureByText(
            "sample.asc",
            "5 pri<caret>"
        )

        val items = myFixture.completeBasic()
        assertNotNull(items)
        assertContainsElements(items!!.map { it.lookupString }, "PRINT")
    }

    fun testGotoCompletionSuggestsLabelsBeforeStringValues() {
        myFixture.configureByText(
            "sample.asc",
            """
            MAINMENU:
            nvar$="name"
            goto <caret>
            """.trimIndent()
        )

        val items = myFixture.completeBasic()
        assertNotNull(items)
        val lookupStrings = items!!.map { it.lookupString }
        assertContainsElements(lookupStrings, "MAINMENU", "NVAR$", "LEFT$")

        val labelIndex = lookupStrings.indexOf("MAINMENU")
        val stringVariableIndex = lookupStrings.indexOf("NVAR$")
        val stringFunctionIndex = lookupStrings.indexOf("LEFT$")
        assertTrue(labelIndex in 0 until stringVariableIndex)
        assertTrue(stringVariableIndex in 0 until stringFunctionIndex)
    }

    fun testGosubCompletionSuggestsSymbolicAndNumericLabels() {
        myFixture.configureByText(
            "sample.asc",
            """
            MAINMENU:
            5 print "hello"
            gosub <caret>
            """.trimIndent()
        )

        val items = myFixture.completeBasic()
        assertNotNull(items)
        val lookupStrings = items!!.map { it.lookupString }
        assertContainsElements(lookupStrings, "MAINMENU", "5")
    }

    fun testGotoCompletionIgnoresInlineTrailingLabels() {
        myFixture.configureByText(
            "sample.asc",
            """
            print "hello": MAINMENU:
            goto <caret>
            """.trimIndent()
        )

        val items = myFixture.completeBasic()
        assertNotNull(items)
        val lookupStrings = items!!.map { it.lookupString }
        assertFalse(lookupStrings.contains("MAINMENU"))
    }

    fun testGotoCompletionDoesNotTreatStringLiteralTargetsAsDeclaredLabels() {
        myFixture.configureByText(
            "sample.asc",
            """
            MAINMENU:
            goto "MENUMAIN"
            goto <caret>
            """.trimIndent()
        )

        val items = myFixture.completeBasic()
        assertNotNull(items)
        val lookupStrings = items!!.map { it.lookupString }
        assertContainsElements(lookupStrings, "MAINMENU")
        assertFalse(lookupStrings.contains("MENUMAIN"))
    }

    fun testProcedureLocalVariableIsNotSuggestedAtRootScope() {
        myFixture.configureByText(
            "sample.asc",
            """
            Procedure TEST
              LOCAL_VALUE=1
            End Proc
            print <caret>
            """.trimIndent()
        )

        val items = myFixture.completeBasic()
        assertNotNull(items)
        assertFalse(items!!.map { it.lookupString }.contains("LOCAL_VALUE"))
    }

    fun testRootVariableIsNotVisibleInsideProcedureWithoutGlobalImport() {
        myFixture.configureByText(
            "sample.asc",
            """
            ROOT_VALUE=1
            Procedure TEST
              print <caret>
            End Proc
            """.trimIndent()
        )

        val items = myFixture.completeBasic()
        assertNotNull(items)
        assertFalse(items!!.map { it.lookupString }.contains("ROOT_VALUE"))
    }

    fun testGlobalOutsideAndInsideProcedureMakesVariableVisibleInProcedure() {
        myFixture.configureByText(
            "sample.asc",
            """
            ROOT_VALUE=1
            Global ROOT_VALUE
            Procedure TEST
              Global ROOT_VALUE
              print <caret>
            End Proc
            """.trimIndent()
        )

        val items = myFixture.completeBasic()
        assertNotNull(items)
        assertContainsElements(items!!.map { it.lookupString }, "ROOT_VALUE")
    }

    fun testForRangeKeywordCompletionSuggestsToOnExplicitCompletion() {
        myFixture.configureByText(
            "sample.asc",
            "for y=5 <caret>"
        )

        val items = myFixture.completeBasic()
        assertNotNull(items)
        val lookupStrings = items!!.map { it.lookupString }
        assertContainsElements(lookupStrings, "To")
    }

    fun testForRangeDoesNotAutoPopupAfterNumericLiteral() {
        myFixture.configureByText(
            "sample.asc",
            "for y=5<caret>"
        )

        myFixture.type(" ")

        myFixture.checkResult("for y=5 To <caret>")
        assertNull(myFixture.lookup)
    }
}








