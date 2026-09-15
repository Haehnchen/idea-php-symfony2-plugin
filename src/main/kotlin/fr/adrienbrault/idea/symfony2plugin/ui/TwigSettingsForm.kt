package fr.adrienbrault.idea.symfony2plugin.ui

import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.table.TableView
import com.intellij.util.concurrency.AppExecutorUtil
import com.intellij.util.ui.ColumnInfo
import com.intellij.util.ui.ElementProducer
import com.intellij.util.ui.ListTableModel
import fr.adrienbrault.idea.symfony2plugin.Settings
import fr.adrienbrault.idea.symfony2plugin.templating.path.TwigNamespaceSetting
import fr.adrienbrault.idea.symfony2plugin.templating.path.TwigPath
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.util.concurrent.atomic.AtomicInteger
import javax.swing.JButton
import javax.swing.JCheckBox
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTable

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
class TwigSettingsForm(private val project: Project) : Configurable {
    private lateinit var panel1: JPanel
    private lateinit var chkTwigBundleNamespaceSupport: JCheckBox
    private lateinit var tableView: TableView<TwigPath>
    private var changed = false
    private lateinit var modelList: ListTableModel<TwigPath>
    private val reloadRequestId = AtomicInteger()

    @Volatile
    private var disposed = false

    private fun attachItems() {
        reloadTwigPaths(true, false)
    }

    private fun reloadTwigPaths(includeSettings: Boolean, forceEnableAll: Boolean) {
        if (project.isDefault) {
            return
        }

        resetList()

        if (DumbService.getInstance(project).isDumb) {
            tableView.emptyText.text = "Loading after indexing completes..."
        } else {
            tableView.emptyText.text = "Loading Twig namespaces..."
        }

        val requestId = reloadRequestId.incrementAndGet()

        ReadAction
            .nonBlocking<List<TwigPath>> {
                val sortableLookupItems = ArrayList(TwigUtil.getTwigNamespaces(project, includeSettings))
                sortableLookupItems.sortWith(TwigUtil.TwigPathNamespaceComparator())
                sortableLookupItems
            }
            .inSmartMode(project)
            .expireWhen { disposed || project.isDisposed || requestId != reloadRequestId.get() }
            .finishOnUiThread(ModalityState.any()) { applyTwigPaths(requestId, it, forceEnableAll) }
            .submit(AppExecutorUtil.getAppExecutorService())
    }

    private fun applyTwigPaths(requestId: Int, twigPaths: List<TwigPath>, forceEnableAll: Boolean) {
        if (disposed || project.isDisposed || requestId != reloadRequestId.get()) {
            return
        }

        resetList()

        twigPaths.forEach {
            modelList.addRow(if (forceEnableAll) TwigPath.createClone(it, true) else TwigPath.createClone(it))
        }

        if (twigPaths.isEmpty()) {
            tableView.emptyText.text = "No Twig namespaces found."
        }
    }

    override fun getDisplayName(): String = "Twig"

    override fun getHelpTopic(): String? = null

    override fun createComponent(): JPanel {
        disposed = false
        panel1 = JPanel(BorderLayout())
        val panelTableView = JPanel(BorderLayout())
        panel1.add(panelTableView, BorderLayout.CENTER)

        chkTwigBundleNamespaceSupport = JCheckBox("Support Bundle Namespaces")
        chkTwigBundleNamespaceSupport.isSelected = true
        chkTwigBundleNamespaceSupport.toolTipText = "Example: Foobar:Bar:Foo.html.twig (for older Symfony Versions)"
        val buttonJsonExample = JButton("JSON Example")
        val resetToDefault = JButton("Reset To Default")

        val northPanel = JPanel(FlowLayout(FlowLayout.LEFT))
        northPanel.add(JLabel("Manage Twig Namespaces"))
        northPanel.add(chkTwigBundleNamespaceSupport)
        northPanel.add(buttonJsonExample)
        northPanel.add(resetToDefault)
        panel1.add(northPanel, BorderLayout.NORTH)

        tableView = TableView()
        modelList = ListTableModel(
            NamespaceColumn(),
            PathColumn(project),
            TypeColumn(),
            CustomColumn(),
            DisableColumn(),
        )

        attachItems()

        tableView.setModelAndUpdateColumns(modelList)
        modelList.addTableModelListener { changed = true }

        resetToDefault.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                super.mouseClicked(e)
                reloadTwigPaths(false, true)
            }
        })

        val tablePanel = ToolbarDecorator.createDecorator(tableView, object : ElementProducer<TwigPath> {
            override fun createElement(): TwigPath? {
                //IdeFocusManager.getInstance(TwigSettingsForm.this.project).requestFocus(TwigNamespaceDialog.getWindows(), true);
                return null //To change body of implemented methods use File | Settings | File Templates.
            }

            override fun canCreateElement(): Boolean {
                return true //To change body of implemented methods use File | Settings | File Templates.
            }
        })

        tablePanel.setEditAction { openTwigPathDialog(tableView.selectedObject) }
        tablePanel.setAddAction { openTwigPathDialog(null) }

        tablePanel.setEditActionUpdater {
            tableView.selectedObject?.isCustomPath == true
        }

        tablePanel.setRemoveActionUpdater {
            tableView.selectedObject?.isCustomPath == true
        }

        tablePanel.disableUpAction()
        tablePanel.disableDownAction()

        panelTableView.add(tablePanel.createPanel())

        buttonJsonExample.addActionListener { TwigJsonExampleDialog.open(panel1) }

        return panel1
    }

    override fun isModified(): Boolean {
        return changed || settings.twigBundleNamespaceSupport != chkTwigBundleNamespaceSupport.isSelected
    }

    override fun apply() {
        val twigPaths = ArrayList<TwigNamespaceSetting>()

        for (twigPath in tableView.listTableModel.items) {
            // Skip absolute paths - only relative paths are supported
            if (twigPath.isCustomPath && FileUtil.isAbsolute(twigPath.path)) {
                continue
            }

            val relativePath = twigPath.getRelativePath(project)

            // only custom and disabled path need to save
            if ((!twigPath.isEnabled && relativePath != null) || twigPath.isCustomPath) {
                twigPaths.add(
                    TwigNamespaceSetting(
                        twigPath.namespace,
                        relativePath,
                        twigPath.isEnabled,
                        twigPath.namespaceType,
                        twigPath.isCustomPath,
                    ),
                )
            }
        }

        settings.twigBundleNamespaceSupport = chkTwigBundleNamespaceSupport.isSelected
        settings.twigNamespaces = twigPaths
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
        attachItems()
        updateUIFromSettings()
        changed = false
    }

    private fun updateUIFromSettings() {
        chkTwigBundleNamespaceSupport.isSelected = settings.twigBundleNamespaceSupport
    }

    override fun disposeUIResources() {
        disposed = true
        reloadRequestId.incrementAndGet()
        resetList()
    }

    private class NamespaceColumn : ColumnInfo<TwigPath, String>("Namespace") {
        override fun valueOf(item: TwigPath): String? = item.namespace
    }

    private class PathColumn(private val project: Project) : ColumnInfo<TwigPath, String>("Path") {
        override fun valueOf(item: TwigPath): String? = item.getRelativePath(project)
    }

    private class CustomColumn : ColumnInfo<TwigPath, String>("Parser") {
        override fun valueOf(item: TwigPath): String? = if (item.isCustomPath) "Custom" else "Internal"
    }

    private class TypeColumn : ColumnInfo<TwigPath, String>("Type") {
        override fun valueOf(item: TwigPath): String? = item.namespaceType.toString()
    }

    private abstract class BooleanColumn(name: String) : ColumnInfo<TwigPath, Boolean>(name) {
        override fun isCellEditable(item: TwigPath): Boolean = true

        override fun getColumnClass(): Class<*> = Boolean::class.javaObjectType
    }

    private inner class DisableColumn : BooleanColumn("on") {
        override fun valueOf(item: TwigPath): Boolean = item.isEnabled

        override fun setValue(item: TwigPath, value: Boolean) {
            val index = tableView.listTableModel.items.indexOf(item)
            if (index >= 0) {
                val newTwigPath = TwigPath.createClone(item, value)
                tableView.listTableModel.removeRow(index)
                tableView.listTableModel.insertRow(index, newTwigPath)
            }
        }

        override fun getWidth(table: JTable): Int = 50
    }

    private fun openTwigPathDialog(twigPath: TwigPath?) {
        val dialog = if (twigPath == null) {
            TwigNamespaceDialog(project, tableView)
        } else {
            TwigNamespaceDialog(project, tableView, twigPath)
        }

        val dim = Dimension()
        dim.setSize(500, 190)
        dialog.title = "Twig Namespace"
        dialog.minimumSize = dim
        dialog.pack()
        dialog.setLocationRelativeTo(panel1)

        dialog.isVisible = true
    }
}
