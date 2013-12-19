package fr.adrienbrault.idea.symfony2plugin.dic;

import org.jetbrains.annotations.Nullable;

public class ContainerService {

    private String name;
    private String className;
    private boolean isPrivate = false;
    private boolean isWeak = false;

    public ContainerService(String name, String className) {
        this.name = name;
        this.className = className;
    }

    public ContainerService(String name, String className, boolean isWeak) {
        this(name, className);
        this.isWeak = isWeak;
    }

    public ContainerService(String name, String className, boolean isWeak, boolean isPrivate) {
        this(name, className, isWeak);
        this.isPrivate = isPrivate;
    }

    /**
     *
     * @return can be null, class, or a parameter name
     */
    @Nullable
    public String getClassName() {
        return className;
    }

    public boolean isWeak() {
        return isWeak;
    }

    public boolean isPrivate() {
        return isPrivate;
    }

    public String getName() {
        return name;
    }

}

