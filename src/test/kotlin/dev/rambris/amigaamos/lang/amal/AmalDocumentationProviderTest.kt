package dev.rambris.amigaamos.lang.amal

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class AmalDocumentationProviderTest : BasePlatformTestCase() {
    fun testJumpInstructionDocumentationIsAvailable() {
        myFixture.configureByText("sample.amal", "J<caret>ump Loop")

        val provider = AmalDocumentationProvider()
        val element = provider.getCustomDocumentationElement(myFixture.editor, myFixture.file, null, myFixture.caretOffset)
        val documentation = provider.generateDoc(element, element)

        assertNotNull(documentation)
        assertTrue(documentation!!.contains("Jump"))
        assertTrue(documentation.contains("amali-jump"))
    }

    fun testCompactJumpLabelShorthandResolvesDocumentation() {
        myFixture.configureByText("sample.amal", "J<caret>L")

        val provider = AmalDocumentationProvider()
        val element = provider.getCustomDocumentationElement(myFixture.editor, myFixture.file, null, myFixture.caretOffset)
        val documentation = provider.generateDoc(element, element)

        assertNotNull(documentation)
        assertTrue(documentation!!.contains("Jump"))
        assertTrue(documentation.contains("amali-jump"))
    }

    fun testPauseJumpLoopShorthandResolvesDocumentation() {
        myFixture.configureByText("sample.amal", "<caret>PJL")

        val provider = AmalDocumentationProvider()
        val element = provider.getCustomDocumentationElement(myFixture.editor, myFixture.file, null, myFixture.caretOffset)
        val documentation = provider.generateDoc(element, element)

        assertNotNull(documentation)
        assertTrue(documentation!!.contains("Pause"))
        assertTrue(documentation.contains("amali-pause"))
    }

    fun testPauseJumpLoopShorthandResolvesJumpOnSecondLetter() {
        myFixture.configureByText("sample.amal", "P<caret>JL")

        val provider = AmalDocumentationProvider()
        val element = provider.getCustomDocumentationElement(myFixture.editor, myFixture.file, null, myFixture.caretOffset)
        val documentation = provider.generateDoc(element, element)

        assertNotNull(documentation)
        assertTrue(documentation!!.contains("Jump"))
        assertTrue(documentation.contains("amali-jump"))
    }

    fun testPauseJumpLoopShorthandLabelParameterHasNoInstructionDoc() {
        myFixture.configureByText("sample.amal", "PJ<caret>L")

        val provider = AmalDocumentationProvider()
        val element = provider.getCustomDocumentationElement(myFixture.editor, myFixture.file, null, myFixture.caretOffset)
        val documentation = provider.generateDoc(element, element)

        assertNull(documentation)
    }

    fun testFunctionDocumentationIsAvailable() {
        myFixture.configureByText("sample.amal", "Let X=X<caret>M")

        val provider = AmalDocumentationProvider()
        val element = provider.getCustomDocumentationElement(myFixture.editor, myFixture.file, null, myFixture.caretOffset)
        val documentation = provider.generateDoc(element, element)

        assertNotNull(documentation)
        assertTrue(documentation!!.contains("XM"))
        assertTrue(documentation.contains("amalf-xm"))
    }

    fun testCanonicalLongFunctionDocumentationIsAvailable() {
        myFixture.configureByText("sample.amal", "Let Y=YMo<caret>use")

        val provider = AmalDocumentationProvider()
        val element = provider.getCustomDocumentationElement(myFixture.editor, myFixture.file, null, myFixture.caretOffset)
        val documentation = provider.generateDoc(element, element)

        assertNotNull(documentation)
        assertTrue(documentation!!.contains("YM"))
        assertTrue(documentation.contains("amalf-ym"))
    }

    fun testNonCanonicalLongFunctionTailHasNoDocumentation() {
        myFixture.configureByText("sample.amal", "Let Y=YMo<caret>nkey")

        val provider = AmalDocumentationProvider()
        val element = provider.getCustomDocumentationElement(myFixture.editor, myFixture.file, null, myFixture.caretOffset)
        val documentation = provider.generateDoc(element, element)

        assertNull(documentation)
    }
}






