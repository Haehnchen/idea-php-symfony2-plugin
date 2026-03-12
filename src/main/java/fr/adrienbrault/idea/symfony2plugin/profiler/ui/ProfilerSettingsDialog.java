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
    private Component[] localComponents;
    private Component[] httpComponents;

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
        textHttpProfilerUrl = new JTextField(20);
        textLocalProfilerUrl = new JTextField(20);
        textLocalProfilerCsvPath = new TextFieldWithBrowseButton();
        radioDefaultProfiler = new JRadioButton("Default Profiler");
        radioLocalProfiler = new JRadioButton("Local Profiler");
        radioHttpProfiler = new JRadioButton("HTTP Profiler");

        ButtonGroup group = new ButtonGroup();
        group.add(radioDefaultProfiler);
        group.add(radioLocalProfiler);
        group.add(radioHttpProfiler);

        textLocalProfilerCsvPath.addBrowseFolderListener(createBrowseFolderListener(textLocalProfilerCsvPath.getTextField(), FileChooserDescriptorFactory.createSingleFolderDescriptor()));

        Font smallFont = UIManager.getFont("Label.font").deriveFont(Math.max(UIManager.getFont("Label.font").getSize() - 2f, 10f));
        Color hintColor = UIManager.getColor("Label.disabledForeground");
        if (hintColor == null) {
            hintColor = com.intellij.ui.JBColor.GRAY;
        }

        // Build rows that will be shown/hidden per radio selection
        JLabel defaultHint = hint("Extract index.csv from container configuration", smallFont, hintColor);

        JPanel localCsvRow = fieldRow("CSV File", textLocalProfilerCsvPath);
        JLabel localCsvHint = hint("Location: var/cache/dev/index.csv", smallFont, hintColor);
        JPanel localUrlRow = fieldRow("Base url", textLocalProfilerUrl);
        JLabel localUrlHint = hint("Overwrite base url: http://127.0.0.1:8080, http://127.0.0.1/app_dev.php", smallFont, hintColor);

        JPanel httpUrlRow = fieldRow("Base url", textHttpProfilerUrl);
        JLabel httpUrlHint = hint("Profiler url without \"_profiler\": http://127.0.0.1:8080", smallFont, hintColor);

        localComponents = new Component[]{localCsvRow, localCsvHint, localUrlRow, localUrlHint};
        httpComponents = new Component[]{httpUrlRow, httpUrlHint};

        radioDefaultProfiler.addActionListener(e -> updateVisibility());
        radioLocalProfiler.addActionListener(e -> updateVisibility());
        radioHttpProfiler.addActionListener(e -> updateVisibility());

        mainPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.weightx = 1.0; gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTHWEST; gbc.weighty = 0;
        gbc.insets = new Insets(1, 0, 1, 0);

        int row = 0;
        gbc.gridy = row++; mainPanel.add(radioDefaultProfiler, gbc);
        gbc.gridy = row++; mainPanel.add(defaultHint, gbc);
        gbc.insets = new Insets(6, 0, 1, 0);
        gbc.gridy = row++; mainPanel.add(radioLocalProfiler, gbc);
        gbc.insets = new Insets(1, 0, 1, 0);
        gbc.gridy = row++; mainPanel.add(localCsvRow, gbc);
        gbc.gridy = row++; mainPanel.add(localCsvHint, gbc);
        gbc.gridy = row++; mainPanel.add(localUrlRow, gbc);
        gbc.gridy = row++; mainPanel.add(localUrlHint, gbc);
        gbc.insets = new Insets(6, 0, 1, 0);
        gbc.gridy = row++; mainPanel.add(radioHttpProfiler, gbc);
        gbc.insets = new Insets(1, 0, 1, 0);
        gbc.gridy = row++; mainPanel.add(httpUrlRow, gbc);
        gbc.gridy = row++; mainPanel.add(httpUrlHint, gbc);

        // filler to push everything up
        gbc.gridy = row; gbc.weighty = 1.0; gbc.fill = GridBagConstraints.VERTICAL;
        mainPanel.add(Box.createVerticalGlue(), gbc);

        // initial visibility matches default radio
        setVisible(localComponents, false);
        setVisible(httpComponents, false);

        return mainPanel;
    }

    private static void setVisible(Component[] components, boolean visible) {
        for (Component c : components) c.setVisible(visible);
    }

    private static JPanel fieldRow(String labelText, JComponent field) {
        JPanel row = new JPanel(new GridBagLayout());
        row.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST; gbc.insets = new Insets(0, 20, 0, 6);
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0; gbc.fill = GridBagConstraints.NONE;
        row.add(new JLabel(labelText), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.insets = new Insets(0, 0, 0, 0);
        row.add(field, gbc);
        return row;
    }

    private static JLabel hint(String text, Font font, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(font);
        label.setForeground(color);
        label.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 0));
        return label;
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
        if (localComponents != null) updateVisibility();
    }

    private void updateDefaultRadio() {
        if (!radioLocalProfiler.isSelected() && !radioHttpProfiler.isSelected()) {
            radioDefaultProfiler.setSelected(true);
        }
    }

    private void updateVisibility() {
        setVisible(localComponents, radioLocalProfiler.isSelected());
        setVisible(httpComponents, radioHttpProfiler.isSelected());
        if (mainPanel != null) mainPanel.revalidate();
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
}
