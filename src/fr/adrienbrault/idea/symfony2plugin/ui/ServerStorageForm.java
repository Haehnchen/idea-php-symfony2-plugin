package fr.adrienbrault.idea.symfony2plugin.ui;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ApplicationSettings;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class ServerStorageForm implements Configurable {

    private JCheckBox checkBoxEnabledServer;
    private JTextField textFieldPort;
    private JCheckBox listenOnAllIPsCheckBox;
    private JPanel panel1;

    @Nls
    @Override
    public String getDisplayName() {
        return "ServerStorage";
    }

    @Nullable
    @Override
    public String getHelpTopic() {
        return null;
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        return (JComponent) panel1;
    }

    @Override
    public boolean isModified() {
        return
            getAppSettings().serverEnabled != checkBoxEnabledServer.isSelected()
            || getAppSettings().listenAll != listenOnAllIPsCheckBox.isSelected()
            || getAppSettings().serverPort != Integer.parseInt(textFieldPort.getText())
            ;
    }

    @Override
    public void apply() throws ConfigurationException {
        getAppSettings().serverEnabled = checkBoxEnabledServer.isSelected();
        getAppSettings().listenAll = listenOnAllIPsCheckBox.isSelected();
        getAppSettings().serverPort = Integer.parseInt(textFieldPort.getText());
    }

    @Override
    public void reset() {
        updateUIFromSettings();
    }

    private void updateUIFromSettings() {
        checkBoxEnabledServer.setSelected(getAppSettings().serverEnabled);
        listenOnAllIPsCheckBox.setSelected(getAppSettings().listenAll);
        textFieldPort.setText(String.valueOf(getAppSettings().serverPort));
    }

    @Override
    public void disposeUIResources() {

    }

    public Symfony2ApplicationSettings getAppSettings() {
        return ServiceManager.getService(Symfony2ApplicationSettings.class);
    }


}
