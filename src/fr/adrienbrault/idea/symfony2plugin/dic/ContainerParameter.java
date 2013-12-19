package fr.adrienbrault.idea.symfony2plugin.dic;


import org.jetbrains.annotations.Nullable;

public class ContainerParameter {

    private String name;
    private String value;
    private boolean isWeak;

    public ContainerParameter(String name, @Nullable String value) {
        this(name, value, false);
    }

    public ContainerParameter(String name, @Nullable String value, boolean isWeak) {
        this.name = name;
        this.value = value;
        this.isWeak = isWeak;
    }

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

