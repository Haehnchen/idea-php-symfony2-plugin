package fr.adrienbrault.idea.symfony2plugin.ui

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.table.TableView
import com.intellij.util.ui.ColumnInfo
import com.intellij.util.ui.ElementProducer
import com.intellij.util.ui.ListTableModel
import fr.adrienbrault.idea.symfony2plugin.Settings
import fr.adrienbrault.idea.symfony2plugin.assistant.signature.MethodSignatureSetting
import fr.adrienbrault.idea.symfony2plugin.util.IdeHelper
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JButton
import javax.swing.JCheckBox
import javax.swing.JPanel

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
class MethodSignatureTypeSettingsForm(private val project: Project) : Configurable {
    private val panel1 = JPanel(BorderLayout())
    private val panelConfigTableView = JPanel(BorderLayout())
    private val enableCustomSignatureTypesCheckBox = JCheckBox("Enable Custom Signature Types")
    private val buttonHelp = JButton("Help")
    private val tableView = TableView<MethodSignatureSetting>()
    private var changed = false
    private val modelList = ListTableModel<MethodSignatureSetting>(
        CallToColumn(),
        MethodColumn(),
        IndexColumn(),
        ProviderColumn(),
    )

    init {
        createUIComponents()
        attachItems()

        tableView.setModelAndUpdateColumns(modelList)
        tableView.model.addTableModelListener { changed = true }

        buttonHelp.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                super.mouseClicked(e)
                IdeHelper.openUrl("https://www.jetbrains.com/help/phpstorm/symfony-creating-helper-functions.html#signature-types")
            }
        })

        enableCustomSignatureTypesCheckBox.isSelected = settings.objectSignatureTypeProvider
        enableCustomSignatureTypesCheckBox.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                super.mouseClicked(e)
                changed = true
            }
        })
    }

    private fun createUIComponents() {
        panel1.add(panelConfigTableView, BorderLayout.CENTER)

        val northPanel = JPanel(BorderLayout())
        northPanel.add(enableCustomSignatureTypesCheckBox, BorderLayout.CENTER)
        northPanel.add(buttonHelp, BorderLayout.EAST)
        panel1.add(northPanel, BorderLayout.NORTH)
    }

    private fun attachItems() {
        settings.methodSignatureSettings?.forEach(modelList::addRow)
    }

    override fun getDisplayName(): String? = null

    override fun getHelpTopic(): String? = null

    override fun createComponent(): JPanel {
        val tablePanel = ToolbarDecorator.createDecorator(tableView, object : ElementProducer<MethodSignatureSetting> {
            override fun createElement(): MethodSignatureSetting? {
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
        settings.methodSignatureSettings = ArrayList(tableView.listTableModel.items)
        settings.objectSignatureTypeProvider = enableCustomSignatureTypesCheckBox.isSelected

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

    private class CallToColumn : ColumnInfo<MethodSignatureSetting, String>("CallTo") {
        override fun valueOf(item: MethodSignatureSetting): String? = item.callTo
    }

    private class MethodColumn : ColumnInfo<MethodSignatureSetting, String>("Method") {
        override fun valueOf(item: MethodSignatureSetting): String? = item.methodName
    }

    private class IndexColumn : ColumnInfo<MethodSignatureSetting, Int>("Index") {
        override fun valueOf(item: MethodSignatureSetting): Int? = item.indexParameter
    }

    private class ProviderColumn : ColumnInfo<MethodSignatureSetting, String>("Provider") {
        override fun valueOf(item: MethodSignatureSetting): String? = item.referenceProviderName
    }

    private fun openTwigPathDialog(methodSignatureSetting: MethodSignatureSetting?) {
        val dialog = if (methodSignatureSetting == null) {
            MethodSignatureTypeDialog(project, tableView)
        } else {
            MethodSignatureTypeDialog(project, tableView, methodSignatureSetting)
        }

        val dim = Dimension()
        dim.setSize(500, 190)
        dialog.title = "MethodSignatureSetting"
        dialog.minimumSize = dim
        dialog.pack()
        dialog.setLocationRelativeTo(panel1)

        dialog.isVisible = true
    }
}
