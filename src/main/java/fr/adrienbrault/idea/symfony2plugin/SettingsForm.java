package fr.adrienbrault.idea.symfony2plugin;

import com.intellij.ide.actions.ShowSettingsUtilImpl;
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
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import fr.adrienbrault.idea.symfony2plugin.stubs.util.IndexUtil;
import fr.adrienbrault.idea.symfony2plugin.util.IdeHelper;
import fr.adrienbrault.idea.symfony2plugin.util.ProjectUtil;
import fr.adrienbrault.idea.symfony2plugin.webDeployment.WebDeploymentUtil;
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
    private JLabel directoryToWebLabel;
    private TextFieldWithBrowseButton directoryToWeb;

    private JButton directoryToAppReset;
    private JLabel directoryToAppLabel;
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
    private JCheckBox enableSchedulerCheckBox;
    private JCheckBox featureTwigIcon;
    private JButton buttonAutoConfigure;
    private JCheckBox featureTypeProvider;
    private JCheckBox dismissYamlSchemaNotification;

    public SettingsForm(@NotNull final Project project) {
        this.project = project;
        buttonHelp.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                IdeHelper.openUrl("https://espend.de/phpstorm/plugin/symfony");
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

    public JComponent createComponent() {
        pathToTranslationRootTextField.addBrowseFolderListener(createBrowseFolderListener(pathToTranslationRootTextField.getTextField(), FileChooserDescriptorFactory.createSingleFolderDescriptor()));
        pathToTranslationRootTextFieldReset.addMouseListener(createResetPathButtonMouseListener(pathToTranslationRootTextField.getTextField(), Settings.DEFAULT_TRANSLATION_PATH));

        directoryToApp.addBrowseFolderListener(createBrowseFolderListener(directoryToApp.getTextField(), FileChooserDescriptorFactory.createSingleFolderDescriptor()));
        directoryToAppReset.addMouseListener(createResetPathButtonMouseListener(directoryToApp.getTextField(), Settings.DEFAULT_APP_DIRECTORY));

        directoryToWeb.addBrowseFolderListener(createBrowseFolderListener(directoryToWeb.getTextField(), FileChooserDescriptorFactory.createSingleFolderDescriptor()));
        directoryToWebReset.addMouseListener(createResetPathButtonMouseListener(directoryToWeb.getTextField(), Settings.DEFAULT_WEB_DIRECTORY));

        enableSchedulerCheckBox.setEnabled(WebDeploymentUtil.isEnabled(project));

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

        return panel1;
    }

    @Override
    public boolean isModified() {
        return
            !pluginEnabled.isSelected() == getSettings().pluginEnabled
                || !pathToTranslationRootTextField.getText().equals(getSettings().pathToTranslation)
                || !enableSchedulerCheckBox.isSelected() == getSettings().remoteDevFileScheduler

                || !codeFoldingPhpRoute.isSelected() == getSettings().codeFoldingPhpRoute
                || !codeFoldingPhpModel.isSelected() == getSettings().codeFoldingPhpModel
                || !codeFoldingPhpTemplate.isSelected() == getSettings().codeFoldingPhpTemplate
                || !codeFoldingTwigRoute.isSelected() == getSettings().codeFoldingTwigRoute
                || !codeFoldingTwigTemplate.isSelected() == getSettings().codeFoldingTwigTemplate
                || !codeFoldingTwigConstant.isSelected() == getSettings().codeFoldingTwigConstant
                || !featureTwigIcon.isSelected() == getSettings().featureTwigIcon
                || !featureTypeProvider.isSelected() == getSettings().featureTypeProvider
                || !dismissYamlSchemaNotification.isSelected() == getSettings().dismissYamlSchemaNotification

                || !directoryToApp.getText().equals(getSettings().directoryToApp)
                || !directoryToWeb.getText().equals(getSettings().directoryToWeb)
            ;
    }

    @Override
    public void apply() throws ConfigurationException {

        getSettings().pluginEnabled = pluginEnabled.isSelected();

        getSettings().pathToTranslation = pathToTranslationRootTextField.getText();
        getSettings().remoteDevFileScheduler = enableSchedulerCheckBox.isSelected();

        getSettings().codeFoldingPhpRoute = codeFoldingPhpRoute.isSelected();
        getSettings().codeFoldingPhpModel = codeFoldingPhpModel.isSelected();
        getSettings().codeFoldingPhpTemplate = codeFoldingPhpTemplate.isSelected();
        getSettings().codeFoldingTwigRoute = codeFoldingTwigRoute.isSelected();
        getSettings().codeFoldingTwigTemplate = codeFoldingTwigTemplate.isSelected();
        getSettings().codeFoldingTwigConstant = codeFoldingTwigConstant.isSelected();
        getSettings().featureTwigIcon = featureTwigIcon.isSelected();
        getSettings().featureTypeProvider = featureTypeProvider.isSelected();
        getSettings().dismissYamlSchemaNotification = dismissYamlSchemaNotification.isSelected();

        getSettings().directoryToApp = directoryToApp.getText();
        getSettings().directoryToWeb = directoryToWeb.getText();
    }

    @Override
    public void reset() {
        updateUIFromSettings();
    }

    private Settings getSettings() {
        return Settings.getInstance(project);
    }

    private void updateUIFromSettings() {

        pluginEnabled.setSelected(getSettings().pluginEnabled);

        pathToTranslationRootTextField.setText(getSettings().pathToTranslation);
        enableSchedulerCheckBox.setSelected(getSettings().remoteDevFileScheduler);

        codeFoldingPhpRoute.setSelected(getSettings().codeFoldingPhpRoute);
        codeFoldingPhpModel.setSelected(getSettings().codeFoldingPhpModel);
        codeFoldingPhpTemplate.setSelected(getSettings().codeFoldingPhpTemplate);
        codeFoldingTwigRoute.setSelected(getSettings().codeFoldingTwigRoute);
        codeFoldingTwigTemplate.setSelected(getSettings().codeFoldingTwigTemplate);
        codeFoldingTwigConstant.setSelected(getSettings().codeFoldingTwigConstant);
        featureTwigIcon.setSelected(getSettings().featureTwigIcon);
        featureTypeProvider.setSelected(getSettings().featureTypeProvider);
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
                    return; // Ignore but keep the previous path
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

    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(2, 5, new Insets(0, 0, 0, 0), -1, -1));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new FormLayout("fill:max(d;4px):noGrow,left:4dlu:noGrow,fill:d:grow,left:4dlu:noGrow,fill:max(d;4px):noGrow,left:4dlu:noGrow,fill:max(d;4px):noGrow", "center:max(d;4px):noGrow,top:4dlu:noGrow,center:max(d;4px):noGrow,top:4dlu:noGrow,center:max(d;4px):noGrow,top:6dlu:noGrow,center:max(d;4px):noGrow,top:4dlu:noGrow,center:max(d;4px):noGrow,top:4dlu:noGrow,center:max(d;4px):noGrow,top:4dlu:noGrow,center:max(d;4px):noGrow,top:4dlu:noGrow,center:max(d;4px):noGrow,top:4dlu:noGrow,center:max(d;4px):noGrow,top:4dlu:noGrow,center:max(d;4px):noGrow,top:4dlu:noGrow,center:max(d;4px):noGrow,top:4dlu:noGrow,center:max(d;4px):noGrow,top:4dlu:noGrow,center:max(d;4px):noGrow,top:4dlu:noGrow,center:max(d;4px):noGrow,top:4dlu:noGrow,center:max(d;4px):noGrow,top:4dlu:noGrow,center:max(d;4px):noGrow,top:4dlu:noGrow,center:max(d;4px):noGrow"));
        panel1.add(panel2, new GridConstraints(1, 0, 1, 5, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        directoryToWebLabel = new JLabel();
        directoryToWebLabel.setText("Web Directory");
        CellConstraints cc = new CellConstraints();
        panel2.add(directoryToWebLabel, cc.xy(1, 3));
        directoryToWeb = new TextFieldWithBrowseButton();
        panel2.add(directoryToWeb, cc.xy(3, 3, CellConstraints.FILL, CellConstraints.DEFAULT));
        directoryToWebReset = new JButton();
        directoryToWebReset.setText("Default");
        panel2.add(directoryToWebReset, cc.xy(5, 3));
        final JLabel label1 = new JLabel();
        label1.setText("Translation Root Path");
        panel2.add(label1, cc.xy(1, 1));
        pathToTranslationRootTextField = new TextFieldWithBrowseButton();
        panel2.add(pathToTranslationRootTextField, cc.xy(3, 1, CellConstraints.FILL, CellConstraints.DEFAULT));
        pathToTranslationRootTextFieldReset = new JButton();
        pathToTranslationRootTextFieldReset.setText("Default");
        panel2.add(pathToTranslationRootTextFieldReset, cc.xy(5, 1));
        final JLabel label2 = new JLabel();
        label2.setText("Code Folding");
        panel2.add(label2, cc.xy(3, 9));
        codeFoldingPhpRoute = new JCheckBox();
        codeFoldingPhpRoute.setText("Route (PHP)");
        panel2.add(codeFoldingPhpRoute, cc.xy(3, 11));
        codeFoldingPhpModel = new JCheckBox();
        codeFoldingPhpModel.setText("Repository Entity (PHP)");
        panel2.add(codeFoldingPhpModel, cc.xy(3, 13));
        codeFoldingTwigRoute = new JCheckBox();
        codeFoldingTwigRoute.setText("Route (Twig)");
        panel2.add(codeFoldingTwigRoute, cc.xy(3, 17));
        codeFoldingPhpTemplate = new JCheckBox();
        codeFoldingPhpTemplate.setText("Template (PHP)");
        panel2.add(codeFoldingPhpTemplate, cc.xy(3, 15));
        codeFoldingTwigTemplate = new JCheckBox();
        codeFoldingTwigTemplate.setText("Template (Twig)");
        panel2.add(codeFoldingTwigTemplate, cc.xy(3, 19));
        codeFoldingTwigConstant = new JCheckBox();
        codeFoldingTwigConstant.setText("Constant (Twig)");
        panel2.add(codeFoldingTwigConstant, cc.xy(3, 21));
        enableSchedulerCheckBox = new JCheckBox();
        enableSchedulerCheckBox.setText("Enable 5min scheduler (reopen Project after change)");
        enableSchedulerCheckBox.setMnemonic('S');
        enableSchedulerCheckBox.setDisplayedMnemonicIndex(12);
        panel2.add(enableSchedulerCheckBox, cc.xy(3, 5));
        final JLabel label3 = new JLabel();
        label3.setText("Download remote files (exp.)");
        panel2.add(label3, cc.xy(1, 5));
        final JLabel label4 = new JLabel();
        label4.setText("Custom Features");
        panel2.add(label4, cc.xy(3, 23));
        featureTwigIcon = new JCheckBox();
        featureTwigIcon.setText("Twig Icon Decoration");
        featureTwigIcon.setToolTipText("Decorate Twig file icons with layer to show possible content type");
        panel2.add(featureTwigIcon, cc.xy(3, 25));
        directoryToAppLabel = new JLabel();
        directoryToAppLabel.setText("App Directory");
        panel2.add(directoryToAppLabel, cc.xy(1, 33));
        directoryToApp = new TextFieldWithBrowseButton();
        panel2.add(directoryToApp, cc.xy(3, 33, CellConstraints.FILL, CellConstraints.DEFAULT));
        directoryToAppReset = new JButton();
        directoryToAppReset.setText("Default");
        panel2.add(directoryToAppReset, cc.xy(5, 33));
        final JLabel label5 = new JLabel();
        label5.setText("Legacy Features");
        panel2.add(label5, cc.xy(1, 31));
        featureTypeProvider = new JCheckBox();
        featureTypeProvider.setText("Type Provider");
        featureTypeProvider.setToolTipText("Resolve return type via parameter eg \"ContainerInterface::get, EntityManager::find\"");
        panel2.add(featureTypeProvider, cc.xy(3, 29));
        dismissYamlSchemaNotification = new JCheckBox();
        dismissYamlSchemaNotification.setText("Dismiss YAML Schema Notification");
        dismissYamlSchemaNotification.setToolTipText("Don't show notification banner suggesting to add YAML schema hint for Symfony service files");
        panel2.add(dismissYamlSchemaNotification, cc.xy(3, 27));
        pluginEnabled = new JCheckBox();
        pluginEnabled.setText("Enable for Project (needs restart)");
        panel1.add(pluginEnabled, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        buttonHelp = new JButton();
        buttonHelp.setHorizontalAlignment(0);
        buttonHelp.setHorizontalTextPosition(4);
        buttonHelp.setText("Project Page / Documentation");
        panel1.add(buttonHelp, new GridConstraints(0, 4, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        buttonReindex = new JButton();
        buttonReindex.setText("Clear Index");
        panel1.add(buttonReindex, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        panel1.add(spacer1, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        buttonAutoConfigure = new JButton();
        buttonAutoConfigure.setText("Auto Configure");
        panel1.add(buttonAutoConfigure, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return panel1;
    }

}
