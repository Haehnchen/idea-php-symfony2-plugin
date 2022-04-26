package fr.adrienbrault.idea.symfony2plugin.installer;

import fr.adrienbrault.idea.symfony2plugin.installer.dict.SymfonyInstallerVersion;
import org.jetbrains.annotations.NotNull;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class SymfonyInstallerSettings {
    @NotNull
    private final SymfonyInstallerVersion version;

    private final boolean isDownloadInstallerSelected;

    @NotNull
    private final String projectType;

    public SymfonyInstallerSettings(@NotNull SymfonyInstallerVersion version, boolean isDownloadInstallerSelected, @NotNull String projectType) {
        this.version = version;
        this.isDownloadInstallerSelected = isDownloadInstallerSelected;
        this.projectType = projectType;
    }

    @NotNull
    public SymfonyInstallerVersion getVersion() {
        return version;
    }

    @NotNull
    public String getProjectType() {
        return projectType;
    }

    public boolean isDownloadInstallerSelected() {
        return isDownloadInstallerSelected;
    }
}
