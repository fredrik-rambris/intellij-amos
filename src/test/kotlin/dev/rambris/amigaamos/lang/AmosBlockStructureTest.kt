package dev.rambris.amigaamos.lang

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class AmosBlockStructureTest : BasePlatformTestCase() {
    fun testNestedBlocksAreParsed() {
        val file = myFixture.configureByText(
            "sample.asc",
            """
            If A=1
              For I=0 To 10
                Print I
              Next I
            Else
              While A<10
                Inc A
              Wend
            End If
            """.trimIndent()
        )

        assertTrue(file.text.contains("End If"))
        assertTrue(file.text.contains("Next I"))
        assertTrue(file.text.contains("Wend"))
        assertTrue(file.node.textLength > 0)
    }
}








