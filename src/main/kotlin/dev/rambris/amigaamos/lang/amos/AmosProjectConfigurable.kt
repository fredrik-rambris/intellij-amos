package dev.rambris.amigaamos.lang.amos

import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import javax.swing.JComponent

class AmosProjectConfigurable : SearchableConfigurable {
    private val panel: JBPanel<JBPanel<*>> = JBPanel<JBPanel<*>>().apply {
        add(JBLabel("Configure AMOS language settings in child pages."))
    }

    override fun getId(): String = "settings.amos"

    override fun getDisplayName(): String = "AMOS"

    override fun createComponent(): JComponent = panel

    override fun isModified(): Boolean = false

    override fun apply() {
        // Parent group has no editable settings.
    }
}

