package dev.rambris.amigaamos.lang.amal

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AmalColorSettingsPageTest {
    @Test
    fun `amal color settings page exposes expected descriptors`() {
        val page = AmalColorSettingsPage()

        assertEquals("AMAL", page.displayName)
        val descriptorKeys = page.attributeDescriptors.map { it.key }.toSet()

        assertTrue(AmalSyntaxHighlighter.KEYWORD in descriptorKeys)
        assertTrue(AmalSyntaxHighlighter.KEYWORD_CONTINUATION in descriptorKeys)
        assertTrue(AmalSyntaxHighlighter.REGISTER in descriptorKeys)
        assertTrue(AmalSyntaxHighlighter.LABEL in descriptorKeys)
        assertTrue(AmalSyntaxHighlighter.IGNORED_TEXT in descriptorKeys)
        assertTrue(page.demoText.contains("Pause Jump Loop"))
    }
}

