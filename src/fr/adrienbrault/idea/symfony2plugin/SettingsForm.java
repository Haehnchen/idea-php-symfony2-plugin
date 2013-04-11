package fr.adrienbrault.idea.symfony2plugin;

import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

/**
 * Created with IntelliJ IDEA.
 * User: Lumbendil
 * Date: 7/04/13
 * Time: 20:11
 * To change this template use File | Settings | File Templates.
 */
public class SettingsForm implements Configurable {

    private Project project;
    private TextFieldWithBrowseButton pathToProjectPanel;

    public SettingsForm(@NotNull final Project project) {
        this.project = project;
    }

    @Nls
    @Override
    public String getDisplayName() {
        return "Symfony2 Plugin";
    }

    @Nullable
    @Override
    public String getHelpTopic() {
        return null;
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        JLabel label = new JLabel("Path to appDevDebugProjectContainer.xml: ");
        pathToProjectPanel = new TextFieldWithBrowseButton(new JTextField("", 35));
        pathToProjectPanel.getButton().addMouseListener(createButtonMouseListener());

        label.setLabelFor(pathToProjectPanel);

        JPanel panel = new JPanel();

        panel.add(label);
        panel.add(pathToProjectPanel);

        updateUIFromSettings();

        return panel;
    }

    @Override
    public boolean isModified() {
        return true;
    }

    @Override
    public void apply() throws ConfigurationException {
        getSettings().pathToProjectContainer = pathToProjectPanel.getText();
    }

    @Override
    public void reset() {
        getSettings().reset();
        updateUIFromSettings();
    }

    @Override
    public void disposeUIResources() {

    }

    private Settings getSettings() {
        return Settings.getInstance(project);
    }

    private void updateUIFromSettings() {
        pathToProjectPanel.setText(getSettings().pathToProjectContainer);
    }

    private MouseListener createButtonMouseListener() {
        return new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent mouseEvent) {
            }

            @Override
            public void mousePressed(MouseEvent mouseEvent) {
                VirtualFile projectDirectory = project.getBaseDir();
                VirtualFile selectedFile = FileChooser.chooseFile(
                    FileChooserDescriptorFactory.createSingleFileNoJarsDescriptor(),
                    project,
                    VfsUtil.findRelativeFile(pathToProjectPanel.getText(), projectDirectory)
                );

                if (null == selectedFile) {
                    return; // Ignore but keep the previous path
                }

                String path = VfsUtil.getRelativePath(selectedFile, projectDirectory, '/');
                if (null == path) {
                    path = selectedFile.getPath();
                }

                pathToProjectPanel.setText(path);
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
}
