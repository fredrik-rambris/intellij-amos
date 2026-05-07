package dev.rambris.amigaamos.lang.amos

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AmosFunctionRegistryTest {
    @Test
    fun `mid function has expected signatures`() {
        val signatures = AmosFunctionRegistry.signaturesFor("MID$")

        assertEquals(2, signatures.size)
        assertTrue(signatures.any { it.presentation == "Mid$(source$, offset)" })
        assertTrue(signatures.any { it.presentation == "Mid$(source$, offset, number)" })
    }

    @Test
    fun `call context finds mid current parameter`() {
        val source = "Print MID$(A$,6,<caret>)"
        val offset = source.indexOf("<caret>")
        val cleanSource = source.replace("<caret>", "")

        val context = AmosFunctionRegistry.findCallContext(cleanSource, offset)

        assertNotNull(context)
        assertEquals("MID$", context.name)
        assertEquals(2, context.currentParameterIndex)
    }

    @Test
    fun `mid parameter types are exposed`() {
        assertEquals(AmosValueType.STRING, AmosFunctionRegistry.expectedParameterType("MID$", 0))
        assertEquals(AmosValueType.INTEGER, AmosFunctionRegistry.expectedParameterType("MID$", 1))
        assertEquals(AmosValueType.INTEGER, AmosFunctionRegistry.expectedParameterType("MID$", 2))
    }

    @Test
    fun `instruction call context found for Screen Open first parameter`() {
        val source = "Screen Open <caret>0,320,256,2,Lowres"
        val offset = source.indexOf("<caret>")
        val clean = source.replace("<caret>", "")

        val ctx = AmosFunctionRegistry.findInstructionCallContext(clean, offset, null)

        assertNotNull(ctx)
        assertEquals("SCREEN OPEN", ctx.name)
        assertEquals(0, ctx.currentParameterIndex)
    }

    @Test
    fun `instruction call context finds correct parameter index by comma count`() {
        val source = "Screen Open 0,320<caret>,256,2,Lowres"
        val offset = source.indexOf("<caret>")
        val clean = source.replace("<caret>", "")

        val ctx = AmosFunctionRegistry.findInstructionCallContext(clean, offset, null)

        assertNotNull(ctx)
        assertEquals("SCREEN OPEN", ctx.name)
        assertEquals(1, ctx.currentParameterIndex)
    }

    @Test
    fun `instruction call context resets across statement separator`() {
        val source = "Screen Open 0,320,256,2,Lowres : Screen Display 0,0,<caret>0,320,256"
        val offset = source.indexOf("<caret>")
        val clean = source.replace("<caret>", "")

        val ctx = AmosFunctionRegistry.findInstructionCallContext(clean, offset, null)

        assertNotNull(ctx)
        assertEquals("SCREEN DISPLAY", ctx.name)
        assertEquals(2, ctx.currentParameterIndex)
    }

    @Test
    fun `call context finds multi-word function name`() {
        val source = "D=Dialog Box(1,1<caret>,Resource\$(60))"
        val offset = source.indexOf("<caret>")
        val clean = source.replace("<caret>", "")

        val ctx = AmosFunctionRegistry.findCallContext(clean, offset)

        assertNotNull(ctx)
        assertEquals("DIALOG BOX", ctx.name)
        assertEquals(1, ctx.currentParameterIndex)
    }

    @Test
    fun `instruction call context returns null for assignment statement`() {
        val source = "A=<caret>42"
        val offset = source.indexOf("<caret>")
        val clean = source.replace("<caret>", "")

        val ctx = AmosFunctionRegistry.findInstructionCallContext(clean, offset, null)

        kotlin.test.assertNull(ctx)
    }
}

