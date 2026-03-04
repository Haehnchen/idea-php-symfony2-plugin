package fr.adrienbrault.idea.symfony2plugin.dic.command;

import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.components.fields.ExtendableTextField;
import com.intellij.util.ui.FormBuilder;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.Objects;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class SymfonyCommandRunConfigurationEditor extends SettingsEditor<SymfonyCommandRunConfiguration> {

    private final JPanel myPanel;
    private final ComboBox<SymfonyCommandRunConfiguration.ExecutionMode> myExecutionModeCombo;
    private final JTextField myCommandNameField;
    private final JTextField myConsolePathField;
    private final JTextField myInterpreterPathField;
    private final JTextField mySymfonyCliPathField;
    private final JTextField myWorkingDirectoryField;
    private final JTextField myCommandLineParametersField;

    private final JLabel myConsolePathLabel;
    private final JLabel myInterpreterPathLabel;
    private final JLabel mySymfonyCliPathLabel;

    public SymfonyCommandRunConfigurationEditor() {
        myExecutionModeCombo = new ComboBox<>(SymfonyCommandRunConfiguration.ExecutionMode.values());
        myCommandNameField = new ExtendableTextField();
        myConsolePathField = new ExtendableTextField();
        myInterpreterPathField = new ExtendableTextField();
        mySymfonyCliPathField = new ExtendableTextField();
        myWorkingDirectoryField = new ExtendableTextField();
        myCommandLineParametersField = new ExtendableTextField();

        myConsolePathLabel = new JLabel("Console path:");
        myInterpreterPathLabel = new JLabel("PHP interpreter:");
        mySymfonyCliPathLabel = new JLabel("Symfony CLI path:");

        myPanel = FormBuilder.createFormBuilder()
            .addLabeledComponent("Execution mode:", myExecutionModeCombo)
            .addLabeledComponent("Command:", myCommandNameField)
            .addLabeledComponent(myConsolePathLabel, myConsolePathField)
            .addLabeledComponent(myInterpreterPathLabel, myInterpreterPathField)
            .addLabeledComponent(mySymfonyCliPathLabel, mySymfonyCliPathField)
            .addLabeledComponent("Working directory:", myWorkingDirectoryField)
            .addLabeledComponent("Additional parameters:", myCommandLineParametersField)
            .addComponentFillVertically(new JPanel(), 0)
            .getPanel();

        Dimension size = new Dimension(300, myCommandNameField.getPreferredSize().height);
        myCommandNameField.setPreferredSize(size);
        myConsolePathField.setPreferredSize(size);
        myInterpreterPathField.setPreferredSize(size);
        mySymfonyCliPathField.setPreferredSize(size);
        myWorkingDirectoryField.setPreferredSize(size);
        myCommandLineParametersField.setPreferredSize(size);

        myExecutionModeCombo.addActionListener(e -> updateFieldVisibility());
    }

    private void updateFieldVisibility() {
        boolean isSymfonyCli = myExecutionModeCombo.getSelectedItem() == SymfonyCommandRunConfiguration.ExecutionMode.SYMFONY_CLI;

        myConsolePathLabel.setVisible(!isSymfonyCli);
        myConsolePathField.setVisible(!isSymfonyCli);
        myInterpreterPathLabel.setVisible(!isSymfonyCli);
        myInterpreterPathField.setVisible(!isSymfonyCli);
        mySymfonyCliPathLabel.setVisible(isSymfonyCli);
        mySymfonyCliPathField.setVisible(isSymfonyCli);
    }

    @Override
    protected void resetEditorFrom(@NotNull SymfonyCommandRunConfiguration configuration) {
        myExecutionModeCombo.setSelectedItem(configuration.getExecutionMode());
        myCommandNameField.setText(nullToEmpty(configuration.getCommandName()));
        myConsolePathField.setText(nullToDefault(configuration.getConsolePath(), "bin/console"));
        myInterpreterPathField.setText(nullToDefault(configuration.getInterpreterPath(), "php"));
        mySymfonyCliPathField.setText(nullToDefault(configuration.getSymfonyCliPath(), "symfony"));
        myWorkingDirectoryField.setText(nullToEmpty(configuration.getWorkingDirectory()));
        myCommandLineParametersField.setText(nullToEmpty(configuration.getCommandLineParameters()));
        updateFieldVisibility();
    }

    @Override
    protected void applyEditorTo(@NotNull SymfonyCommandRunConfiguration configuration) {
        configuration.setExecutionMode((SymfonyCommandRunConfiguration.ExecutionMode) Objects.requireNonNull(myExecutionModeCombo.getSelectedItem()));
        configuration.setCommandName(myCommandNameField.getText().trim());
        configuration.setConsolePath(myConsolePathField.getText().trim());
        configuration.setInterpreterPath(myInterpreterPathField.getText().trim());
        configuration.setSymfonyCliPath(mySymfonyCliPathField.getText().trim());
        configuration.setWorkingDirectory(myWorkingDirectoryField.getText().trim());
        configuration.setCommandLineParameters(myCommandLineParametersField.getText().trim());
    }

    @NotNull
    @Override
    protected JComponent createEditor() {
        return myPanel;
    }

    private static String nullToEmpty(String value) {
        return value != null ? value : "";
    }

    private static String nullToDefault(String value, String defaultValue) {
        return value != null && !value.isEmpty() ? value : defaultValue;
    }
}
