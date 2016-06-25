package fr.adrienbrault.idea.symfony2plugin.dic;

import fr.adrienbrault.idea.symfony2plugin.dic.container.ServiceInterface;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

public class ContainerService {

    @Nullable
    private ServiceInterface service;

    @NotNull
    private String name;

    @Nullable
    private String className;
    private boolean isPrivate = false;
    private boolean isWeak = false;
    private Set<String> classVariants = new HashSet<>();

    public ContainerService(@NotNull ServiceInterface service, @Nullable String classResolved) {
        this.service = service;
        this.name = service.getId();
        this.className = classResolved != null ? classResolved : service.getClassName();
        this.isPrivate = !service.isPublic();
        this.isWeak = true;
    }

    public ContainerService(@NotNull String name, @Nullable String className) {
        this.name = name;
        this.className = className;
    }

    public ContainerService(@NotNull String name, String className, boolean isWeak) {
        this(name, className);
        this.isWeak = isWeak;
    }

    public ContainerService(@NotNull String name, @Nullable String className, boolean isWeak, boolean isPrivate) {
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

    public void addClassName(@NotNull String className) {
        this.classVariants.add(className);
    }

    @NotNull
    public Set<String> getClassNames() {
        Set<String> variants = new HashSet<>();

        if(className != null) {
            variants.add(className);
        }

        variants.addAll(classVariants);

        return variants;
    }

    public boolean isWeak() {
        return isWeak;
    }

    public boolean isPrivate() {
        return isPrivate;
    }

    @NotNull
    public String getName() {
        return name;
    }

    /**
     * legacy support
     */
    @Nullable
    public ServiceInterface getService() {
        return service;
    }
}

