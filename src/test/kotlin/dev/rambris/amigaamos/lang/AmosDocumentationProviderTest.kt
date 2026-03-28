package dev.rambris.amigaamos.lang

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AmosDocumentationProviderTest : BasePlatformTestCase() {
    fun testFunctionDocumentationComesFromRegistry() {
        myFixture.configureByText("sample.asc", "print MID$<caret>(\"abc\",1,1)")

        val element = myFixture.file.findElementAt(myFixture.caretOffset)
        val documentation = AmosDocumentationProvider().generateDoc(element, element)

        assertNotNull(documentation)
        assertTrue(documentation!!.contains("MID$"))
        assertTrue(documentation.contains("middle of a string", ignoreCase = true))
        assertTrue(documentation.contains("https://amospromanual.dev/05-02-string-functions.html#fn-mid-dollar"))
    }

    fun testSecondWordInMultiWordInstructionResolvesDocumentation() {
        myFixture.configureByText("sample.asc", "Set Buff<caret>er 13")

        val element = myFixture.file.findElementAt(myFixture.caretOffset)
        val documentation = AmosDocumentationProvider().generateDoc(element, element)

        assertNotNull(documentation)
        assertTrue(documentation!!.contains("SET BUFFER"))
        assertTrue(documentation.contains("first instruction", ignoreCase = true))
        assertTrue(documentation.contains("https://amospromanual.dev/05-01-the-bare-bones.html#i-set-buffer"))
    }

    fun testGlobeFunctionDocumentationComesFromRegistry() {
        myFixture.configureByText("sample.asc", "if Length<caret>(10)=0 then print \"missing\"")

        val element = myFixture.file.findElementAt(myFixture.caretOffset)
        val documentation = AmosDocumentationProvider().generateDoc(element, element)

        assertNotNull(documentation)
        assertTrue(documentation!!.contains("LENGTH"))
        assertTrue(documentation.contains("memory bank", ignoreCase = true))
        assertTrue(documentation.contains("https://amospromanual.dev/05-09-memory-banks.html#fn-length"))
    }

    fun testGlobeInstructionDocumentationComesFromRegistry() {
        myFixture.configureByText("sample.asc", "Screen Ope<caret>n 0,128,128,32,Lowres")

        val element = myFixture.file.findElementAt(myFixture.caretOffset)
        val documentation = AmosDocumentationProvider().generateDoc(element, element)

        assertNotNull(documentation)
        assertTrue(documentation!!.contains("SCREEN OPEN"))
        assertTrue(documentation.contains("open a new screen", ignoreCase = true))
        assertTrue(documentation.contains("https://amospromanual.dev/06-01-setting-up-screens.html#i-screen-open"))
    }

    fun testFirstWordInMultiWordInstructionResolvesDocumentation() {
        myFixture.configureByText("sample.asc", "Scr<caret>een Open 0,128,128,32,Lowres")

        val element = myFixture.file.findElementAt(myFixture.caretOffset)
        val documentation = AmosDocumentationProvider().generateDoc(element, element)

        assertNotNull(documentation)
        assertTrue(documentation!!.contains("SCREEN OPEN"))
        assertTrue(documentation.contains("open a new screen", ignoreCase = true))
        assertTrue(documentation.contains("https://amospromanual.dev/06-01-setting-up-screens.html#i-screen-open"))
    }

    fun testStandaloneScreenStillResolvesScreenDocumentation() {
        myFixture.configureByText("sample.asc", "Scr<caret>een 1")

        val element = myFixture.file.findElementAt(myFixture.caretOffset)
        val documentation = AmosDocumentationProvider().generateDoc(element, element)

        assertNotNull(documentation)
        assertTrue(documentation!!.contains("SCREEN"))
        assertTrue(!documentation.contains("SCREEN OPEN"))
        assertTrue(documentation.contains("current screen", ignoreCase = true))
        assertTrue(documentation.contains("https://amospromanual.dev/06-01-setting-up-screens.html#i-screen"))
    }

    fun testPiQuickDocumentationComesFromMathRegistry() {
        myFixture.configureByText("sample.asc", "print Pi<caret>#")

        val element = myFixture.file.findElementAt(myFixture.caretOffset)
        val documentation = AmosDocumentationProvider().generateDoc(element, element)

        assertNotNull(documentation)
        assertTrue(documentation!!.contains("PI#"))
        assertTrue(documentation.contains("constant π", ignoreCase = true) || documentation.contains("constant", ignoreCase = true))
        assertTrue(documentation.contains("https://amospromanual.dev/05-03-maths.html#fn-pi-pound"))
    }

    fun testRandomizeQuickDocumentationComesFromMathRegistry() {
        myFixture.configureByText("sample.asc", "Rando<caret>mize 1234")

        val element = myFixture.file.findElementAt(myFixture.caretOffset)
        val documentation = AmosDocumentationProvider().generateDoc(element, element)

        assertNotNull(documentation)
        assertTrue(documentation!!.contains("RANDOMIZE"))
        assertTrue(documentation.contains("random seed", ignoreCase = true))
        assertTrue(documentation.contains("https://amospromanual.dev/05-03-maths.html#i-randomize"))
    }

    fun testIfQuickDocumentationComesFromControlStructureRegistry() {
        myFixture.configureByText("sample.asc", "If<caret> X=1 Then Print \"one\"")

        val element = myFixture.file.findElementAt(myFixture.caretOffset)
        val documentation = AmosDocumentationProvider().generateDoc(element, element)

        assertNotNull(documentation)
        assertTrue(documentation!!.contains("IF"))
        assertTrue(documentation.contains("condition", ignoreCase = true))
        assertTrue(documentation.contains("https://amospromanual.dev/05-04-control-structures.html#str-if"))
    }

    fun testEndIfQuickDocumentationComesFromControlStructureRegistry() {
        myFixture.configureByText(
            "sample.asc",
            """
            If X=1
              Print \"one\"
            End<caret> If
            """.trimIndent()
        )

        val element = myFixture.file.findElementAt(myFixture.caretOffset)
        val documentation = AmosDocumentationProvider().generateDoc(element, element)

        assertNotNull(documentation)
        assertTrue(documentation!!.contains("END IF"))
        assertTrue(documentation.contains("structured test", ignoreCase = true))
        assertTrue(documentation.contains("https://amospromanual.dev/05-04-control-structures.html#str-end-if"))
    }

    fun testForQuickDocumentationComesFromControlStructureRegistry() {
        myFixture.configureByText("sample.asc", "For<caret> X=0 To 10")

        val element = myFixture.file.findElementAt(myFixture.caretOffset)
        val documentation = AmosDocumentationProvider().generateDoc(element, element)

        assertNotNull(documentation)
        assertTrue(documentation!!.contains("FOR"))
        assertTrue(documentation.contains("specific number of times", ignoreCase = true))
        assertTrue(documentation.contains("https://amospromanual.dev/05-04-control-structures.html#str-for"))
    }

    fun testGlobalQuickDocumentationIncludesProcedureScopeDetails() {
        myFixture.configureByText("sample.asc", "Glob<caret>al A,B")

        val element = myFixture.file.findElementAt(myFixture.caretOffset)
        val documentation = AmosDocumentationProvider().generateDoc(element, element)

        assertNotNull(documentation)
        assertTrue(documentation!!.contains("GLOBAL"))
        assertTrue(documentation.contains("outside procedures", ignoreCase = true))
        assertTrue(documentation.contains("inside a procedure", ignoreCase = true))
        assertTrue(documentation.contains("https://amospromanual.dev/05-05-procedures.html#str-global"))
    }

    fun testLocateQuickDocumentationComesFromTextRegistry() {
        myFixture.configureByText("sample.asc", "Loc<caret>ate 10,5")

        val element = myFixture.file.findElementAt(myFixture.caretOffset)
        val documentation = AmosDocumentationProvider().generateDoc(element, element)

        assertNotNull(documentation)
        assertTrue(documentation!!.contains("LOCATE"))
        assertTrue(documentation.contains("cursor", ignoreCase = true))
        assertTrue(documentation.contains("https://amospromanual.dev/05-06-text.html#i-locate"))
    }

    fun testBorderFunctionQuickDocumentationComesFromTextRegistry() {
        myFixture.configureByText("sample.asc", "print Bord<caret>er$(\"Hi\",2)")

        val element = myFixture.file.findElementAt(myFixture.caretOffset)
        val documentation = AmosDocumentationProvider().generateDoc(element, element)

        assertNotNull(documentation)
        assertTrue(documentation!!.contains("BORDER$"))
        assertTrue(documentation.contains("bordered", ignoreCase = true))
        assertTrue(documentation.contains("https://amospromanual.dev/05-06-text.html#fn-border-dollar"))
    }

    fun testWindOpenQuickDocumentationComesFromWindowsRegistry() {
        myFixture.configureByText("sample.asc", "Wind Ope<caret>n 1,10,10,40,12")

        val element = myFixture.file.findElementAt(myFixture.caretOffset)
        val documentation = AmosDocumentationProvider().generateDoc(element, element)

        assertNotNull(documentation)
        assertTrue(documentation!!.contains("WIND OPEN"))
        assertTrue(documentation.contains("window", ignoreCase = true))
        assertTrue(documentation.contains("https://amospromanual.dev/05-07-windows.html#i-wind-open"))
    }

    fun testJoyQuickDocumentationComesFromJoystickRegistry() {
        myFixture.configureByText("sample.asc", "if Jo<caret>y(1) then print \"go\"")

        val element = myFixture.file.findElementAt(myFixture.caretOffset)
        val documentation = AmosDocumentationProvider().generateDoc(element, element)

        assertNotNull(documentation)
        assertTrue(documentation!!.contains("JOY"))
        assertTrue(documentation.contains("joystick", ignoreCase = true))
        assertTrue(documentation.contains("https://amospromanual.dev/05-08-the-joystick-and-mouse.html#fn-joy"))
    }

    fun testBlengthQuickDocumentationComesFromBankRegistry() {
        myFixture.configureByText("sample.asc", "print Blen<caret>gth(1)")

        val element = myFixture.file.findElementAt(myFixture.caretOffset)
        val documentation = AmosDocumentationProvider().generateDoc(element, element)

        assertNotNull(documentation)
        assertTrue(documentation!!.contains("BLENGTH"))
        assertTrue(documentation.contains("bank", ignoreCase = true))
        assertTrue(documentation.contains("https://amospromanual.dev/05-09-memory-banks.html#fn-blength"))
    }

    fun testScreenWidthQuickDocumentationComesFromScreenSetupRegistry() {
        myFixture.configureByText("sample.asc", "print Screen Wid<caret>th(0)")

        val element = myFixture.file.findElementAt(myFixture.caretOffset)
        val documentation = AmosDocumentationProvider().generateDoc(element, element)

        assertNotNull(documentation)
        assertTrue(documentation!!.contains("SCREEN WIDTH"))
        assertTrue(documentation.contains("width", ignoreCase = true))
        assertTrue(documentation.contains("https://amospromanual.dev/06-01-setting-up-screens.html#fn-screen-width"))
    }

    fun testScreenCopyQuickDocumentationComesFromUsingScreensRegistry() {
        myFixture.configureByText("sample.asc", "Screen Cop<caret>y 0 To 1")

        val element = myFixture.file.findElementAt(myFixture.caretOffset)
        val documentation = AmosDocumentationProvider().generateDoc(element, element)

        assertNotNull(documentation)
        assertTrue(documentation!!.contains("SCREEN COPY"))
        assertTrue(documentation.contains("copy", ignoreCase = true))
        assertTrue(documentation.contains("https://amospromanual.dev/06-02-using-screens.html#i-screen-copy"))
    }

    fun testSpackQuickDocumentationComesFromCompactExtensionRegistry() {
        myFixture.configureByText("sample.asc", "Spa<caret>ck 0 To 20")

        val element = myFixture.file.findElementAt(myFixture.caretOffset)
        val documentation = AmosDocumentationProvider().generateDoc(element, element)

        assertNotNull(documentation)
        assertTrue(documentation!!.contains("SPACK"))
        assertTrue(documentation.contains("extension:", ignoreCase = true))
        assertTrue(documentation.contains("Picture compactor extension"))
        assertTrue(documentation.contains("compact", ignoreCase = true))
        assertTrue(documentation.contains("https://amospromanual.dev/06-02-using-screens.html#picoext-spack"))
    }
}



