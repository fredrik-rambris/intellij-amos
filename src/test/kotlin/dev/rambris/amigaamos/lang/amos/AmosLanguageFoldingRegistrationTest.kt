package dev.rambris.amigaamos.lang.amos

import com.intellij.lang.folding.LanguageFolding
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class AmosLanguageFoldingRegistrationTest : BasePlatformTestCase() {
    fun testAmosFoldingBuilderIsRegisteredAndBuildsDescriptors() {
        val file = myFixture.configureByText(
            "sample.asc",
            """
            If A=1
              Print "one"
            Else
              Print "other"
            End If
            """.trimIndent()
        )

        val builder = LanguageFolding.INSTANCE.forLanguage(AmosLanguage)
        val allBuilders = LanguageFolding.INSTANCE.allForLanguage(AmosLanguage)
        assertNotNull(builder)
        assertTrue(allBuilders.joinToString { it.javaClass.name }, allBuilders.any { it is AmosFoldingBuilder })

        val descriptors = LanguageFolding.buildFoldingDescriptors(builder, file, myFixture.editor.document, false)
        assertTrue(descriptors.isNotEmpty())
    }
}



