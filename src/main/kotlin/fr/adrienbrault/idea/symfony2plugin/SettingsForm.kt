package fr.adrienbrault.idea.symfony2plugin

import com.intellij.ide.DataManager
import com.intellij.ide.actions.ShowSettingsUtilImpl
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.TextBrowseFolderListener
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.ui.JBColor
import com.intellij.ui.TitledSeparator
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import fr.adrienbrault.idea.symfony2plugin.mcp.McpApplicationSettings
import fr.adrienbrault.idea.symfony2plugin.stubs.util.IndexUtil
import fr.adrienbrault.idea.symfony2plugin.util.IdeHelper
import fr.adrienbrault.idea.symfony2plugin.util.ProjectUtil
import fr.adrienbrault.idea.symfony2plugin.util.syncSymfonyVarDirectoryWatcher
import org.apache.commons.lang3.StringUtils
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Cursor
import java.awt.FlowLayout
import java.awt.Font
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.event.ActionEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import javax.swing.Box
import javax.swing.JButton
import javax.swing.JCheckBox
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JOptionPane
import javax.swing.JPanel
import javax.swing.JTextField
import javax.swing.UIManager

abstract class SettingsFormShowSupport protected constructor() {
    companion object {
        @JvmStatic
        fun show(project: Project) {
            ShowSettingsUtilImpl.showSettingsDialog(project, "Symfony2.SettingsForm", null)
        }
    }
}

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
class SettingsForm(private val project: Project) : SettingsFormShowSupport(), Configurable {
    private lateinit var panel1: JPanel

    private lateinit var pluginEnabled: JCheckBox

    private lateinit var directoryToWebReset: JButton
    private lateinit var directoryToWeb: TextFieldWithBrowseButton

    private lateinit var directoryToAppReset: JButton
    private lateinit var directoryToApp: TextFieldWithBrowseButton
    private lateinit var pathToTranslationRootTextFieldReset: JButton
    private lateinit var pathToTranslationRootTextField: TextFieldWithBrowseButton
    private lateinit var buttonHelp: JButton

    private lateinit var codeFoldingPhpRoute: JCheckBox
    private lateinit var codeFoldingPhpModel: JCheckBox
    private lateinit var codeFoldingTwigRoute: JCheckBox
    private lateinit var codeFoldingPhpTemplate: JCheckBox
    private lateinit var codeFoldingTwigTemplate: JCheckBox
    private lateinit var codeFoldingTwigConstant: JCheckBox

    private lateinit var buttonReindex: JButton
    private lateinit var featureTwigIcon: JCheckBox
    private lateinit var featurePhpClassIcon: JCheckBox
    private lateinit var buttonBuyLicense: JButton
    private lateinit var buttonAutoConfigure: JButton
    private lateinit var featureTypeProvider: JCheckBox
    private lateinit var featurePropertyInjection: JCheckBox
    private lateinit var showYamlSchemaNotification: JCheckBox
    private lateinit var mcpEnabled: JCheckBox

    init {
        createUIComponents()
        buttonHelp.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                super.mouseClicked(e)
                IdeHelper.openUrl("https://espend.de/phpstorm/plugin/symfony")
            }
        })

        buttonBuyLicense.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                super.mouseClicked(e)
                IdeHelper.openUrl("https://plugins.jetbrains.com/plugin/7219-symfony-support/pricing")
            }
        })
    }

    override fun getDisplayName(): String = "Symfony Plugin"

    override fun getHelpTopic(): String? = null

    private fun createUIComponents() {
        pluginEnabled = JCheckBox("Enable Plugin for this Project")
        buttonHelp = JButton("Open Help Page")
        buttonAutoConfigure = JButton("Autoconfigure")
        buttonBuyLicense = JButton("Buy License")
        buttonReindex = JButton("Clear Index")
        mcpEnabled = JCheckBox("Enable MCP Tools")

        pathToTranslationRootTextField = TextFieldWithBrowseButton()
        pathToTranslationRootTextFieldReset = JButton("Default")
        directoryToWeb = TextFieldWithBrowseButton()
        directoryToWebReset = JButton("Default")
        directoryToApp = TextFieldWithBrowseButton()
        directoryToAppReset = JButton("Default")

        codeFoldingPhpRoute = JCheckBox("Route (PHP)")
        codeFoldingPhpModel = JCheckBox("Repository Entity (PHP)")
        codeFoldingPhpTemplate = JCheckBox("Template (PHP)")
        codeFoldingTwigRoute = JCheckBox("Route (Twig)")
        codeFoldingTwigTemplate = JCheckBox("Template (Twig)")
        codeFoldingTwigConstant = JCheckBox("Constant (Twig)")
        featureTwigIcon = JCheckBox("Twig Icon Decoration")
        featurePhpClassIcon = JCheckBox("PHP Class Icon Decoration")
        showYamlSchemaNotification = JCheckBox("Show YAML Schema Notification")
        featurePropertyInjection = JCheckBox("Service Property Injection")
        featureTypeProvider = JCheckBox("Type Provider")
    }

    override fun createComponent(): JComponent {
        pathToTranslationRootTextField.addBrowseFolderListener(
            createBrowseFolderListener(
                pathToTranslationRootTextField.textField,
                FileChooserDescriptorFactory.createSingleFolderDescriptor(),
            ),
        )
        pathToTranslationRootTextFieldReset.addMouseListener(
            createResetPathButtonMouseListener(pathToTranslationRootTextField.textField, Settings.DEFAULT_TRANSLATION_PATH),
        )

        directoryToApp.addBrowseFolderListener(
            createBrowseFolderListener(directoryToApp.textField, FileChooserDescriptorFactory.createSingleFolderDescriptor()),
        )
        directoryToAppReset.addMouseListener(
            createResetPathButtonMouseListener(directoryToApp.textField, Settings.DEFAULT_APP_DIRECTORY),
        )

        directoryToWeb.addBrowseFolderListener(
            createBrowseFolderListener(directoryToWeb.textField, FileChooserDescriptorFactory.createSingleFolderDescriptor()),
        )
        directoryToWebReset.addMouseListener(
            createResetPathButtonMouseListener(directoryToWeb.textField, Settings.DEFAULT_WEB_DIRECTORY),
        )

        buttonReindex.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                IndexUtil.forceReindex()
                super.mouseClicked(e)
            }
        })

        buttonAutoConfigure.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                super.mouseClicked(e)
                val list = IdeHelper.enablePluginAndConfigure(project).map { "- $it" }
                settings.pluginEnabled = true
                updateUIFromSettings()
                JOptionPane.showMessageDialog(
                    panel1,
                    "Plugin activated and configured with:\n" + StringUtils.join(list, "\n"),
                    "Symfony Plugin",
                    JOptionPane.PLAIN_MESSAGE,
                )
            }
        })

        val content = JPanel(GridBagLayout())
        val gbc = GridBagConstraints()
        gbc.gridx = 0
        gbc.fill = GridBagConstraints.HORIZONTAL
        gbc.weightx = 1.0
        gbc.anchor = GridBagConstraints.NORTHWEST
        gbc.insets = JBUI.emptyInsets()
        var row = 0

        // ── General ──────────────────────────────────────────────────────────
        gbc.gridy = row++
        content.add(TitledSeparator("General"), gbc)

        gbc.gridy = row++
        gbc.insets = JBUI.insets(4, 8, 0, 0)
        content.add(
            createCheckWithHint(
                pluginEnabled,
                "Activates all Symfony-specific features for this project. Requires an IDE restart.",
            ),
            gbc,
        )

        gbc.gridy = row++
        gbc.insets = JBUI.insets(8, 8, 4, 0)
        val actionBar = JPanel(FlowLayout(FlowLayout.LEFT, 4, 0))
        actionBar.add(buttonAutoConfigure)
        actionBar.add(buttonHelp)
        actionBar.add(buttonReindex)
        actionBar.add(buttonBuyLicense)
        content.add(actionBar, gbc)

        gbc.gridy = row++
        gbc.insets = JBUI.insets(4, 8, 0, 0)
        content.add(createMcpCheckPanel(), gbc)

        // ── Paths ─────────────────────────────────────────────────────────────
        gbc.gridy = row++
        gbc.insets = JBUI.insetsTop(12)
        content.add(TitledSeparator("Paths"), gbc)

        gbc.gridy = row++
        gbc.insets = JBUI.insets(6, 8, 0, 0)
        content.add(
            createFieldWithHint(
                "Translation Root Path",
                pathToTranslationRootTextField,
                pathToTranslationRootTextFieldReset,
                "Root directory for translation files (e.g. translations/)",
            ),
            gbc,
        )

        gbc.gridy = row++
        gbc.insets = JBUI.insets(8, 8, 0, 0)
        content.add(
            createFieldWithHint(
                "Web Directory",
                directoryToWeb,
                directoryToWebReset,
                "Public web root directory, used to resolve asset paths (e.g. public/ or web/)",
            ),
            gbc,
        )

        // ── Features ──────────────────────────────────────────────────────────
        gbc.gridy = row++
        gbc.insets = JBUI.insetsTop(12)
        content.add(TitledSeparator("Features"), gbc)

        val featureItems = listOf(
            featureTwigIcon to "Overlay Twig file icons with a badge indicating usage: extends, include, controller, ...",
            featurePhpClassIcon to "Overlay Symfony-related PHP class file icons with badges",
            showYamlSchemaNotification to "Show the banner suggesting to add a YAML schema hint for out-of-box Symfony service completion",
            featurePropertyInjection to "Autocomplete properties in service and auto injection class via constructor",
            featureTypeProvider to "Resolve return types for container calls like ContainerInterface::get() and EntityManager::find()",
        )
        for ((checkBox, hint) in featureItems) {
            gbc.gridy = row++
            gbc.insets = JBUI.insets(4, 8, 0, 0)
            content.add(createCheckWithHint(checkBox, hint), gbc)
        }

        // ── Code Folding ──────────────────────────────────────────────────────
        gbc.gridy = row++
        gbc.insets = JBUI.insetsTop(12)
        content.add(TitledSeparator("Code Folding"), gbc)

        val foldingItems = listOf(
            codeFoldingPhpRoute to "Route name strings in PHP files to a short label",
            codeFoldingPhpModel to "Doctrine entity repository class names in PHP",
            codeFoldingPhpTemplate to "Template paths in PHP render() calls",
            codeFoldingTwigRoute to "Route name strings in Twig templates to url",
            codeFoldingTwigTemplate to "Template paths in Twig include and extends tags",
            codeFoldingTwigConstant to "Fold PHP constant / enums references in Twig",
        )
        for ((checkBox, hint) in foldingItems) {
            gbc.gridy = row++
            gbc.insets = JBUI.insets(4, 8, 0, 0)
            content.add(createCheckWithHint(checkBox, hint), gbc)
        }

        // ── Legacy ────────────────────────────────────────────────────────────
        gbc.gridy = row++
        gbc.insets = JBUI.insetsTop(12)
        content.add(TitledSeparator("Legacy"), gbc)

        gbc.gridy = row++
        gbc.insets = JBUI.insets(6, 8, 0, 0)
        content.add(
            createFieldWithHint(
                "App Directory",
                directoryToApp,
                directoryToAppReset,
                "Legacy Symfony 2/3 app/ directory containing config, cache, and logs",
            ),
            gbc,
        )

        // Filler
        gbc.gridy = row
        gbc.fill = GridBagConstraints.BOTH
        gbc.weighty = 1.0
        gbc.insets = JBUI.emptyInsets()
        content.add(Box.createVerticalGlue(), gbc)

        panel1 = JPanel(BorderLayout())
        panel1.add(content, BorderLayout.CENTER)

        return panel1
    }

    private fun createMcpCheckPanel(): JPanel {
        val panel = JPanel(GridBagLayout())
        panel.isOpaque = false
        val c = GridBagConstraints()

        c.gridx = 0
        c.gridy = 0
        c.gridwidth = 2
        c.anchor = GridBagConstraints.WEST
        c.fill = GridBagConstraints.HORIZONTAL
        c.weightx = 1.0
        c.insets = JBUI.emptyInsets()
        panel.add(mcpEnabled, c)

        c.gridwidth = 1
        c.gridy = 1
        c.gridx = 0
        c.fill = GridBagConstraints.NONE
        c.weightx = 0.0
        panel.add(Box.createHorizontalStrut(JBUI.scale(26)), c)

        c.gridx = 1
        c.fill = GridBagConstraints.HORIZONTAL
        c.weightx = 1.0
        c.insets = JBUI.insetsTop(1)
        panel.add(createMcpHintRow(), c)

        return panel
    }

    private fun createMcpHintRow(): JPanel {
        val hintRow = JPanel(FlowLayout(FlowLayout.LEFT, 0, 0))
        hintRow.isOpaque = false

        val smallFont = UIUtil.getLabelFont(UIUtil.FontSize.SMALL)
        val hintColor = UIUtil.getContextHelpForeground()

        hintRow.add(createHintLabel("Application-wide: requires an IDE restart or ", smallFont, hintColor))
        hintRow.add(createSettingsLinkLabel("re-enabling the MCP server", smallFont, "com.intellij.mcpserver.settings"))
        hintRow.add(createHintLabel("; configure tools in ", smallFont, hintColor))
        hintRow.add(createSettingsLinkLabel("Exposed Tools", smallFont, "com.intellij.mcpserver.settings.filter"))
        hintRow.add(createHintLabel(".", smallFont, hintColor))

        return hintRow
    }

    private fun createHintLabel(text: String, font: Font, color: Color): JLabel {
        val label = JLabel(text)
        label.font = font
        label.foreground = color
        return label
    }

    private fun createSettingsLinkLabel(text: String, font: Font, configurableId: String): JLabel {
        val linkLabel = JLabel(text)
        linkLabel.font = font
        linkLabel.foreground = UIManager.getColor("link.foreground")
            ?: JBColor(Color(0x2470B3), Color(0x589DF6))
        linkLabel.cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        linkLabel.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                val dataContext: DataContext = DataManager.getInstance().getDataContext(linkLabel)
                val ideSettings = com.intellij.openapi.options.ex.Settings.KEY.getData(dataContext)
                val configurable = ideSettings?.find(configurableId)
                if (configurable != null) {
                    ideSettings.select(configurable)
                }
            }
        })

        return linkLabel
    }

    private fun createCheckWithHint(checkBox: JCheckBox, hint: String): JPanel {
        val panel = JPanel(GridBagLayout())
        panel.isOpaque = false
        val c = GridBagConstraints()

        // Row 0: checkbox spans full width
        c.gridx = 0
        c.gridy = 0
        c.gridwidth = 2
        c.anchor = GridBagConstraints.WEST
        c.fill = GridBagConstraints.HORIZONTAL
        c.weightx = 1.0
        c.insets = JBUI.emptyInsets()
        panel.add(checkBox, c)

        // Row 1 col 0: strut sized to checkbox icon+gap so hint aligns with label text
        c.gridwidth = 1
        c.gridy = 1
        c.gridx = 0
        c.fill = GridBagConstraints.NONE
        c.weightx = 0.0
        c.insets = JBUI.emptyInsets()
        panel.add(Box.createHorizontalStrut(JBUI.scale(26)), c)

        // Row 1 col 1: hint label flush-left with checkbox text
        val hintLabel = JBLabel(hint, UIUtil.ComponentStyle.SMALL, UIUtil.FontColor.BRIGHTER)
        c.gridx = 1
        c.fill = GridBagConstraints.HORIZONTAL
        c.weightx = 1.0
        c.insets = JBUI.insetsTop(1)
        panel.add(hintLabel, c)

        return panel
    }

    private fun createFieldWithHint(label: String, field: JComponent, resetBtn: JButton?, hint: String): JPanel {
        val panel = JPanel(GridBagLayout())
        panel.isOpaque = false
        val c = GridBagConstraints()
        c.gridwidth = GridBagConstraints.REMAINDER
        c.anchor = GridBagConstraints.WEST

        // Row 0: label on its own line
        c.gridx = 0
        c.gridy = 0
        c.fill = GridBagConstraints.NONE
        c.weightx = 0.0
        c.insets = JBUI.insetsBottom(2)
        panel.add(JLabel(label), c)

        // Row 1: input field + reset button — field stretches full width
        val inputRow = JPanel(GridBagLayout())
        inputRow.isOpaque = false
        val ic = GridBagConstraints()
        ic.gridx = 0
        ic.gridy = 0
        ic.fill = GridBagConstraints.HORIZONTAL
        ic.weightx = 1.0
        ic.insets = JBUI.emptyInsets()
        inputRow.add(field, ic)
        if (resetBtn != null) {
            ic.gridx = 1
            ic.fill = GridBagConstraints.NONE
            ic.weightx = 0.0
            ic.insets = JBUI.insetsLeft(4)
            inputRow.add(resetBtn, ic)
        }
        c.gridy = 1
        c.fill = GridBagConstraints.HORIZONTAL
        c.weightx = 1.0
        c.insets = JBUI.emptyInsets()
        panel.add(inputRow, c)

        // Row 2: hint flush-left below the input
        val hintLabel = JBLabel(hint, UIUtil.ComponentStyle.SMALL, UIUtil.FontColor.BRIGHTER)
        c.gridy = 2
        c.fill = GridBagConstraints.HORIZONTAL
        c.insets = JBUI.insetsTop(2)
        panel.add(hintLabel, c)

        return panel
    }

    override fun isModified(): Boolean {
        return pluginEnabled.isSelected != settings.pluginEnabled ||
            mcpEnabled.isSelected != applicationSettings.mcpEnabled ||
            pathToTranslationRootTextField.text != settings.pathToTranslation ||
            codeFoldingPhpRoute.isSelected != settings.codeFoldingPhpRoute ||
            codeFoldingPhpModel.isSelected != settings.codeFoldingPhpModel ||
            codeFoldingPhpTemplate.isSelected != settings.codeFoldingPhpTemplate ||
            codeFoldingTwigRoute.isSelected != settings.codeFoldingTwigRoute ||
            codeFoldingTwigTemplate.isSelected != settings.codeFoldingTwigTemplate ||
            codeFoldingTwigConstant.isSelected != settings.codeFoldingTwigConstant ||
            featureTwigIcon.isSelected != settings.featureTwigIcon ||
            featurePhpClassIcon.isSelected != settings.featurePhpClassIcon ||
            featureTypeProvider.isSelected != settings.featureTypeProvider ||
            featurePropertyInjection.isSelected != settings.featurePropertyInjection ||
            showYamlSchemaNotification.isSelected == settings.dismissYamlSchemaNotification ||
            directoryToApp.text != settings.directoryToApp ||
            directoryToWeb.text != settings.directoryToWeb
    }

    override fun apply() {
        settings.pluginEnabled = pluginEnabled.isSelected
        applicationSettings.mcpEnabled = mcpEnabled.isSelected

        settings.pathToTranslation = pathToTranslationRootTextField.text
        settings.codeFoldingPhpRoute = codeFoldingPhpRoute.isSelected
        settings.codeFoldingPhpModel = codeFoldingPhpModel.isSelected
        settings.codeFoldingPhpTemplate = codeFoldingPhpTemplate.isSelected
        settings.codeFoldingTwigRoute = codeFoldingTwigRoute.isSelected
        settings.codeFoldingTwigTemplate = codeFoldingTwigTemplate.isSelected
        settings.codeFoldingTwigConstant = codeFoldingTwigConstant.isSelected
        settings.featureTwigIcon = featureTwigIcon.isSelected
        settings.featurePhpClassIcon = featurePhpClassIcon.isSelected
        settings.featureTypeProvider = featureTypeProvider.isSelected
        settings.featurePropertyInjection = featurePropertyInjection.isSelected
        settings.dismissYamlSchemaNotification = !showYamlSchemaNotification.isSelected

        settings.directoryToApp = directoryToApp.text
        settings.directoryToWeb = directoryToWeb.text

        syncSymfonyVarDirectoryWatcher(project)
    }

    override fun reset() {
        updateUIFromSettings()
    }

    private val settings: Settings
        get() = Settings.getInstance(project)

    private val applicationSettings: McpApplicationSettings
        get() = McpApplicationSettings.getInstance()

    private fun updateUIFromSettings() {
        pluginEnabled.isSelected = settings.pluginEnabled
        mcpEnabled.isSelected = applicationSettings.mcpEnabled

        pathToTranslationRootTextField.text = settings.pathToTranslation
        codeFoldingPhpRoute.isSelected = settings.codeFoldingPhpRoute
        codeFoldingPhpModel.isSelected = settings.codeFoldingPhpModel
        codeFoldingPhpTemplate.isSelected = settings.codeFoldingPhpTemplate
        codeFoldingTwigRoute.isSelected = settings.codeFoldingTwigRoute
        codeFoldingTwigTemplate.isSelected = settings.codeFoldingTwigTemplate
        codeFoldingTwigConstant.isSelected = settings.codeFoldingTwigConstant
        featureTwigIcon.isSelected = settings.featureTwigIcon
        featurePhpClassIcon.isSelected = settings.featurePhpClassIcon
        featureTypeProvider.isSelected = settings.featureTypeProvider
        featurePropertyInjection.isSelected = settings.featurePropertyInjection
        showYamlSchemaNotification.isSelected = !settings.dismissYamlSchemaNotification

        directoryToApp.text = settings.directoryToApp
        directoryToWeb.text = settings.directoryToWeb
    }

    private fun createBrowseFolderListener(
        textField: JTextField,
        fileChooserDescriptor: FileChooserDescriptor,
    ): TextBrowseFolderListener {
        return object : TextBrowseFolderListener(fileChooserDescriptor) {
            override fun actionPerformed(e: ActionEvent) {
                val projectDirectory = ProjectUtil.getProjectDir(this@SettingsForm.project)
                val selectedFile = FileChooser.chooseFile(
                    fileChooserDescriptor,
                    this@SettingsForm.project,
                    VfsUtil.findRelativeFile(textField.text, projectDirectory),
                ) ?: return

                val path = VfsUtil.getRelativePath(selectedFile, projectDirectory, '/') ?: selectedFile.path
                textField.text = path
            }
        }
    }

    private fun createResetPathButtonMouseListener(textField: JTextField, defaultValue: String): MouseListener {
        return object : MouseListener {
            override fun mouseClicked(mouseEvent: MouseEvent) = Unit

            override fun mousePressed(mouseEvent: MouseEvent) {
                textField.text = defaultValue
            }

            override fun mouseReleased(mouseEvent: MouseEvent) = Unit

            override fun mouseEntered(mouseEvent: MouseEvent) = Unit

            override fun mouseExited(mouseEvent: MouseEvent) = Unit
        }
    }
}
