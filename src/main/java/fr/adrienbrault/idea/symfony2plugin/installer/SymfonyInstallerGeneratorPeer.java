package fr.adrienbrault.idea.symfony2plugin.installer;

import com.intellij.ide.util.projectWizard.SettingsStep;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.platform.ProjectGeneratorPeer;
import fr.adrienbrault.idea.symfony2plugin.installer.dict.SymfonyInstallerVersion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class SymfonyInstallerGeneratorPeer implements ProjectGeneratorPeer<SymfonyInstallerSettings> {

    private SymfonyInstallerForm symfonyInstallerForm;

    public SymfonyInstallerGeneratorPeer() {
        symfonyInstallerForm = new SymfonyInstallerForm();
    }

    @NotNull
    @Override
    public JComponent getComponent() {
        return symfonyInstallerForm.getContentPane();
    }

    @Override
    public void buildUI(@NotNull SettingsStep settingsStep) {
        settingsStep.addSettingsComponent(symfonyInstallerForm.getContentPane());
    }

    @NotNull
    @Override
    public SymfonyInstallerSettings getSettings() {
        return new SymfonyInstallerSettings(
            new SymfonyInstallerVersion("unknown", symfonyInstallerForm.getProjectType()), // apt-get does not work with this value
            symfonyInstallerForm.isDownloadInstallerSelected(),
            symfonyInstallerForm.getProjectType()
        );
    }

    @Nullable
    @Override
    public ValidationInfo validate() {
        Boolean installerExists = false;
        try {
            installerExists = ProgressManager.getInstance().run(new Task.WithResult<Boolean, Exception>(null, "Checking", false) {
                @Override
                protected Boolean compute(@NotNull ProgressIndicator indicator) {
                    return SymfonyInstallerUtil.isValidSymfonyCliToolsCommandInPath();
                }
            });
        } catch (Exception ignored) {
        }

        if (installerExists) {
            return null;
        }

        return new ValidationInfo("Symfony CLI not found please install: <a href=\"https://symfony.com/download\">https://symfony.com/download</a>");
    }

    @Override
    public boolean isBackgroundJobRunning() {
        return false;
    }
}
