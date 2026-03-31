package dev.rambris.amigaamos.lang.amos

import com.intellij.icons.AllIcons
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.table.JBTable
import kotlinx.serialization.json.Json
import java.net.URI
import java.util.Locale
import javax.swing.Icon
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.ListSelectionModel
import javax.swing.table.AbstractTableModel

class AmosDefinitionProjectConfigurable(private val project: Project) : SearchableConfigurable {
    private val json = Json { ignoreUnknownKeys = true }
    private val appSettings: AmosDefinitionApplicationSettings
        get() = ApplicationManager.getApplication().service()

    private val projectSettings: AmosDefinitionProjectSettings
        get() = project.service()

    private val globalModel = DefinitionTableModel(mutableListOf())
    private val projectModel = DefinitionTableModel(mutableListOf())

    private val globalTable = JBTable(globalModel)
    private val projectTable = JBTable(projectModel)

    private val panel: JPanel by lazy {
        JPanel().apply {
            layout = java.awt.GridLayout(2, 1, 0, 8)
            add(createSection("Global definitions", globalTable, globalModel, AmosDefinitionSourceKind.GLOBAL))
            add(createSection("Project definitions", projectTable, projectModel, AmosDefinitionSourceKind.PROJECT))
        }
    }

    override fun getId(): String = "settings.amos.definitions"

    override fun getDisplayName(): String = "AMOS Definitions"

    override fun createComponent(): JComponent = panel

    override fun isModified(): Boolean {
        val globalSources = globalModel.rows.filter { it.sourceKind == AmosDefinitionSourceKind.GLOBAL }.map { it.reference }
        val projectSources = projectModel.rows.filter { it.sourceKind == AmosDefinitionSourceKind.PROJECT }.map { it.reference }
        val globalDisabled = globalModel.rows
            .asSequence()
            .filter { it.canDisable && !it.enabled }
            .mapNotNull { it.extensionId }
            .map { it.lowercase(Locale.ROOT) }
            .toSet()
        val projectDisabled = projectModel.rows
            .asSequence()
            .filter { it.canDisable && !it.enabled }
            .mapNotNull { it.extensionId }
            .map { it.lowercase(Locale.ROOT) }
            .toSet()

        return globalSources != appSettings.additionalSourceReferences() ||
            projectSources != projectSettings.additionalSourceReferences() ||
            globalDisabled != appSettings.disabledExtensionIdsNormalized() ||
            projectDisabled != projectSettings.disabledExtensionIdsNormalized()
    }

    override fun apply() {
        validateSlotConflicts()

        val globalSources = globalModel.rows
            .filter { it.sourceKind == AmosDefinitionSourceKind.GLOBAL }
            .map { it.reference }
        val projectSources = projectModel.rows
            .filter { it.sourceKind == AmosDefinitionSourceKind.PROJECT }
            .map { it.reference }

        val globalDisabled = globalModel.rows
            .asSequence()
            .filter { it.canDisable && !it.enabled }
            .mapNotNull { it.extensionId }
            .map { it.lowercase(Locale.ROOT) }
            .toSet()
        val projectDisabled = projectModel.rows
            .asSequence()
            .filter { it.canDisable && !it.enabled }
            .mapNotNull { it.extensionId }
            .map { it.lowercase(Locale.ROOT) }
            .toSet()

        appSettings.update(globalSources, globalDisabled)
        projectSettings.update(projectSources, projectDisabled)
    }

    override fun reset() {
        val entries = AmosDefinitionRegistry.definitionFileEntries(project)
        globalModel.rows = entries
            .filter {
                it.sourceKind == AmosDefinitionSourceKind.CORE ||
                    it.sourceKind == AmosDefinitionSourceKind.BUNDLED ||
                    it.sourceKind == AmosDefinitionSourceKind.GLOBAL
            }
            .map { it.toRow() }
            .toMutableList()
        projectModel.rows = entries
            .filter { it.sourceKind == AmosDefinitionSourceKind.PROJECT }
            .map { it.toRow() }
            .toMutableList()

        sortRows(globalModel.rows)
        sortRows(projectModel.rows)
        globalModel.fireTableDataChanged()
        projectModel.fireTableDataChanged()
    }

    private fun createSection(
        title: String,
        table: JBTable,
        model: DefinitionTableModel,
        addKind: AmosDefinitionSourceKind
    ): JPanel {
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
        table.columnModel.getColumn(0).minWidth = 64
        table.columnModel.getColumn(0).preferredWidth = 64
        table.columnModel.getColumn(0).maxWidth = 64
        val infoColumn = table.columnModel.getColumn(5)
        infoColumn.minWidth = 28
        infoColumn.preferredWidth = 28
        infoColumn.maxWidth = 28

        table.addMouseListener(object : java.awt.event.MouseAdapter() {
            override fun mouseClicked(e: java.awt.event.MouseEvent) {
                val viewRow = table.rowAtPoint(e.point)
                val viewColumn = table.columnAtPoint(e.point)
                if (viewRow < 0 || viewColumn < 0) {
                    return
                }
                val modelColumn = table.convertColumnIndexToModel(viewColumn)
                if (modelColumn != 5) {
                    return
                }
                val rowIndex = table.convertRowIndexToModel(viewRow)
                val row = model.rows.getOrNull(rowIndex) ?: return
                if (!row.hasInfo()) {
                    return
                }
                Messages.showInfoMessage(project, row.infoText(), "AMOS Extension Info")
            }
        })

        return JPanel().apply {
            layout = java.awt.BorderLayout(4, 4)
            add(JLabel(title), java.awt.BorderLayout.NORTH)
            add(JBScrollPane(table), java.awt.BorderLayout.CENTER)
            add(
                JPanel().apply {
                    layout = java.awt.FlowLayout(java.awt.FlowLayout.LEFT)
                    add(JButton("+").apply {
                        addActionListener {
                            addEntry(model, addKind)
                        }
                    })
                    add(JButton("-").apply {
                        addActionListener {
                            val selected = table.selectedRow
                            if (selected < 0) {
                                return@addActionListener
                            }
                            val rowIndex = table.convertRowIndexToModel(selected)
                            if (rowIndex !in model.rows.indices) {
                                return@addActionListener
                            }
                            val row = model.rows[rowIndex]
                            if (!row.canRemove) {
                                return@addActionListener
                            }
                            model.rows.removeAt(rowIndex)
                            sortRows(model.rows)
                            model.fireTableDataChanged()
                        }
                    })
                },
                java.awt.BorderLayout.SOUTH
            )
        }
    }

    private fun addEntry(model: DefinitionTableModel, sourceKind: AmosDefinitionSourceKind) {
        val value = Messages.showInputDialog(
            project,
            "Enter definition file path or URI",
            "Add AMOS Definition",
            null
        ) ?: return

        val reference = value.trim()
        if (reference.isEmpty()) {
            return
        }

        if (model.rows.any { it.reference.equals(reference, ignoreCase = true) }) {
            return
        }

        val metadata = parseDefinitionMetadata(reference)
        model.rows += DefinitionRow(
            sourceKind = sourceKind,
            reference = reference,
            extensionId = metadata.extensionId,
            extensionName = metadata.extensionName,
            extensionFilename = metadata.extensionFilename,
            extensionVendor = metadata.extensionVendor,
            extensionVersion = metadata.extensionVersion,
            slot = metadata.slot,
            canDisable = metadata.extensionId != null,
            canRemove = true,
            enabled = true,
            parseError = metadata.parseError
        )
        sortRows(model.rows)
        model.fireTableDataChanged()
    }

    private fun sortRows(rows: MutableList<DefinitionRow>) {
        rows.sortWith(compareBy<DefinitionRow>(
            { if (it.sourceKind == AmosDefinitionSourceKind.CORE) 0 else 1 },
            { it.slot ?: Int.MAX_VALUE },
            { (it.extensionName ?: it.extensionId ?: it.reference).uppercase(Locale.ROOT) }
        ))
    }

    private fun validateSlotConflicts() {
        val enabledRows = (globalModel.rows + projectModel.rows)
            .filter { it.enabled && it.slot != null && it.extensionId != null }

        val conflicts = enabledRows
            .groupBy { it.slot }
            .values
            .map { group ->
                group.distinctBy { it.extensionId }
            }
            .filter { it.size > 1 }

        if (conflicts.isNotEmpty()) {
            val conflictText = conflicts.joinToString("\n") { group ->
                val slot = group.first().slot
                val names = group.joinToString(", ") { it.extensionName ?: it.extensionId ?: "unknown" }
                "Slot $slot is used by: $names"
            }
            throw ConfigurationException(
                "Resolve extension slot conflicts before applying:\n$conflictText",
                "AMOS Definitions"
            )
        }
    }

    private fun AmosDefinitionFileEntry.toRow(): DefinitionRow {
        return DefinitionRow(
            sourceKind = sourceKind,
            reference = reference,
            extensionId = extensionId,
            extensionName = extensionName,
            extensionFilename = extensionFilename,
            extensionVendor = extensionVendor,
            extensionVersion = extensionVersion,
            slot = slot,
            canDisable = canDisable,
            canRemove = canRemove,
            enabled = enabled,
            parseError = parseError
        )
    }

    private data class DefinitionMetadata(
        val extensionId: String?,
        val extensionName: String?,
        val extensionFilename: String?,
        val extensionVendor: String?,
        val extensionVersion: String?,
        val slot: Int?,
        val parseError: String?
    )

    private fun parseDefinitionMetadata(reference: String): DefinitionMetadata {
        val source = AmosDefinitionSourceReferences.toSource(reference)
            ?: return DefinitionMetadata(null, null, null, null, null, null, "Invalid source reference")

        val file = runCatching {
            val stream = when {
                source.resourcePath != null && source.classLoader != null -> source.classLoader.getResourceAsStream(source.resourcePath)
                source.uri != null -> runCatching { URI(source.uri).toURL().openStream() }.getOrNull()
                else -> null
            } ?: return DefinitionMetadata(null, null, null, null, null, null, "Unable to read source")

            stream.bufferedReader(Charsets.UTF_8).use {
                json.decodeFromString<AmosDefinitionFile>(it.readText())
            }
        }

        val parsed = file.getOrNull()
        if (parsed == null) {
            return DefinitionMetadata(null, null, null, null, null, null, file.exceptionOrNull()?.message)
        }

        val extensionId = parsed.extension?.extensionId()
        val extensionName = parsed.extension?.name?.trim()?.takeIf(String::isNotEmpty)
            ?: parsed.extension?.id?.trim()?.takeIf(String::isNotEmpty)

        return DefinitionMetadata(
            extensionId = extensionId,
            extensionName = extensionName,
            extensionFilename = parsed.extension?.filename?.trim()?.takeIf(String::isNotEmpty),
            extensionVendor = parsed.extension?.vendor?.trim()?.takeIf(String::isNotEmpty),
            extensionVersion = parsed.extension?.version?.trim()?.takeIf(String::isNotEmpty),
            slot = parsed.extension?.slot,
            parseError = null
        )
    }

    private data class DefinitionRow(
        val sourceKind: AmosDefinitionSourceKind,
        val reference: String,
        val extensionId: String?,
        val extensionName: String?,
        val extensionFilename: String?,
        val extensionVendor: String?,
        val extensionVersion: String?,
        val slot: Int?,
        val canDisable: Boolean,
        val canRemove: Boolean,
        var enabled: Boolean,
        val parseError: String?
    ) {
        fun hasInfo(): Boolean {
            return extensionId != null ||
                extensionName != null ||
                extensionFilename != null ||
                extensionVendor != null ||
                extensionVersion != null ||
                slot != null ||
                parseError != null
        }

        fun infoText(): String {
            val lines = mutableListOf<String>()
            lines += "Reference: $reference"
            lines += "Source: ${sourceKind.name.lowercase(Locale.ROOT)}"
            lines += "Extension: ${extensionName ?: extensionId ?: "(none)"}"
            if (extensionId != null) lines += "ID: $extensionId"
            if (slot != null) lines += "Slot: $slot"
            if (extensionFilename != null) lines += "Filename: $extensionFilename"
            if (extensionVersion != null) lines += "Version: $extensionVersion"
            if (extensionVendor != null) lines += "Vendor: $extensionVendor"
            if (parseError != null) lines += "Parse error: $parseError"
            return lines.joinToString("\n")
        }
    }

    private class DefinitionTableModel(var rows: MutableList<DefinitionRow>) : AbstractTableModel() {
        override fun getRowCount(): Int = rows.size

        override fun getColumnCount(): Int = 6

        override fun getColumnName(column: Int): String {
            return when (column) {
                0 -> "Enabled"
                1 -> "Extension"
                2 -> "Slot"
                3 -> "Definition"
                4 -> "Status"
                else -> ""
            }
        }

        override fun getColumnClass(columnIndex: Int): Class<*> {
            if (columnIndex == 0) {
                return java.lang.Boolean::class.java
            }
            if (columnIndex == 5) {
                return Icon::class.java
            }
            return String::class.java
        }

        override fun isCellEditable(rowIndex: Int, columnIndex: Int): Boolean {
            if (columnIndex != 0) {
                return false
            }
            return rows.getOrNull(rowIndex)?.canDisable == true
        }

        override fun getValueAt(rowIndex: Int, columnIndex: Int): Any {
            val row = rows[rowIndex]
            return when (columnIndex) {
                0 -> row.enabled
                1 -> row.extensionName ?: row.extensionId ?: "core"
                2 -> row.slot?.toString().orEmpty()
                3 -> row.reference
                4 -> row.parseError ?: statusText(row)
                else -> if (row.hasInfo()) AllIcons.General.Information else ""
            }
        }

        override fun setValueAt(aValue: Any?, rowIndex: Int, columnIndex: Int) {
            if (columnIndex != 0 || rowIndex !in rows.indices) {
                return
            }
            rows[rowIndex].enabled = (aValue as? Boolean) == true
            fireTableCellUpdated(rowIndex, columnIndex)
        }

        companion object {
            fun statusText(row: DefinitionRow): String {
            return when {
                row.sourceKind == AmosDefinitionSourceKind.CORE -> "core"
                row.sourceKind == AmosDefinitionSourceKind.BUNDLED -> "bundled"
                row.sourceKind == AmosDefinitionSourceKind.GLOBAL -> "global"
                else -> "project"
            }
        }
        }
    }
}








