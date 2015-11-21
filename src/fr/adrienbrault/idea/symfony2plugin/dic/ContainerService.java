package fr.adrienbrault.idea.symfony2plugin.dic;

import fr.adrienbrault.idea.symfony2plugin.dic.container.ServiceInterface;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ContainerService {

    @Nullable
    private ServiceInterface service;
    private String name;
    private String className;
    private boolean isPrivate = false;
    private boolean isWeak = false;

    public ContainerService(@NotNull ServiceInterface service, @Nullable String classResolved) {
        this.service = service;
        this.name = service.getId();
        this.className = classResolved != null ? classResolved : service.getClassName();
        this.isPrivate = !service.isPublic();
        this.isWeak = true;
    }

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

    /**
     * legacy support
     */
    @Nullable
    public ServiceInterface getService() {
        return service;
    }
}

