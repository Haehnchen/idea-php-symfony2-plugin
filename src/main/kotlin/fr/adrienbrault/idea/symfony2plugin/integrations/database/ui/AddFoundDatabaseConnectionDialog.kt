package fr.adrienbrault.idea.symfony2plugin.integrations.database.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import fr.adrienbrault.idea.symfony2plugin.integrations.database.DatabaseConnectionConfig
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Dimension
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JComponent
import javax.swing.JPanel

/**
 * Dialog for selecting which discovered Symfony database connections to add.
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
class AddFoundDatabaseConnectionDialog(
    project: Project,
    private val connections: List<DatabaseConnectionConfig>,
) : DialogWrapper(project) {

    private val checkboxes = LinkedHashMap<DatabaseConnectionConfig, JBCheckBox>()

    init {
        title = "Add Symfony Database Connections"
        setOKButtonText("Add Connection(s)")
        init()
    }

    override fun createCenterPanel(): JComponent {
        val panel = JPanel(BorderLayout())
        panel.border = JBUI.Borders.empty(10)

        val header = JBLabel("Select database connections to add from Symfony configuration:")
        header.border = JBUI.Borders.emptyBottom(8)
        panel.add(header, BorderLayout.NORTH)

        val connectionsPanel = JPanel()
        connectionsPanel.layout = BoxLayout(connectionsPanel, BoxLayout.Y_AXIS)
        connectionsPanel.border = JBUI.Borders.empty(4)

        for (config in connections) {
            val checkBox = JBCheckBox(formatConnectionLabel(config), true)
            checkBox.alignmentX = Component.LEFT_ALIGNMENT
            checkboxes[config] = checkBox
            connectionsPanel.add(checkBox)
            connectionsPanel.add(Box.createVerticalStrut(2))
        }

        val scrollPane = JBScrollPane(connectionsPanel)
        scrollPane.preferredSize = Dimension(520, minOf(connections.size * 36 + 20, 240))
        panel.add(scrollPane, BorderLayout.CENTER)

        return panel
    }

    private fun formatConnectionLabel(config: DatabaseConnectionConfig): String {
        return buildString {
            append(config.name).append(" \u2014 ").append(config.driver).append("://")

            if (config.host != null) {
                append(config.host)
                if (config.port != null) append(":").append(config.port)
            }

            if (config.database != null) append("/").append(config.database)
            if (config.username != null) append(" (user: ").append(config.username).append(")")
        }
    }

    fun getSelectedConnections(): List<DatabaseConnectionConfig> =
        checkboxes.entries.filter { it.value.isSelected }.map { it.key }
}
