package fr.adrienbrault.idea.symfony2plugin.ui

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.table.TableView
import com.intellij.util.ui.ColumnInfo
import com.intellij.util.ui.ElementProducer
import com.intellij.util.ui.ListTableModel
import fr.adrienbrault.idea.symfony2plugin.Settings
import fr.adrienbrault.idea.symfony2plugin.assistant.reference.AssistantReferenceUtil
import fr.adrienbrault.idea.symfony2plugin.assistant.reference.MethodParameterSetting
import fr.adrienbrault.idea.symfony2plugin.util.IdeHelper
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JPanel

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
class MethodParameterReferenceSettingsForm(private val project: Project) : Configurable {
    private val panel1 = JPanel(BorderLayout())
    private val panelConfigTableView = JPanel(BorderLayout())
    private val buttonHelp = JButton("Help")
    private val tableView = TableView<MethodParameterSetting>()
    private var changed = false
    private val modelList = ListTableModel<MethodParameterSetting>(
        CallToColumn(),
        MethodColumn(),
        IndexColumn(),
        ProviderColumn(),
        ContributorColumn(),
        ContributorDataColumn(),
    )

    init {
        createUIComponents()
        attachItems()

        tableView.setModelAndUpdateColumns(modelList)
        tableView.model.addTableModelListener { changed = true }

        buttonHelp.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                super.mouseClicked(e)
                IdeHelper.openUrl("https://www.jetbrains.com/help/phpstorm/symfony-creating-helper-functions.html#method-parameter")
            }
        })
    }

    private fun createUIComponents() {
        panel1.add(panelConfigTableView, BorderLayout.CENTER)

        val northPanel = JPanel(BorderLayout())
        northPanel.add(JLabel("Provides Completion and Goto for method parameter"), BorderLayout.CENTER)
        northPanel.add(buttonHelp, BorderLayout.EAST)
        panel1.add(northPanel, BorderLayout.NORTH)
    }

    private fun attachItems() {
        AssistantReferenceUtil.getMethodsParameterSettings(project).forEach(modelList::addRow)
    }

    override fun getDisplayName(): String? = null

    override fun getHelpTopic(): String? = null

    override fun createComponent(): JPanel {
        val tablePanel = ToolbarDecorator.createDecorator(tableView, object : ElementProducer<MethodParameterSetting> {
            override fun createElement(): MethodParameterSetting? {
                //IdeFocusManager.getInstance(TwigSettingsForm.this.project).requestFocus(TwigNamespaceDialog.getWindows(), true);
                return null
            }

            override fun canCreateElement(): Boolean {
                return true
            }
        })

        tablePanel.setEditAction { openTwigPathDialog(tableView.selectedObject) }
        tablePanel.setAddAction { openTwigPathDialog(null) }

        tablePanel.disableUpAction()
        tablePanel.disableDownAction()

        panelConfigTableView.add(tablePanel.createPanel())

        return panel1
    }

    override fun isModified(): Boolean = changed

    override fun apply() {
        settings.methodParameterSettings = ArrayList(tableView.listTableModel.items)
        changed = false
    }

    private val settings: Settings
        get() = Settings.getInstance(project)

    private fun resetList() {
        // clear list, easier?
        while (modelList.rowCount > 0) {
            modelList.removeRow(0)
        }
    }

    override fun reset() {
        resetList()
        attachItems()
        changed = false
    }

    private class CallToColumn : ColumnInfo<MethodParameterSetting, String>("CallTo") {
        override fun valueOf(item: MethodParameterSetting): String? = item.callTo
    }

    private class MethodColumn : ColumnInfo<MethodParameterSetting, String>("Method") {
        override fun valueOf(item: MethodParameterSetting): String? = item.methodName
    }

    private class IndexColumn : ColumnInfo<MethodParameterSetting, Int>("Index") {
        override fun valueOf(item: MethodParameterSetting): Int? = item.indexParameter
    }

    private class ProviderColumn : ColumnInfo<MethodParameterSetting, String>("Provider") {
        override fun valueOf(item: MethodParameterSetting): String? = item.referenceProviderName
    }

    private class ContributorColumn : ColumnInfo<MethodParameterSetting, String>("Contributor") {
        override fun valueOf(item: MethodParameterSetting): String? = item.contributorName
    }

    private class ContributorDataColumn : ColumnInfo<MethodParameterSetting, String>("ContributorData") {
        override fun valueOf(item: MethodParameterSetting): String? = item.contributorData
    }

    private fun openTwigPathDialog(methodParameterSetting: MethodParameterSetting?) {
        val dialog = if (methodParameterSetting == null) {
            MethodParameterDialog(project, tableView)
        } else {
            MethodParameterDialog(project, tableView, methodParameterSetting)
        }

        val dim = Dimension()
        dim.setSize(500, 190)
        dialog.title = "MethodParameterSetting"
        dialog.minimumSize = dim
        dialog.pack()
        dialog.setLocationRelativeTo(panel1)

        dialog.isVisible = true
    }
}
