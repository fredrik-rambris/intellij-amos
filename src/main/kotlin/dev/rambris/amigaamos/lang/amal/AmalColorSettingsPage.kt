package dev.rambris.amigaamos.lang.amal

import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.options.colors.AttributesDescriptor
import com.intellij.openapi.options.colors.ColorDescriptor
import com.intellij.openapi.options.colors.ColorSettingsPage
import javax.swing.Icon

class AmalColorSettingsPage : ColorSettingsPage {
    override fun getDisplayName(): String = "AMAL"

    override fun getIcon(): Icon? = AmalFileType.icon

    override fun getHighlighter(): SyntaxHighlighter = AmalSyntaxHighlighter()

    override fun getDemoText(): String {
        return """
Label: Pause Jump Loop
Let Y=YMouse
Let Y=YMonkey
PJLinguini
Move R0,R1,RZ
L: L Y=Y+2;P;If Y>242 JS JL;S:A0,(17,2)(18,2);FR0=1T75;
""".trimIndent()
    }

    override fun getAdditionalHighlightingTagToDescriptorMap(): Map<String, TextAttributesKey>? = null

    override fun getAttributeDescriptors(): Array<AttributesDescriptor> = AMAL_ATTRIBUTE_DESCRIPTORS

    override fun getColorDescriptors(): Array<ColorDescriptor> = ColorDescriptor.EMPTY_ARRAY
}

private val AMAL_ATTRIBUTE_DESCRIPTORS = arrayOf(
    AttributesDescriptor("Keyword", AmalSyntaxHighlighter.KEYWORD),
    AttributesDescriptor("Keyword continuation", AmalSyntaxHighlighter.KEYWORD_CONTINUATION),
    AttributesDescriptor("Register", AmalSyntaxHighlighter.REGISTER),
    AttributesDescriptor("Label", AmalSyntaxHighlighter.LABEL),
    AttributesDescriptor("Identifier", AmalSyntaxHighlighter.IDENTIFIER),
    AttributesDescriptor("Ignored text", AmalSyntaxHighlighter.IGNORED_TEXT),
    AttributesDescriptor("Number", AmalSyntaxHighlighter.NUMBER),
    AttributesDescriptor("String", AmalSyntaxHighlighter.STRING),
    AttributesDescriptor("Operator", AmalSyntaxHighlighter.OPERATOR),
    AttributesDescriptor("Separator", AmalSyntaxHighlighter.SEPARATOR),
    AttributesDescriptor("Parentheses", AmalSyntaxHighlighter.PAREN),
    AttributesDescriptor("Bad character", AmalSyntaxHighlighter.BAD_CHAR)
)

