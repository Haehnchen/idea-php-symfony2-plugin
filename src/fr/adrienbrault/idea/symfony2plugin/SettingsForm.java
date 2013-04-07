package fr.adrienbrault.idea.symfony2plugin;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * Created with IntelliJ IDEA.
 * User: Lumbendil
 * Date: 7/04/13
 * Time: 20:11
 * To change this template use File | Settings | File Templates.
 */
public class SettingsForm implements Configurable {
    private Project project;
    private JTextField pathToProjectPanel;

    public SettingsForm(@NotNull final Project project)
    {
        this.project = project;
    }

    @Nls
    @Override
    public String getDisplayName() {
        return "Symfony2";
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
        pathToProjectPanel = new JTextField(getSettings().pathToProjectContainer, 20);

        label.setLabelFor(pathToProjectPanel);

        JPanel panel = new JPanel();

        panel.add(label);
        panel.add(pathToProjectPanel);

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
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void disposeUIResources() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    private Settings getSettings()
    {
        return Settings.getInstance(project);
    }
}
