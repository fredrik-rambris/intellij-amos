package dev.rambris.amigaamos.lang

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class AmosFoldingBuilderTest : BasePlatformTestCase() {
    fun testFoldRegionsExistForBlocks() {
        val file = myFixture.configureByText(
            "sample.asc",
            """
            If A=1
              Print "one"
            Else
              Print "other"
            End If

            For I=0 To 10
              Print I
            Next I
            """.trimIndent()
        )

        val builder = AmosFoldingBuilder()
        val descriptors = builder.buildFoldRegions(file, myFixture.editor.document, false)

        val summary = descriptors.joinToString(" | ") {
            "${it.element.elementType}@${it.range.startOffset}-${it.range.endOffset}:${it.placeholderText}"
        }
        assertTrue(summary, descriptors.any { it.placeholderText == "If A=1" })
        assertTrue(summary, descriptors.any { it.placeholderText == "For I=0 To 10" })
    }

    fun testFoldRegionsExistForNumberedBlocks() {
        val file = myFixture.configureByText(
            "sample.asc",
            """
            10 If A=1
            20   Print "one"
            30 Else
            40   Print "other"
            50 End If
            """.trimIndent()
        )

        val builder = AmosFoldingBuilder()
        val descriptors = builder.buildFoldRegions(file, myFixture.editor.document, false)

        assertTrue(descriptors.any { it.placeholderText == "If A=1" })
    }

    fun testFoldRegionsExistForProcedures() {
        val file = myFixture.configureByText(
            "sample.asc",
            """
            Procedure DRAW_LINE[X,Y]
              Draw X,Y To X+10,Y+10
            End Proc
            """.trimIndent()
        )

        val builder = AmosFoldingBuilder()
        val descriptors = builder.buildFoldRegions(file, myFixture.editor.document, false)

        assertTrue(descriptors.any { it.placeholderText == "Procedure DRAW_LINE[X,Y]" })
    }
}















