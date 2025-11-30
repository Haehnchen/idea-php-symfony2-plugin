package fr.adrienbrault.idea.symfony2plugin.util.dict;

import org.jetbrains.annotations.NotNull;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class SymfonyCommand {
    @NotNull
    private final String name;

    @NotNull
    private final String fqn;

    /**
     * @param fqn starting with "\"
     */
    public SymfonyCommand(@NotNull String name, @NotNull String fqn) {
        if (!fqn.startsWith("\\")) {
            throw new IllegalArgumentException("Invalid fqn: " + fqn);
        }

        this.name = name;
        this.fqn = fqn;
    }
    
    @NotNull
    public String getName() {
        return name;
    }

    public @NotNull String getFqn() {
        return fqn;
    }
}
