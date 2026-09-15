package fr.adrienbrault.idea.symfony2plugin.ui

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.intellij.ui.JBColor
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.table.TableView
import com.intellij.util.ui.ElementProducer
import com.intellij.util.ui.ListTableModel
import com.jetbrains.php.lang.PhpFileType
import fr.adrienbrault.idea.symfony2plugin.Settings
import fr.adrienbrault.idea.symfony2plugin.routing.dict.RoutingFile
import fr.adrienbrault.idea.symfony2plugin.ui.utils.UiSettingsUtil
import fr.adrienbrault.idea.symfony2plugin.ui.utils.dict.UiPathColumnInfo
import fr.adrienbrault.idea.symfony2plugin.util.syncSymfonyVarDirectoryWatcher
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Font
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.BorderFactory
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.UIManager

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
class RoutingSettingsForm(private val project: Project) : Configurable {
    private val panel1 = JPanel(BorderLayout())
    private val buttonReset = JButton("Reset To Default")
    private val tableView = TableView<RoutingFile>()
    private var changed = false
    private val modelList = ListTableModel<RoutingFile>(
        UiPathColumnInfo.PathColumn(),
        UiPathColumnInfo.TypeColumn(project),
    )

    init {
        createUIComponents()
        initList()

        modelList.addTableModelListener { changed = true }
        tableView.setModelAndUpdateColumns(modelList)

        buttonReset.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                super.mouseClicked(e)

                resetList()

                // add default path
                Settings.DEFAULT_ROUTES.forEach {
                    modelList.addRow(RoutingFile(it))
                }
            }
        })
    }

    private fun createUIComponents() {
        val labelFont = UIManager.getFont("Label.font")
        val smallFont: Font = labelFont.deriveFont(maxOf(labelFont.size - 2f, 10f))
        val hintColor: Color = UIManager.getColor("Label.disabledForeground") ?: JBColor.GRAY

        val titleLabel = JLabel("Add PHP Routing")

        val hintLabel = JLabel("If your application does not support guessing the value for compiled route files, you can add custom ones.")
        hintLabel.font = smallFont
        hintLabel.foreground = hintColor

        val hintLabel2 = JLabel("Examples: var/cache/dev/[appDevUrlGenerator.php, UrlGenerator.php, url_generating_routes.php]")
        hintLabel2.font = smallFont
        hintLabel2.foreground = hintColor

        val northPanel = JPanel(BorderLayout(4, 2))
        northPanel.border = BorderFactory.createEmptyBorder(0, 0, 6, 0)
        val titleRow = JPanel(BorderLayout())
        titleRow.add(titleLabel, BorderLayout.CENTER)
        titleRow.add(buttonReset, BorderLayout.EAST)
        val hintPanel = JPanel()
        hintPanel.layout = BoxLayout(hintPanel, BoxLayout.Y_AXIS)
        hintPanel.add(hintLabel)
        hintPanel.add(hintLabel2)
        northPanel.add(titleRow, BorderLayout.NORTH)
        northPanel.add(hintPanel, BorderLayout.CENTER)

        panel1.add(northPanel, BorderLayout.NORTH)
    }

    private fun initList() {
        settings.routingFiles?.takeIf { it.isNotEmpty() }?.let(modelList::addRows)
    }

    override fun getDisplayName(): String = "Routing"

    override fun getHelpTopic(): String? = null

    override fun createComponent(): JPanel {
        val tablePanel = ToolbarDecorator.createDecorator(tableView, object : ElementProducer<RoutingFile> {
            override fun createElement(): RoutingFile? = null

            override fun canCreateElement(): Boolean = true
        })

        tablePanel.setEditAction {
            val routingFile = tableView.selectedObject ?: return@setEditAction
            val uri = UiSettingsUtil.getPathDialog(project, PhpFileType.INSTANCE) ?: return@setEditAction

            routingFile.path = uri
            changed = true
        }

        tablePanel.setAddAction {
            val uri = UiSettingsUtil.getPathDialog(project, PhpFileType.INSTANCE) ?: return@setAddAction

            tableView.listTableModel.addRow(RoutingFile(uri))
            changed = true
        }

        panel1.add(tablePanel.createPanel())
        return panel1
    }

    override fun isModified(): Boolean = changed

    override fun apply() {
        settings.routingFiles = ArrayList(tableView.listTableModel.items.map { RoutingFile(it.path) })
        changed = false

        syncSymfonyVarDirectoryWatcher(project)
    }

    private val settings: Settings
        get() = Settings.getInstance(project)

    override fun reset() {
        resetList()
        initList()
        changed = false
    }

    private fun resetList() {
        // clear list, easier?
        while (modelList.rowCount > 0) {
            modelList.removeRow(0)
        }
    }
}
