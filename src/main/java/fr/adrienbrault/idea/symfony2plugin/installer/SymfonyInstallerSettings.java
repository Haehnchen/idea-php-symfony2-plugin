package fr.adrienbrault.idea.symfony2plugin.installer;

import fr.adrienbrault.idea.symfony2plugin.installer.dict.SymfonyInstallerVersion;
import org.jetbrains.annotations.NotNull;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class SymfonyInstallerSettings {
    @NotNull
    private final SymfonyInstallerVersion version;

    @NotNull
    private final String phpInterpreter;

    @NotNull
    private final String projectType;

    public SymfonyInstallerSettings(@NotNull SymfonyInstallerVersion version, @NotNull String phpInterpreter, @NotNull String projectType) {
        this.version = version;
        this.phpInterpreter = phpInterpreter;
        this.projectType = projectType;
    }

    public boolean isDownload() {
        return true;
    }

    @NotNull
    public SymfonyInstallerVersion getVersion() {
        return version;
    }

    @NotNull
    public String getPhpInterpreter() {
        return phpInterpreter;
    }

    public String getExistingPath() {
        // @TODO: implement
        return "";
    }

    @NotNull
    public String getProjectType() {
        return projectType;
    }
}
