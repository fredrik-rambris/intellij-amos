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
}

