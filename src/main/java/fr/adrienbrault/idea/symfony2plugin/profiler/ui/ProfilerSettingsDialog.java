package fr.adrienbrault.idea.symfony2plugin.profiler.ui;

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
import fr.adrienbrault.idea.symfony2plugin.Settings;
import fr.adrienbrault.idea.symfony2plugin.util.ProjectUtil;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ProfilerSettingsDialog implements Configurable {
    @NotNull
    private final Project project;
    private JTextField textHttpProfilerUrl;
    private JRadioButton radioDefaultProfiler;
    private JRadioButton radioLocalProfiler;
    private JRadioButton radioHttpProfiler;
    private JPanel mainPanel;
    private JTextField textLocalProfilerUrl;
    private TextFieldWithBrowseButton textLocalProfilerCsvPath;

    public ProfilerSettingsDialog(@NotNull Project project) {
        this.project = project;
    }

    @Nls
    @Override
    public String getDisplayName() {
        return "Profiler";
    }

    @Nullable
    @Override
    public String getHelpTopic() {
        return null;
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        textLocalProfilerCsvPath.addBrowseFolderListener(createBrowseFolderListener(textLocalProfilerCsvPath.getTextField(), FileChooserDescriptorFactory.createSingleFolderDescriptor()));

        return mainPanel;
    }

    @Override
    public boolean isModified() {
        Settings settings = Settings.getInstance(project);

        return
            settings.profilerLocalEnabled != radioLocalProfiler.isSelected() ||
                !textLocalProfilerUrl.getText().equals(settings.profilerLocalUrl) ||
                !textLocalProfilerCsvPath.getText().equals(settings.profilerCsvPath) ||

                settings.profilerHttpEnabled != radioHttpProfiler.isSelected() ||
                !textHttpProfilerUrl.getText().equals(settings.profilerHttpUrl)
            ;
    }

    @Override
    public void apply() throws ConfigurationException {
        Settings settings = Settings.getInstance(project);

        settings.profilerLocalEnabled = radioLocalProfiler.isSelected();
        settings.profilerLocalUrl = textLocalProfilerUrl.getText();
        settings.profilerCsvPath = textLocalProfilerCsvPath.getText();

        settings.profilerHttpEnabled = radioHttpProfiler.isSelected();
        settings.profilerHttpUrl = textHttpProfilerUrl.getText();
    }

    @Override
    public void reset() {
        Settings setting = Settings.getInstance(project);

        radioLocalProfiler.setSelected(setting.profilerLocalEnabled);
        textLocalProfilerUrl.setText(setting.profilerLocalUrl);
        textLocalProfilerCsvPath.setText(setting.profilerCsvPath);

        radioHttpProfiler.setSelected(setting.profilerHttpEnabled);
        textHttpProfilerUrl.setText(setting.profilerHttpUrl);

        updateDefaultRadio();
    }

    private void updateDefaultRadio() {
        // set default if non profiler selected
        if (!radioLocalProfiler.isSelected() && !radioHttpProfiler.isSelected()) {
            radioDefaultProfiler.setSelected(true);
        }
    }

    @Override
    public void disposeUIResources() {

    }

    private TextBrowseFolderListener createBrowseFolderListener(final JTextField textField, final FileChooserDescriptor fileChooserDescriptor) {
        return new TextBrowseFolderListener(fileChooserDescriptor) {
            @Override
            public void actionPerformed(ActionEvent e) {
                VirtualFile projectDirectory = ProjectUtil.getProjectDir(project);

                String text = textField.getText();
                VirtualFile toSelect = VfsUtil.findRelativeFile(text, projectDirectory);
                if (toSelect == null) {
                    toSelect = projectDirectory;
                }

                VirtualFile selectedFile = FileChooser.chooseFile(
                    FileChooserDescriptorFactory.createSingleFileDescriptor("csv"),
                    project,
                    toSelect
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
        mainPanel = new JPanel();
        mainPanel.setLayout(new GridLayoutManager(4, 2, new Insets(0, 0, 0, 0), -1, -1));
        final Spacer spacer1 = new Spacer();
        mainPanel.add(spacer1, new GridConstraints(3, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(5, 2, new Insets(0, 0, 0, 0), -1, -1));
        mainPanel.add(panel1, new GridConstraints(1, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        radioLocalProfiler = new JRadioButton();
        radioLocalProfiler.setText("Local Profiler");
        panel1.add(radioLocalProfiler, new GridConstraints(0, 0, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        textLocalProfilerUrl = new JTextField();
        panel1.add(textLocalProfilerUrl, new GridConstraints(3, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JLabel label1 = new JLabel();
        label1.setText("Base url");
        panel1.add(label1, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label2 = new JLabel();
        label2.setText(" Overwrite baseUrl prefix for url navigation: http://127.0.0.1:8080, http://127.0.0.1/app_dev.php");
        panel1.add(label2, new GridConstraints(4, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        textLocalProfilerCsvPath = new TextFieldWithBrowseButton();
        panel1.add(textLocalProfilerCsvPath, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label3 = new JLabel();
        label3.setText("CSV File");
        panel1.add(label3, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label4 = new JLabel();
        label4.setText("Location for profiler csv file: var/cache/dev/index.csv");
        panel1.add(label4, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(3, 2, new Insets(0, 0, 0, 0), -1, -1));
        mainPanel.add(panel2, new GridConstraints(2, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label5 = new JLabel();
        label5.setText("Base url");
        panel2.add(label5, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        textHttpProfilerUrl = new JTextField();
        panel2.add(textHttpProfilerUrl, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        radioHttpProfiler = new JRadioButton();
        radioHttpProfiler.setText("HTTP Profiler");
        panel2.add(radioHttpProfiler, new GridConstraints(0, 0, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label6 = new JLabel();
        label6.setText("Profiler url for HTML extraction without \"_profiler\": http://127.0.0.1:8080. ");
        panel2.add(label6, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        mainPanel.add(panel3, new GridConstraints(0, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        radioDefaultProfiler = new JRadioButton();
        radioDefaultProfiler.setText("Default Profiler");
        panel3.add(radioDefaultProfiler, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label7 = new JLabel();
        label7.setText("Extract index.csv from container configuration");
        panel3.add(label7, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        ButtonGroup buttonGroup;
        buttonGroup = new ButtonGroup();
        buttonGroup.add(radioDefaultProfiler);
        buttonGroup.add(radioLocalProfiler);
        buttonGroup.add(radioHttpProfiler);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return mainPanel;
    }

}
