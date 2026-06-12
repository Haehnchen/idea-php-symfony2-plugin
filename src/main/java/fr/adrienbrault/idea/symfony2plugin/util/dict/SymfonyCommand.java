package fr.adrienbrault.idea.symfony2plugin.util.dict;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class SymfonyCommand {
    @NotNull
    private final String name;

    @NotNull
    private final String fqn;

    @Nullable
    private final String methodName;

    /**
     * @param fqn starting with "\"
     */
    public SymfonyCommand(@NotNull String name, @NotNull String fqn) {
        this(name, fqn, null);
    }

    /**
     * @param fqn starting with "\"
     */
    public SymfonyCommand(@NotNull String name, @NotNull String fqn, @Nullable String methodName) {
        if (!fqn.startsWith("\\")) {
            throw new IllegalArgumentException("Invalid fqn: " + fqn);
        }

        this.name = name;
        this.fqn = fqn;
        this.methodName = methodName;
    }
    
    @NotNull
    public String getName() {
        return name;
    }

    public @NotNull String getFqn() {
        return fqn;
    }

    @Nullable
    public String getMethodName() {
        return methodName;
    }

    public boolean isMethodCommand() {
        return methodName != null && !methodName.isBlank();
    }
}
