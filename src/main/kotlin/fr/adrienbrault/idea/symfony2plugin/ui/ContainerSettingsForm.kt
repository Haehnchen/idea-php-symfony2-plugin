package fr.adrienbrault.idea.symfony2plugin.ui

import com.intellij.ide.highlighter.XmlFileType
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.table.TableView
import com.intellij.util.ui.ElementProducer
import com.intellij.util.ui.ListTableModel
import fr.adrienbrault.idea.symfony2plugin.Settings
import fr.adrienbrault.idea.symfony2plugin.dic.ContainerFile
import fr.adrienbrault.idea.symfony2plugin.dic.container.util.ServiceContainerUtil
import fr.adrienbrault.idea.symfony2plugin.ui.utils.UiSettingsUtil
import fr.adrienbrault.idea.symfony2plugin.ui.utils.dict.UiPathColumnInfo
import fr.adrienbrault.idea.symfony2plugin.util.syncSymfonyVarDirectoryWatcher
import java.awt.BorderLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JPanel

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
class ContainerSettingsForm(private val project: Project) : Configurable {
    private val panel1 = JPanel(BorderLayout())
    private val buttonReset = JButton("Reset To Default")
    private val tableView = TableView<ContainerFile>()
    private var changed = false
    private val modelList = ListTableModel<ContainerFile>(
        UiPathColumnInfo.PathColumn(),
        UiPathColumnInfo.TypeColumn(project),
    )

    init {
        createUIComponents()
        fillContainerList()

        modelList.addTableModelListener { changed = true }
        tableView.setModelAndUpdateColumns(modelList)

        buttonReset.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                super.mouseClicked(e)

                resetContainerList()

                // add default path
                ServiceContainerUtil.getContainerFiles(project).forEach {
                    modelList.addRow(ContainerFile(it))
                }
            }
        })
    }

    private fun createUIComponents() {
        val northPanel = JPanel(BorderLayout())
        northPanel.add(JLabel("Add additional Symfony xml container files"), BorderLayout.CENTER)
        northPanel.add(buttonReset, BorderLayout.EAST)

        panel1.add(northPanel, BorderLayout.NORTH)
    }

    private fun fillContainerList() {
        settings.containerFiles?.takeIf { it.isNotEmpty() }?.let(modelList::addRows)
    }

    override fun getDisplayName(): String = "Container"

    override fun getHelpTopic(): String? = null

    override fun createComponent(): JPanel {
        val tablePanel = ToolbarDecorator.createDecorator(tableView, object : ElementProducer<ContainerFile> {
            override fun createElement(): ContainerFile? = null

            override fun canCreateElement(): Boolean = true
        })

        tablePanel.setEditAction {
            val containerFile = tableView.selectedObject ?: return@setEditAction
            val uri = UiSettingsUtil.getPathDialog(project, XmlFileType.INSTANCE) ?: return@setEditAction

            containerFile.path = uri
            changed = true
        }

        tablePanel.setAddAction {
            val uri = UiSettingsUtil.getPathDialog(project, XmlFileType.INSTANCE) ?: return@setAddAction

            tableView.listTableModel.addRow(ContainerFile(uri))
            changed = true
        }

        panel1.add(tablePanel.createPanel())
        return panel1
    }

    override fun isModified(): Boolean = changed

    override fun apply() {
        settings.containerFiles = ArrayList(tableView.listTableModel.items.map { ContainerFile(it.path) })
        changed = false

        syncSymfonyVarDirectoryWatcher(project)
    }

    private val settings: Settings
        get() = Settings.getInstance(project)

    override fun reset() {
        resetContainerList()
        fillContainerList()
        changed = false
    }

    private fun resetContainerList() {
        // clear list, easier?
        while (modelList.rowCount > 0) {
            modelList.removeRow(0)
        }
    }
}
