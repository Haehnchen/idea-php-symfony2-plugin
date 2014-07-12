package fr.adrienbrault.idea.symfony2plugin.dic;


import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;

public class ContainerParameter {

    final private String name;
    final private boolean isWeak;
    final private Collection<String> values;

    @Deprecated
    public ContainerParameter(String name, @Nullable String value) {
        this(name, value, false);
    }

    public ContainerParameter(String name, @Nullable String value, boolean isWeak) {
        this(name, Arrays.asList(value), isWeak);
    }

    public ContainerParameter(String name, Collection<String> values, boolean isWeak) {
        this.name = name;
        this.values = values;
        this.isWeak = isWeak;
    }

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

