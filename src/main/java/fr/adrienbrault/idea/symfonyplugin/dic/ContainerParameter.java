package fr.adrienbrault.idea.symfonyplugin.dic;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ContainerParameter {

    final private String name;
    final private boolean isWeak;
    final private Collection<String> values;

    @Deprecated
    public ContainerParameter(@NotNull String name, @Nullable String value) {
        this(name, value, false);
    }

    public ContainerParameter(@NotNull String name, boolean isWeak) {
        this(name, Collections.emptyList(), isWeak);
    }

    public ContainerParameter(@NotNull String name, @Nullable String value, boolean isWeak) {
        this(name, Collections.singletonList(value), isWeak);
    }

    public ContainerParameter(@NotNull String name, Collection<String> values, boolean isWeak) {
        this.name = name;
        this.values = values;
        this.isWeak = isWeak;
    }

    @NotNull
    public String getName() {
        return name;
    }

    @Nullable
    public String getValue() {

        if(values.size() > 0) {
            return values.iterator().next();
        }

        return null;
    }

    public boolean isWeak() {
        return isWeak;
    }

    public Collection<String> getValues() {
        return values;
    }

}

