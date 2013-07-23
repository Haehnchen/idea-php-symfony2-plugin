package fr.adrienbrault.idea.symfony2plugin;

import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.fileTypes.FileTypeManager;
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

public class SettingsForm implements Configurable {

    private Project project;

    private JPanel panel1;
    private JButton pathToUrlGeneratorTextFieldReset;

    private JCheckBox symfonyContainerTypeProvider;
    private JCheckBox objectRepositoryTypeProvider;
    private JCheckBox objectRepositoryResultTypeProvider;
    private JCheckBox objectManagerFindTypeProvider;

    private TextFieldWithBrowseButton pathToUrlGeneratorTextField;
    private JLabel typesLabel;
    private JCheckBox twigAnnotateRoute;
    private JCheckBox twigAnnotateTemplate;
    private JCheckBox twigAnnotateAsset;
    private JCheckBox twigAnnotateAssetTags;
    private JCheckBox phpAnnotateTemplate;
    private JCheckBox phpAnnotateService;
    private JCheckBox phpAnnotateRoute;
    private JCheckBox phpAnnotateTemplateAnnotation;
    private JCheckBox pluginEnabled;
    private JCheckBox yamlAnnotateServiceConfig;

    private JButton directoryToWebReset;
    private JLabel directoryToWebLabel;
    private TextFieldWithBrowseButton directoryToWeb;

    private JButton directoryToAppReset;
    private JLabel directoryToAppLabel;
    private TextFieldWithBrowseButton directoryToApp;
    private JButton pathToTranslationRootTextFieldReset;
    private TextFieldWithBrowseButton pathToTranslationRootTextField;

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

    public JComponent createComponent() {

        pathToUrlGeneratorTextField.getButton().addMouseListener(createPathButtonMouseListener(pathToUrlGeneratorTextField.getTextField(), FileChooserDescriptorFactory.createSingleFileDescriptor(FileTypeManager.getInstance().getStdFileType("PHP"))));
        pathToUrlGeneratorTextFieldReset.addMouseListener(createResetPathButtonMouseListener(pathToUrlGeneratorTextField.getTextField(), Settings.DEFAULT_URL_GENERATOR_PATH));

        pathToTranslationRootTextField.getButton().addMouseListener(createPathButtonMouseListener(pathToTranslationRootTextField.getTextField(), FileChooserDescriptorFactory.createSingleFolderDescriptor()));
        pathToTranslationRootTextFieldReset.addMouseListener(createResetPathButtonMouseListener(pathToTranslationRootTextField.getTextField(), Settings.DEFAULT_TRANSLATION_PATH));

        directoryToApp.getButton().addMouseListener(createPathButtonMouseListener(directoryToApp.getTextField(), FileChooserDescriptorFactory.createSingleFolderDescriptor()));
        directoryToAppReset.addMouseListener(createResetPathButtonMouseListener(directoryToApp.getTextField(), Settings.DEFAULT_APP_DIRECTORY));

        directoryToWeb.getButton().addMouseListener(createPathButtonMouseListener(directoryToWeb.getTextField(), FileChooserDescriptorFactory.createSingleFolderDescriptor()));
        directoryToWebReset.addMouseListener(createResetPathButtonMouseListener(directoryToWeb.getTextField(), Settings.DEFAULT_WEB_DIRECTORY));

        return (JComponent) panel1;
    }

    @Override
    public boolean isModified() {
        return
            !pluginEnabled.isSelected() == getSettings().pluginEnabled
            || !pathToUrlGeneratorTextField.getText().equals(getSettings().pathToUrlGenerator)
            || !pathToTranslationRootTextField.getText().equals(getSettings().pathToTranslation)
            || !symfonyContainerTypeProvider.isSelected() == getSettings().symfonyContainerTypeProvider
            || !objectRepositoryTypeProvider.isSelected() == getSettings().objectRepositoryTypeProvider
            || !objectRepositoryResultTypeProvider.isSelected() == getSettings().objectRepositoryResultTypeProvider
            || !objectManagerFindTypeProvider.isSelected() == getSettings().objectManagerFindTypeProvider

            || !twigAnnotateRoute.isSelected() == getSettings().twigAnnotateRoute
            || !twigAnnotateTemplate.isSelected() == getSettings().twigAnnotateTemplate
            || !twigAnnotateAsset.isSelected() == getSettings().twigAnnotateAsset
            || !twigAnnotateAssetTags.isSelected() == getSettings().twigAnnotateAssetTags

            || !phpAnnotateTemplate.isSelected() == getSettings().phpAnnotateTemplate
            || !phpAnnotateService.isSelected() == getSettings().phpAnnotateService
            || !phpAnnotateRoute.isSelected() == getSettings().phpAnnotateRoute
            || !phpAnnotateTemplateAnnotation.isSelected() == getSettings().phpAnnotateTemplateAnnotation

            || !yamlAnnotateServiceConfig.isSelected() == getSettings().yamlAnnotateServiceConfig

            || !directoryToApp.getText().equals(getSettings().directoryToApp)
            || !directoryToWeb.getText().equals(getSettings().directoryToWeb)
        ;
    }

    @Override
    public void apply() throws ConfigurationException {

        getSettings().pluginEnabled = pluginEnabled.isSelected();

        getSettings().pathToUrlGenerator = pathToUrlGeneratorTextField.getText();
        getSettings().pathToTranslation = pathToTranslationRootTextField.getText();

        getSettings().symfonyContainerTypeProvider = symfonyContainerTypeProvider.isSelected();
        getSettings().objectRepositoryTypeProvider = objectRepositoryTypeProvider.isSelected();
        getSettings().objectRepositoryResultTypeProvider = objectRepositoryResultTypeProvider.isSelected();
        getSettings().objectManagerFindTypeProvider = objectManagerFindTypeProvider.isSelected();

        getSettings().twigAnnotateRoute = twigAnnotateRoute.isSelected();
        getSettings().twigAnnotateTemplate = twigAnnotateTemplate.isSelected();
        getSettings().twigAnnotateAsset = twigAnnotateAsset.isSelected();
        getSettings().twigAnnotateAssetTags = twigAnnotateAssetTags.isSelected();

        getSettings().phpAnnotateTemplate = phpAnnotateTemplate.isSelected();
        getSettings().phpAnnotateService = phpAnnotateService.isSelected();
        getSettings().phpAnnotateRoute = phpAnnotateRoute.isSelected();
        getSettings().phpAnnotateTemplateAnnotation = phpAnnotateTemplateAnnotation.isSelected();

        getSettings().yamlAnnotateServiceConfig = yamlAnnotateServiceConfig.isSelected();

        getSettings().directoryToApp = directoryToApp.getText();
        getSettings().directoryToWeb = directoryToWeb.getText();
    }

    @Override
    public void reset() {
        updateUIFromSettings();
    }

    @Override
    public void disposeUIResources() {
    }

    private Settings getSettings() {
        return Settings.getInstance(project);
    }

    private void updateUIFromSettings() {

        pluginEnabled.setSelected(getSettings().pluginEnabled);

        pathToUrlGeneratorTextField.setText(getSettings().pathToUrlGenerator);
        pathToTranslationRootTextField.setText(getSettings().pathToTranslation);

        symfonyContainerTypeProvider.setSelected(getSettings().symfonyContainerTypeProvider);
        objectRepositoryTypeProvider.setSelected(getSettings().objectRepositoryTypeProvider);
        objectRepositoryResultTypeProvider.setSelected(getSettings().objectRepositoryResultTypeProvider);
        objectManagerFindTypeProvider.setSelected(getSettings().objectManagerFindTypeProvider);

        twigAnnotateRoute.setSelected(getSettings().twigAnnotateRoute);
        twigAnnotateTemplate.setSelected(getSettings().twigAnnotateTemplate);
        twigAnnotateAsset.setSelected(getSettings().twigAnnotateAsset);
        twigAnnotateAssetTags.setSelected(getSettings().twigAnnotateAssetTags);

        phpAnnotateTemplate.setSelected(getSettings().phpAnnotateTemplate);
        phpAnnotateService.setSelected(getSettings().phpAnnotateService);
        phpAnnotateRoute.setSelected(getSettings().phpAnnotateRoute);
        phpAnnotateTemplateAnnotation.setSelected(getSettings().phpAnnotateTemplateAnnotation);

        yamlAnnotateServiceConfig.setSelected(getSettings().yamlAnnotateServiceConfig);

        directoryToApp.setText(getSettings().directoryToApp);
        directoryToWeb.setText(getSettings().directoryToWeb);
    }

    private MouseListener createPathButtonMouseListener(final JTextField textField) {
        return createPathButtonMouseListener(textField, FileChooserDescriptorFactory.createSingleFileNoJarsDescriptor());
    }

    private MouseListener createPathButtonMouseListener(final JTextField textField, final FileChooserDescriptor fileChooserDescriptor) {
        return new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent mouseEvent) {
            }

            @Override
            public void mousePressed(MouseEvent mouseEvent) {
                VirtualFile projectDirectory = project.getBaseDir();
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
