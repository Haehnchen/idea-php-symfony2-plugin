package fr.adrienbrault.idea.symfony2plugin;

import com.intellij.ide.actions.ShowSettingsUtilImpl;
import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.TextBrowseFolderListener;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.JBColor;
import com.intellij.ui.TitledSeparator;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import fr.adrienbrault.idea.symfony2plugin.mcp.McpApplicationSettings;
import fr.adrienbrault.idea.symfony2plugin.stubs.util.IndexUtil;
import fr.adrienbrault.idea.symfony2plugin.util.IdeHelper;
import fr.adrienbrault.idea.symfony2plugin.util.ProjectUtil;
import fr.adrienbrault.idea.symfony2plugin.util.SymfonyVarDirectoryWatcherKt;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.List;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class SettingsForm implements Configurable {

    private final Project project;

    private JPanel panel1;

    private JCheckBox pluginEnabled;

    private JButton directoryToWebReset;
    private TextFieldWithBrowseButton directoryToWeb;

    private JButton directoryToAppReset;
    private TextFieldWithBrowseButton directoryToApp;
    private JButton pathToTranslationRootTextFieldReset;
    private TextFieldWithBrowseButton pathToTranslationRootTextField;
    private JButton buttonHelp;

    private JCheckBox codeFoldingPhpRoute;
    private JCheckBox codeFoldingPhpModel;
    private JCheckBox codeFoldingTwigRoute;
    private JCheckBox codeFoldingPhpTemplate;
    private JCheckBox codeFoldingTwigTemplate;
    private JCheckBox codeFoldingTwigConstant;

    private JButton buttonReindex;
    private JCheckBox featureTwigIcon;
    private JButton buttonBuyLicense;
    private JButton buttonAutoConfigure;
    private JCheckBox featureTypeProvider;
    private JCheckBox featurePropertyInjection;
    private JCheckBox dismissYamlSchemaNotification;
    private JCheckBox mcpEnabled;

    public SettingsForm(@NotNull final Project project) {
        this.project = project;
        createUIComponents();
        buttonHelp.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                IdeHelper.openUrl("https://espend.de/phpstorm/plugin/symfony");
            }
        });

        buttonBuyLicense.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                IdeHelper.openUrl("https://plugins.jetbrains.com/plugin/7219-symfony-support/pricing");
            }
        });
    }

    @Nls
    @Override
    public String getDisplayName() {
        return "Symfony Plugin";
    }

    @Nullable
    @Override
    public String getHelpTopic() {
        return null;
    }

    private void createUIComponents() {
        pluginEnabled = new JCheckBox("Enable Plugin for this Project");
        buttonHelp = new JButton("Open Help Page");
        buttonAutoConfigure = new JButton("Autoconfigure");
        buttonBuyLicense = new JButton("Buy License");
        buttonReindex = new JButton("Clear Index");
        mcpEnabled = new JCheckBox("Enable MCP Tools");

        pathToTranslationRootTextField = new TextFieldWithBrowseButton();
        pathToTranslationRootTextFieldReset = new JButton("Default");
        directoryToWeb = new TextFieldWithBrowseButton();
        directoryToWebReset = new JButton("Default");
        directoryToApp = new TextFieldWithBrowseButton();
        directoryToAppReset = new JButton("Default");

        codeFoldingPhpRoute = new JCheckBox("Route (PHP)");
        codeFoldingPhpModel = new JCheckBox("Repository Entity (PHP)");
        codeFoldingPhpTemplate = new JCheckBox("Template (PHP)");
        codeFoldingTwigRoute = new JCheckBox("Route (Twig)");
        codeFoldingTwigTemplate = new JCheckBox("Template (Twig)");
        codeFoldingTwigConstant = new JCheckBox("Constant (Twig)");
        featureTwigIcon = new JCheckBox("Twig Icon Decoration");
        dismissYamlSchemaNotification = new JCheckBox("Dismiss YAML Schema Notification");
        featurePropertyInjection = new JCheckBox("Service Property Injection");
        featureTypeProvider = new JCheckBox("Type Provider");
    }

    public JComponent createComponent() {
        pathToTranslationRootTextField.addBrowseFolderListener(createBrowseFolderListener(pathToTranslationRootTextField.getTextField(), FileChooserDescriptorFactory.createSingleFolderDescriptor()));
        pathToTranslationRootTextFieldReset.addMouseListener(createResetPathButtonMouseListener(pathToTranslationRootTextField.getTextField(), Settings.DEFAULT_TRANSLATION_PATH));

        directoryToApp.addBrowseFolderListener(createBrowseFolderListener(directoryToApp.getTextField(), FileChooserDescriptorFactory.createSingleFolderDescriptor()));
        directoryToAppReset.addMouseListener(createResetPathButtonMouseListener(directoryToApp.getTextField(), Settings.DEFAULT_APP_DIRECTORY));

        directoryToWeb.addBrowseFolderListener(createBrowseFolderListener(directoryToWeb.getTextField(), FileChooserDescriptorFactory.createSingleFolderDescriptor()));
        directoryToWebReset.addMouseListener(createResetPathButtonMouseListener(directoryToWeb.getTextField(), Settings.DEFAULT_WEB_DIRECTORY));

        buttonReindex.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                IndexUtil.forceReindex();
                super.mouseClicked(e);
            }
        });

        buttonAutoConfigure.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                List<String> list = IdeHelper.enablePluginAndConfigure(project).stream().map(s -> "- " + s).toList();
                getSettings().pluginEnabled = true;
                updateUIFromSettings();
                JOptionPane.showMessageDialog(panel1, "Plugin activated and configured with:\n" + StringUtils.join(list, "\n"), "Symfony Plugin", JOptionPane.PLAIN_MESSAGE);
            }
        });

        JPanel content = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.insets = JBUI.emptyInsets();
        int row = 0;

        // ── General ──────────────────────────────────────────────────────────
        gbc.gridy = row++;
        content.add(new TitledSeparator("General"), gbc);

        gbc.gridy = row++;
        gbc.insets = JBUI.insets(4, 8, 0, 0);
        content.add(createCheckWithHint(pluginEnabled,
            "Activates all Symfony-specific features for this project. Requires an IDE restart."), gbc);

        gbc.gridy = row++;
        gbc.insets = JBUI.insets(8, 8, 4, 0);
        JPanel actionBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        actionBar.add(buttonAutoConfigure);
        actionBar.add(buttonHelp);
        actionBar.add(buttonReindex);
        actionBar.add(buttonBuyLicense);
        content.add(actionBar, gbc);

        gbc.gridy = row++;
        gbc.insets = JBUI.insets(4, 8, 0, 0);
        content.add(createMcpCheckPanel(), gbc);

        // ── Paths ─────────────────────────────────────────────────────────────
        gbc.gridy = row++;
        gbc.insets = JBUI.insetsTop(12);
        content.add(new TitledSeparator("Paths"), gbc);

        gbc.gridy = row++;
        gbc.insets = JBUI.insets(6, 8, 0, 0);
        content.add(createFieldWithHint("Translation Root Path", pathToTranslationRootTextField, pathToTranslationRootTextFieldReset,
            "Root directory for translation files (e.g. translations/)"), gbc);

        gbc.gridy = row++;
        gbc.insets = JBUI.insets(8, 8, 0, 0);
        content.add(createFieldWithHint("Web Directory", directoryToWeb, directoryToWebReset,
            "Public web root directory, used to resolve asset paths (e.g. public/ or web/)"), gbc);

        // ── Features ──────────────────────────────────────────────────────────
        gbc.gridy = row++;
        gbc.insets = JBUI.insetsTop(12);
        content.add(new TitledSeparator("Features"), gbc);

        Object[][] featureItems = {
            {featureTwigIcon,                "Overlay Twig file icons with a badge indicating usage: extends, include, controller, ..."},
            {dismissYamlSchemaNotification,  "Suppress the banner suggesting to add a YAML schema hint for out-of-box Symfony service completion"},
            {featurePropertyInjection,       "Autocomplete properties in service and auto injection class via constructor"},
            {featureTypeProvider,            "Resolve return types for container calls like ContainerInterface::get() and EntityManager::find()"},
        };
        for (Object[] item : featureItems) {
            gbc.gridy = row++;
            gbc.insets = JBUI.insets(4, 8, 0, 0);
            content.add(createCheckWithHint((JCheckBox) item[0], (String) item[1]), gbc);
        }

        // ── Code Folding ──────────────────────────────────────────────────────
        gbc.gridy = row++;
        gbc.insets = JBUI.insetsTop(12);
        content.add(new TitledSeparator("Code Folding"), gbc);

        Object[][] foldingItems = {
            {codeFoldingPhpRoute,    "Route name strings in PHP files to a short label"},
            {codeFoldingPhpModel,    "Doctrine entity repository class names in PHP"},
            {codeFoldingPhpTemplate, "Template paths in PHP render() calls"},
            {codeFoldingTwigRoute,   "Route name strings in Twig templates to url"},
            {codeFoldingTwigTemplate,"Template paths in Twig include and extends tags"},
            {codeFoldingTwigConstant,"Fold PHP constant / enums references in Twig"},
        };
        for (Object[] item : foldingItems) {
            gbc.gridy = row++;
            gbc.insets = JBUI.insets(4, 8, 0, 0);
            content.add(createCheckWithHint((JCheckBox) item[0], (String) item[1]), gbc);
        }

        // ── Legacy ────────────────────────────────────────────────────────────
        gbc.gridy = row++;
        gbc.insets = JBUI.insetsTop(12);
        content.add(new TitledSeparator("Legacy"), gbc);

        gbc.gridy = row++;
        gbc.insets = JBUI.insets(6, 8, 0, 0);
        content.add(createFieldWithHint("App Directory", directoryToApp, directoryToAppReset,
            "Legacy Symfony 2/3 app/ directory containing config, cache, and logs"), gbc);

        // Filler
        gbc.gridy = row;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1.0;
        gbc.insets = JBUI.emptyInsets();
        content.add(Box.createVerticalGlue(), gbc);

        panel1 = new JPanel(new BorderLayout());
        panel1.add(content, BorderLayout.CENTER);

        return panel1;
    }

    private JPanel createMcpCheckPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();

        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 2;
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0;
        c.insets = JBUI.emptyInsets();
        panel.add(mcpEnabled, c);

        c.gridwidth = 1;
        c.gridy = 1;
        c.gridx = 0;
        c.fill = GridBagConstraints.NONE;
        c.weightx = 0;
        panel.add(Box.createHorizontalStrut(JBUI.scale(26)), c);

        c.gridx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0;
        c.insets = JBUI.insetsTop(1);
        panel.add(createMcpHintRow(), c);

        return panel;
    }

    private JPanel createMcpHintRow() {
        JPanel hintRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        hintRow.setOpaque(false);

        Font smallFont = UIUtil.getLabelFont(UIUtil.FontSize.SMALL);
        Color hintColor = UIUtil.getContextHelpForeground();

        hintRow.add(createHintLabel("Application-wide: requires an IDE restart or ", smallFont, hintColor));
        hintRow.add(createSettingsLinkLabel("re-enabling the MCP server", smallFont, "com.intellij.mcpserver.settings"));
        hintRow.add(createHintLabel("; configure tools in ", smallFont, hintColor));
        hintRow.add(createSettingsLinkLabel("Exposed Tools", smallFont, "com.intellij.mcpserver.settings.filter"));
        hintRow.add(createHintLabel(".", smallFont, hintColor));

        return hintRow;
    }

    private JLabel createHintLabel(String text, Font font, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(font);
        label.setForeground(color);
        return label;
    }

    private JLabel createSettingsLinkLabel(String text, Font font, String configurableId) {
        JLabel linkLabel = new JLabel(text);
        linkLabel.setFont(font);
        linkLabel.setForeground(UIManager.getColor("link.foreground") != null
            ? UIManager.getColor("link.foreground")
            : new JBColor(new Color(0x2470B3), new Color(0x589DF6)));
        linkLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        linkLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                DataContext dataContext = DataManager.getInstance().getDataContext(linkLabel);
                com.intellij.openapi.options.ex.Settings settings = com.intellij.openapi.options.ex.Settings.KEY.getData(dataContext);
                if (settings != null) {
                    Configurable configurable = settings.find(configurableId);
                    if (configurable != null) {
                        settings.select(configurable);
                    }
                }
            }
        });

        return linkLabel;
    }

    private JPanel createCheckWithHint(JCheckBox checkBox, String hint) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();

        // Row 0: checkbox spans full width
        c.gridx = 0; c.gridy = 0; c.gridwidth = 2;
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0;
        c.insets = JBUI.emptyInsets();
        panel.add(checkBox, c);

        // Row 1 col 0: strut sized to checkbox icon+gap so hint aligns with label text
        c.gridwidth = 1; c.gridy = 1;
        c.gridx = 0; c.fill = GridBagConstraints.NONE; c.weightx = 0;
        c.insets = JBUI.emptyInsets();
        panel.add(Box.createHorizontalStrut(JBUI.scale(26)), c);

        // Row 1 col 1: hint label flush-left with checkbox text
        JBLabel hintLabel = new JBLabel(hint, UIUtil.ComponentStyle.SMALL, UIUtil.FontColor.BRIGHTER);
        c.gridx = 1; c.fill = GridBagConstraints.HORIZONTAL; c.weightx = 1.0;
        c.insets = JBUI.insetsTop(1);
        panel.add(hintLabel, c);

        return panel;
    }

    private JPanel createFieldWithHint(String label, JComponent field, @Nullable JButton resetBtn, String hint) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.WEST;

        // Row 0: label on its own line
        c.gridx = 0; c.gridy = 0;
        c.fill = GridBagConstraints.NONE; c.weightx = 0;
        c.insets = JBUI.insetsBottom(2);
        panel.add(new JLabel(label), c);

        // Row 1: input field + reset button — field stretches full width
        JPanel inputRow = new JPanel(new GridBagLayout());
        inputRow.setOpaque(false);
        GridBagConstraints ic = new GridBagConstraints();
        ic.gridx = 0; ic.gridy = 0; ic.fill = GridBagConstraints.HORIZONTAL; ic.weightx = 1.0;
        ic.insets = JBUI.emptyInsets();
        inputRow.add(field, ic);
        if (resetBtn != null) {
            ic.gridx = 1; ic.fill = GridBagConstraints.NONE; ic.weightx = 0;
            ic.insets = JBUI.insetsLeft(4);
            inputRow.add(resetBtn, ic);
        }
        c.gridy = 1; c.fill = GridBagConstraints.HORIZONTAL; c.weightx = 1.0;
        c.insets = JBUI.emptyInsets();
        panel.add(inputRow, c);

        // Row 2: hint flush-left below the input
        JBLabel hintLabel = new JBLabel(hint, UIUtil.ComponentStyle.SMALL, UIUtil.FontColor.BRIGHTER);
        c.gridy = 2; c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = JBUI.insetsTop(2);
        panel.add(hintLabel, c);

        return panel;
    }

    @Override
    public boolean isModified() {
        return
            !pluginEnabled.isSelected() == getSettings().pluginEnabled
                || !mcpEnabled.isSelected() == getApplicationSettings().mcpEnabled
                || !pathToTranslationRootTextField.getText().equals(getSettings().pathToTranslation)
                || !codeFoldingPhpRoute.isSelected() == getSettings().codeFoldingPhpRoute
                || !codeFoldingPhpModel.isSelected() == getSettings().codeFoldingPhpModel
                || !codeFoldingPhpTemplate.isSelected() == getSettings().codeFoldingPhpTemplate
                || !codeFoldingTwigRoute.isSelected() == getSettings().codeFoldingTwigRoute
                || !codeFoldingTwigTemplate.isSelected() == getSettings().codeFoldingTwigTemplate
                || !codeFoldingTwigConstant.isSelected() == getSettings().codeFoldingTwigConstant
                || !featureTwigIcon.isSelected() == getSettings().featureTwigIcon
                || !featureTypeProvider.isSelected() == getSettings().featureTypeProvider
                || !featurePropertyInjection.isSelected() == getSettings().featurePropertyInjection
                || !dismissYamlSchemaNotification.isSelected() == getSettings().dismissYamlSchemaNotification

                || !directoryToApp.getText().equals(getSettings().directoryToApp)
                || !directoryToWeb.getText().equals(getSettings().directoryToWeb)
            ;
    }

    @Override
    public void apply() throws ConfigurationException {
        getSettings().pluginEnabled = pluginEnabled.isSelected();
        getApplicationSettings().mcpEnabled = mcpEnabled.isSelected();

        getSettings().pathToTranslation = pathToTranslationRootTextField.getText();
        getSettings().codeFoldingPhpRoute = codeFoldingPhpRoute.isSelected();
        getSettings().codeFoldingPhpModel = codeFoldingPhpModel.isSelected();
        getSettings().codeFoldingPhpTemplate = codeFoldingPhpTemplate.isSelected();
        getSettings().codeFoldingTwigRoute = codeFoldingTwigRoute.isSelected();
        getSettings().codeFoldingTwigTemplate = codeFoldingTwigTemplate.isSelected();
        getSettings().codeFoldingTwigConstant = codeFoldingTwigConstant.isSelected();
        getSettings().featureTwigIcon = featureTwigIcon.isSelected();
        getSettings().featureTypeProvider = featureTypeProvider.isSelected();
        getSettings().featurePropertyInjection = featurePropertyInjection.isSelected();
        getSettings().dismissYamlSchemaNotification = dismissYamlSchemaNotification.isSelected();

        getSettings().directoryToApp = directoryToApp.getText();
        getSettings().directoryToWeb = directoryToWeb.getText();

        SymfonyVarDirectoryWatcherKt.syncSymfonyVarDirectoryWatcher(project);
    }

    @Override
    public void reset() {
        updateUIFromSettings();
    }

    private Settings getSettings() {
        return Settings.getInstance(project);
    }

    private McpApplicationSettings getApplicationSettings() {
        return McpApplicationSettings.Companion.getInstance();
    }

    private void updateUIFromSettings() {
        pluginEnabled.setSelected(getSettings().pluginEnabled);
        mcpEnabled.setSelected(getApplicationSettings().mcpEnabled);

        pathToTranslationRootTextField.setText(getSettings().pathToTranslation);
        codeFoldingPhpRoute.setSelected(getSettings().codeFoldingPhpRoute);
        codeFoldingPhpModel.setSelected(getSettings().codeFoldingPhpModel);
        codeFoldingPhpTemplate.setSelected(getSettings().codeFoldingPhpTemplate);
        codeFoldingTwigRoute.setSelected(getSettings().codeFoldingTwigRoute);
        codeFoldingTwigTemplate.setSelected(getSettings().codeFoldingTwigTemplate);
        codeFoldingTwigConstant.setSelected(getSettings().codeFoldingTwigConstant);
        featureTwigIcon.setSelected(getSettings().featureTwigIcon);
        featureTypeProvider.setSelected(getSettings().featureTypeProvider);
        featurePropertyInjection.setSelected(getSettings().featurePropertyInjection);
        dismissYamlSchemaNotification.setSelected(getSettings().dismissYamlSchemaNotification);

        directoryToApp.setText(getSettings().directoryToApp);
        directoryToWeb.setText(getSettings().directoryToWeb);
    }

    private TextBrowseFolderListener createBrowseFolderListener(final JTextField textField, final FileChooserDescriptor fileChooserDescriptor) {
        return new TextBrowseFolderListener(fileChooserDescriptor) {
            @Override
            public void actionPerformed(ActionEvent e) {
                VirtualFile projectDirectory = ProjectUtil.getProjectDir(project);
                VirtualFile selectedFile = FileChooser.chooseFile(
                    fileChooserDescriptor,
                    project,
                    VfsUtil.findRelativeFile(textField.getText(), projectDirectory)
                );

                if (null == selectedFile) {
                    return;
                }

                String path = VfsUtil.getRelativePath(selectedFile, projectDirectory, '/');
                if (null == path) {
                    path = selectedFile.getPath();
                }

                textField.setText(path);
            }
        };
    }

    private MouseListener createResetPathButtonMouseListener(final JTextField textField, final String defaultValue) {
        return new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent mouseEvent) {
            }

            @Override
            public void mousePressed(MouseEvent mouseEvent) {
                textField.setText(defaultValue);
            }

            @Override
            public void mouseReleased(MouseEvent mouseEvent) {
            }

            @Override
            public void mouseEntered(MouseEvent mouseEvent) {
            }

            @Override
            public void mouseExited(MouseEvent mouseEvent) {
            }
        };
    }

    public static void show(@NotNull Project project) {
        ShowSettingsUtilImpl.showSettingsDialog(project, "Symfony2.SettingsForm", null);
    }
}
