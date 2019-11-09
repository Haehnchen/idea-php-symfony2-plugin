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
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

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
        if(!radioLocalProfiler.isSelected() && !radioHttpProfiler.isSelected()) {
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
                VirtualFile projectDirectory = project.getBaseDir();

                String text = textField.getText();
                VirtualFile toSelect = VfsUtil.findRelativeFile(text, projectDirectory);
                if(toSelect == null) {
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
