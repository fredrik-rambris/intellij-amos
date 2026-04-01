package dev.rambris.amigaamos.lang.amos

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class AmosSetBufferPositionInspectionTest : BasePlatformTestCase() {
    fun testSetBufferAfterExecutableStatementIsReported() {
        myFixture.enableInspections(AmosSetBufferPositionInspection())
        myFixture.configureByText(
            "sample.asc",
            """
            Print "hi"
            <warning descr="Set Buffer must be the first instruction (except Rem lines)">Set Buffer 13</warning>
            """.trimIndent()
        )
        myFixture.checkHighlighting()
    }

    fun testSetBufferAfterRemIsAllowed() {
        myFixture.enableInspections(AmosSetBufferPositionInspection())
        myFixture.configureByText(
            "sample.asc",
            """
            Rem comment
            Set Buffer 13
            Print "ok"
            """.trimIndent()
        )
        myFixture.checkHighlighting()
    }

    fun testSetBufferAfterLabelDeclarationIsAllowed() {
        myFixture.enableInspections(AmosSetBufferPositionInspection())
        myFixture.configureByText(
            "sample.asc",
            """
            MainMenu:
            Set Buffer 13
            Print "ok"
            """.trimIndent()
        )
        myFixture.checkHighlighting()
    }

    fun testNumberedSetBufferLineIsStillRecognized() {
        myFixture.enableInspections(AmosSetBufferPositionInspection())
        myFixture.configureByText(
            "sample.asc",
            """
            Print "hi"
            10 <warning descr="Set Buffer must be the first instruction (except Rem lines)">Set Buffer 13</warning>
            """.trimIndent()
        )
        myFixture.checkHighlighting()
    }
}
