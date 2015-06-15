package fr.adrienbrault.idea.symfony2plugin.installer;

import com.intellij.ide.util.projectWizard.SettingsStep;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.platform.WebProjectGenerator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class SymfonyInstallerGeneratorPeer implements WebProjectGenerator.GeneratorPeer<SymfonyInstallerSettings> {

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
            symfonyInstallerForm.getVersion(),
            symfonyInstallerForm.getInterpreter()
        );
    }

    @Nullable
    @Override
    public ValidationInfo validate() {
        return symfonyInstallerForm.validate();
    }

    @Override
    public boolean isBackgroundJobRunning() {
        return false;
    }

    @Override
    public void addSettingsStateListener(@NotNull WebProjectGenerator.SettingsStateListener settingsStateListener) {

    }
}
