package fr.adrienbrault.idea.symfonyplugin.installer.dict;

import org.jetbrains.annotations.NotNull;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class SymfonyInstallerVersion {

    private String version;
    private String presentableName;

    public SymfonyInstallerVersion(@NotNull String version) {
        this.version = version;
        this.presentableName = version;
    }

    public SymfonyInstallerVersion(@NotNull String version, @NotNull String presentableName) {
        this.version = version;
        this.presentableName = presentableName;
    }

    public String getPresentableName() {
        return presentableName;
    }

    public String getVersion() {
        return version;
    }

}
