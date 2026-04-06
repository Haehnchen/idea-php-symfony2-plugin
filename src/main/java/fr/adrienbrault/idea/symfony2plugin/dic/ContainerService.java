package fr.adrienbrault.idea.symfony2plugin.dic;

import fr.adrienbrault.idea.symfony2plugin.dic.container.MemoryReducedCollectionService;
import fr.adrienbrault.idea.symfony2plugin.dic.container.ServiceInterface;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ContainerService {

    @Nullable
    private ServiceInterface service;

    final private String name;

    final private String className;
    private boolean isPrivate = false;
    private boolean isWeak = false;
    private final Set<String> classVariants = new HashSet<>();
    private final Set<ContainerServiceMetadata> metadata = new LinkedHashSet<>();
    @Nullable
    private Set<String> cachedClassNames;
    @Nullable
    private Boolean cachedMetadataAutowire;

    public ContainerService(@NotNull ServiceInterface service, @Nullable String classResolved) {
        this.service = new MemoryReducedCollectionService(service);
        this.name = service.getId();
        this.className = classResolved != null ? classResolved : service.getClassName();
        this.isPrivate = !service.isPublic();
        this.isWeak = true;
    }

    public ContainerService(@NotNull String name, @Nullable String className) {
        this.name = name;
        this.className = className;
    }

    public ContainerService(@NotNull String name, @Nullable String className, boolean isWeak) {
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
        this.cachedClassNames = null;
    }

    public void addMetadata(@NotNull ContainerServiceMetadata metadata) {
        if (this.metadata.add(metadata)) {
            this.cachedMetadataAutowire = null;
        }
    }

    public void addMetadata(@NotNull Iterable<ContainerServiceMetadata> metadata) {
        for (ContainerServiceMetadata containerServiceMetadata : metadata) {
            addMetadata(containerServiceMetadata);
        }
    }

    @NotNull
    public Set<String> getClassNames() {
        if (cachedClassNames != null) {
            return cachedClassNames;
        }

        Set<String> variants = new HashSet<>();

        if(className != null) {
            variants.add(className);
        }

        variants.addAll(classVariants);

        return cachedClassNames = Collections.unmodifiableSet(variants);
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

    @NotNull
    public List<ContainerServiceMetadata> getMetadata() {
        return List.copyOf(metadata);
    }

    public boolean isAutowireEnabled() {
        if (service != null && service.isAutowire()) {
            return true;
        }

        if (cachedMetadataAutowire != null) {
            return cachedMetadataAutowire;
        }

        for (ContainerServiceMetadata containerServiceMetadata : metadata) {
            if (containerServiceMetadata.autowire()) {
                return cachedMetadataAutowire = true;
            }
        }

        return cachedMetadataAutowire = false;
    }

    /**
     * legacy support
     */
    @Nullable
    public ServiceInterface getService() {
        return service;
    }
}
