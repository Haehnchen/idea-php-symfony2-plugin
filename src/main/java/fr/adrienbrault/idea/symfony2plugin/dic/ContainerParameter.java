package fr.adrienbrault.idea.symfony2plugin.dic;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ContainerParameter {
    @NotNull
    final private String name;
    @Nullable
    final private String value;
    final private boolean isWeak;

    @Deprecated
    public ContainerParameter(@NotNull String name, @Nullable String value) {
        this(name, value, false);
    }

    public ContainerParameter(@NotNull String name, @Nullable String value, boolean isWeak) {
        this.name = name;
        this.value = value;
        this.isWeak = isWeak;
    }

    public ContainerParameter(@NotNull String name, boolean isWeak) {
        this.name = name;
        this.isWeak = isWeak;
        this.value = null;
    }

    public ContainerParameter(@NotNull String name, @NotNull Collection<String> values, boolean isWeak) {
        this.name = name;
        this.value = values.isEmpty() ? null : values.iterator().next();
        this.isWeak = isWeak;
    }

    @NotNull
    public String getName() {
        return name;
    }

    @Nullable
    public String getValue() {
        return value;
    }

    public boolean isWeak() {
        return isWeak;
    }
}

