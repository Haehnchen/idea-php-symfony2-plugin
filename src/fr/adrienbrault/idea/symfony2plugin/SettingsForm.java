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
import java.awt.*;
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
    private TextFieldWithBrowseButton pathToContainerTextField;
    private TextFieldWithBrowseButton pathToUrlGeneratorTextField;

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
        JLabel label;
        JButton resetPathButton;
        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.WEST;

        label = new JLabel("Path to container.xml: ");
        pathToContainerTextField = new TextFieldWithBrowseButton(new JTextField(""));
        pathToContainerTextField.getButton().addMouseListener(createPathButtonMouseListener(pathToContainerTextField.getTextField()));

        label.setLabelFor(pathToContainerTextField);

        resetPathButton = new JButton("Default");
        resetPathButton.addMouseListener(createResetPathButtonMouseListener(pathToContainerTextField.getTextField(), Settings.DEFAULT_CONTAINER_PATH));

        c.gridy = 0;
        c.gridx = 0;
        panel.add(label, c);
        c.gridx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        panel.add(pathToContainerTextField, c);
        c.gridx = 2;
        c.fill = 0;
        panel.add(resetPathButton, c);

        label = new JLabel("Path to urlGenerator.php: ");
        pathToUrlGeneratorTextField = new TextFieldWithBrowseButton(new JTextField(""));
        pathToUrlGeneratorTextField.getButton().addMouseListener(createPathButtonMouseListener(pathToUrlGeneratorTextField.getTextField()));

        resetPathButton = new JButton("Default");
        resetPathButton.addMouseListener(createResetPathButtonMouseListener(pathToUrlGeneratorTextField.getTextField(), Settings.DEFAULT_URL_GENERATOR_PATH));

        c.gridy = 1;
        c.gridx = 0;
        panel.add(label, c);
        c.gridx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        panel.add(pathToUrlGeneratorTextField, c);
        c.gridx = 2;
        c.fill = 0;
        panel.add(resetPathButton, c);

        updateUIFromSettings();

        JPanel container = new JPanel();
        container.setLayout(new BorderLayout());
        container.add(panel, BorderLayout.WEST);

        JPanel container2 = new JPanel();
        container2.setLayout(new BorderLayout());
        container2.add(container, BorderLayout.NORTH);

        return container2;
    }

    @Override
    public boolean isModified() {
        return
            !pathToContainerTextField.getText().equals(getSettings().pathToProjectContainer)
            || !pathToUrlGeneratorTextField.getText().equals(getSettings().pathToUrlGenerator)
        ;
    }

    @Override
    public void apply() throws ConfigurationException {
        getSettings().pathToProjectContainer = pathToContainerTextField.getText();
        getSettings().pathToUrlGenerator = pathToUrlGeneratorTextField.getText();
    }

    @Override
    public void reset() {
        updateUIFromSettings();
    }

    @Override
    public void disposeUIResources() {
        pathToContainerTextField = null;
        pathToUrlGeneratorTextField = null;
    }

    private Settings getSettings() {
        return Settings.getInstance(project);
    }

    private void updateUIFromSettings() {
        pathToContainerTextField.setText(getSettings().pathToProjectContainer);
        pathToUrlGeneratorTextField.setText(getSettings().pathToUrlGenerator);
    }

    private MouseListener createPathButtonMouseListener(final JTextField textField) {
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
}
