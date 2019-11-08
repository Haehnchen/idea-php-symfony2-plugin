package fr.adrienbrault.idea.symfonyplugin.ui.utils.dict;

import com.jetbrains.plugins.webDeployment.config.WebServerConfig;
import fr.adrienbrault.idea.symfonyplugin.ui.utils.UiSettingsUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public abstract class WebServerFileDialogExtensionCallback implements UiSettingsUtil.WebServerFileDialogCallback {

    final private String fileExtension;

    public WebServerFileDialogExtensionCallback(@NotNull String fileExtension) {
        this.fileExtension = fileExtension;
    }

    @Override
    public void noDefaultServer() {
        JOptionPane.showMessageDialog(null, "No default server given. Set one in Tools -> Deployment -> Configuration", "Error", JOptionPane.ERROR_MESSAGE);
    }

    @Override
    public boolean validate(@NotNull WebServerConfig server, @NotNull WebServerConfig.RemotePath remotePath) {
        return remotePath.path.toLowerCase().endsWith(this.fileExtension.toLowerCase());
    }
}
