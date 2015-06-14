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

    public SymfonyInstallerSettings(@NotNull SymfonyInstallerVersion version, @NotNull String phpInterpreter) {
        this.version = version;
        this.phpInterpreter = phpInterpreter;
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

}
