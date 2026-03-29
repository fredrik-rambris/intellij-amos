package dev.rambris.amigaamos.lang

import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.options.colors.AttributesDescriptor
import com.intellij.openapi.options.colors.ColorDescriptor
import com.intellij.openapi.options.colors.ColorSettingsPage
import javax.swing.Icon

class AmosColorSettingsPage : ColorSettingsPage {
    override fun getDisplayName(): String = "AMOS"

    override fun getIcon(): Icon? = AmosFileType.icon

    override fun getHighlighter(): SyntaxHighlighter = AmosSyntaxHighlighter()

    override fun getDemoText(): String {
        return """
Procedure DEMO
   SCORE=10
   NAME$="AMOS"
   RATE#=3.14
   If SCORE>0 and RATE#>1 or NAME$="AMOS"
      Print "Hello" : Goto DEMO
   End If
End Proc
""".trimIndent()
    }

    override fun getAdditionalHighlightingTagToDescriptorMap(): Map<String, TextAttributesKey>? = null

    override fun getAttributeDescriptors(): Array<AttributesDescriptor> = AMOS_ATTRIBUTE_DESCRIPTORS

    override fun getColorDescriptors(): Array<ColorDescriptor> = ColorDescriptor.EMPTY_ARRAY

}

private val AMOS_ATTRIBUTE_DESCRIPTORS = arrayOf(
    AttributesDescriptor("Keyword", AmosSyntaxHighlighter.KEYWORD),
    AttributesDescriptor("Identifier", AmosSyntaxHighlighter.IDENTIFIER),
    AttributesDescriptor("String variable", AmosSyntaxHighlighter.STRING_VARIABLE),
    AttributesDescriptor("Float variable", AmosSyntaxHighlighter.FLOAT_VARIABLE),
    AttributesDescriptor("Number", AmosSyntaxHighlighter.NUMBER),
    AttributesDescriptor("String", AmosSyntaxHighlighter.STRING),
    AttributesDescriptor("Comment", AmosSyntaxHighlighter.COMMENT),
    AttributesDescriptor("Operator", AmosSyntaxHighlighter.OPERATOR),
    AttributesDescriptor("Comma", AmosSyntaxHighlighter.COMMA),
    AttributesDescriptor("Parentheses", AmosSyntaxHighlighter.PAREN),
    AttributesDescriptor("Bad character", AmosSyntaxHighlighter.BAD_CHAR)
)


