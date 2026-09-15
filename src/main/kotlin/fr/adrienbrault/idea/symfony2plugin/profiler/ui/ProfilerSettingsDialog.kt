package fr.adrienbrault.idea.symfony2plugin.profiler.ui

import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.TextBrowseFolderListener
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.ui.JBColor
import fr.adrienbrault.idea.symfony2plugin.Settings
import fr.adrienbrault.idea.symfony2plugin.util.ProjectUtil
import java.awt.Component
import java.awt.Font
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import java.awt.event.ActionEvent
import javax.swing.BorderFactory
import javax.swing.Box
import javax.swing.ButtonGroup
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JRadioButton
import javax.swing.JTextField
import javax.swing.UIManager

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
class ProfilerSettingsDialog(private val project: Project) : Configurable {
    private lateinit var textHttpProfilerUrl: JTextField
    private lateinit var radioDefaultProfiler: JRadioButton
    private lateinit var radioLocalProfiler: JRadioButton
    private lateinit var radioHttpProfiler: JRadioButton
    private lateinit var mainPanel: JPanel
    private lateinit var textLocalProfilerUrl: JTextField
    private lateinit var textLocalProfilerCsvPath: TextFieldWithBrowseButton
    private lateinit var localComponents: Array<Component>
    private lateinit var httpComponents: Array<Component>

    override fun getDisplayName(): String = "Profiler"

    override fun getHelpTopic(): String? = null

    override fun createComponent(): JPanel {
        textHttpProfilerUrl = JTextField(20)
        textLocalProfilerUrl = JTextField(20)
        textLocalProfilerCsvPath = TextFieldWithBrowseButton()
        radioDefaultProfiler = JRadioButton("Default Profiler")
        radioLocalProfiler = JRadioButton("Local Profiler")
        radioHttpProfiler = JRadioButton("HTTP Profiler")

        val group = ButtonGroup()
        group.add(radioDefaultProfiler)
        group.add(radioLocalProfiler)
        group.add(radioHttpProfiler)

        textLocalProfilerCsvPath.addBrowseFolderListener(
            createBrowseFolderListener(
                textLocalProfilerCsvPath.textField,
                FileChooserDescriptorFactory.createSingleFolderDescriptor(),
            ),
        )

        val labelFont = UIManager.getFont("Label.font")
        val smallFont = labelFont.deriveFont(maxOf(labelFont.size - 2f, 10f))
        val hintColor = UIManager.getColor("Label.disabledForeground") ?: JBColor.GRAY

        // Build rows that will be shown/hidden per radio selection
        val defaultHint = hint("Extract index.csv from container configuration", smallFont, hintColor)

        val localCsvRow = fieldRow("CSV File", textLocalProfilerCsvPath)
        val localCsvHint = hint("Location: var/cache/dev/index.csv", smallFont, hintColor)
        val localUrlRow = fieldRow("Base url", textLocalProfilerUrl)
        val localUrlHint = hint("Overwrite base url: http://127.0.0.1:8080, http://127.0.0.1/app_dev.php", smallFont, hintColor)

        val httpUrlRow = fieldRow("Base url", textHttpProfilerUrl)
        val httpUrlHint = hint("Profiler url without \"_profiler\": http://127.0.0.1:8080", smallFont, hintColor)

        localComponents = arrayOf(localCsvRow, localCsvHint, localUrlRow, localUrlHint)
        httpComponents = arrayOf(httpUrlRow, httpUrlHint)

        radioDefaultProfiler.addActionListener { updateVisibility() }
        radioLocalProfiler.addActionListener { updateVisibility() }
        radioHttpProfiler.addActionListener { updateVisibility() }

        mainPanel = JPanel(GridBagLayout())
        val gbc = GridBagConstraints()
        gbc.gridx = 0
        gbc.weightx = 1.0
        gbc.fill = GridBagConstraints.HORIZONTAL
        gbc.anchor = GridBagConstraints.NORTHWEST
        gbc.weighty = 0.0
        gbc.insets = Insets(1, 0, 1, 0)

        var row = 0
        gbc.gridy = row++
        mainPanel.add(radioDefaultProfiler, gbc)
        gbc.gridy = row++
        mainPanel.add(defaultHint, gbc)
        gbc.insets = Insets(6, 0, 1, 0)
        gbc.gridy = row++
        mainPanel.add(radioLocalProfiler, gbc)
        gbc.insets = Insets(1, 0, 1, 0)
        gbc.gridy = row++
        mainPanel.add(localCsvRow, gbc)
        gbc.gridy = row++
        mainPanel.add(localCsvHint, gbc)
        gbc.gridy = row++
        mainPanel.add(localUrlRow, gbc)
        gbc.gridy = row++
        mainPanel.add(localUrlHint, gbc)
        gbc.insets = Insets(6, 0, 1, 0)
        gbc.gridy = row++
        mainPanel.add(radioHttpProfiler, gbc)
        gbc.insets = Insets(1, 0, 1, 0)
        gbc.gridy = row++
        mainPanel.add(httpUrlRow, gbc)
        gbc.gridy = row++
        mainPanel.add(httpUrlHint, gbc)

        // filler to push everything up
        gbc.gridy = row
        gbc.weighty = 1.0
        gbc.fill = GridBagConstraints.VERTICAL
        mainPanel.add(Box.createVerticalGlue(), gbc)

        // initial visibility matches default radio
        setVisible(localComponents, false)
        setVisible(httpComponents, false)

        return mainPanel
    }

    private fun setVisible(components: Array<Component>, visible: Boolean) {
        components.forEach { it.isVisible = visible }
    }

    private fun fieldRow(labelText: String, field: JComponent): JPanel {
        val row = JPanel(GridBagLayout())
        row.isOpaque = false
        val gbc = GridBagConstraints()
        gbc.anchor = GridBagConstraints.WEST
        gbc.insets = Insets(0, 20, 0, 6)
        gbc.gridx = 0
        gbc.gridy = 0
        gbc.weightx = 0.0
        gbc.fill = GridBagConstraints.NONE
        row.add(JLabel(labelText), gbc)
        gbc.gridx = 1
        gbc.weightx = 1.0
        gbc.fill = GridBagConstraints.HORIZONTAL
        gbc.insets = Insets(0, 0, 0, 0)
        row.add(field, gbc)
        return row
    }

    private fun hint(text: String, font: Font, color: java.awt.Color): JLabel {
        val label = JLabel(text)
        label.font = font
        label.foreground = color
        label.border = BorderFactory.createEmptyBorder(0, 20, 0, 0)
        return label
    }

    override fun isModified(): Boolean {
        val settings = Settings.getInstance(project)

        return settings.profilerLocalEnabled != radioLocalProfiler.isSelected ||
            textLocalProfilerUrl.text != settings.profilerLocalUrl ||
            textLocalProfilerCsvPath.text != settings.profilerCsvPath ||
            settings.profilerHttpEnabled != radioHttpProfiler.isSelected ||
            textHttpProfilerUrl.text != settings.profilerHttpUrl
    }

    override fun apply() {
        val settings = Settings.getInstance(project)

        settings.profilerLocalEnabled = radioLocalProfiler.isSelected
        settings.profilerLocalUrl = textLocalProfilerUrl.text
        settings.profilerCsvPath = textLocalProfilerCsvPath.text

        settings.profilerHttpEnabled = radioHttpProfiler.isSelected
        settings.profilerHttpUrl = textHttpProfilerUrl.text
    }

    override fun reset() {
        val settings = Settings.getInstance(project)

        radioLocalProfiler.isSelected = settings.profilerLocalEnabled
        textLocalProfilerUrl.text = settings.profilerLocalUrl
        textLocalProfilerCsvPath.text = settings.profilerCsvPath

        radioHttpProfiler.isSelected = settings.profilerHttpEnabled
        textHttpProfilerUrl.text = settings.profilerHttpUrl

        updateDefaultRadio()
        if (::localComponents.isInitialized) {
            updateVisibility()
        }
    }

    private fun updateDefaultRadio() {
        if (!radioLocalProfiler.isSelected && !radioHttpProfiler.isSelected) {
            radioDefaultProfiler.isSelected = true
        }
    }

    private fun updateVisibility() {
        setVisible(localComponents, radioLocalProfiler.isSelected)
        setVisible(httpComponents, radioHttpProfiler.isSelected)
        if (::mainPanel.isInitialized) {
            mainPanel.revalidate()
        }
    }

    private fun createBrowseFolderListener(
        textField: JTextField,
        fileChooserDescriptor: FileChooserDescriptor,
    ): TextBrowseFolderListener {
        return object : TextBrowseFolderListener(fileChooserDescriptor) {
            override fun actionPerformed(e: ActionEvent) {
                val projectDirectory = ProjectUtil.getProjectDir(this@ProfilerSettingsDialog.project)

                val text = textField.text
                val toSelect = VfsUtil.findRelativeFile(text, projectDirectory) ?: projectDirectory

                val selectedFile = FileChooser.chooseFile(
                    FileChooserDescriptorFactory.createSingleFileDescriptor("csv"),
                    this@ProfilerSettingsDialog.project,
                    toSelect,
                ) ?: return

                val path = VfsUtil.getRelativePath(selectedFile, projectDirectory, '/') ?: selectedFile.path
                textField.text = path
            }
        }
    }
}
