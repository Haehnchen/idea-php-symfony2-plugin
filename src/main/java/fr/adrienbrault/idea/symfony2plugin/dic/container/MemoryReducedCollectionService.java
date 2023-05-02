package fr.adrienbrault.idea.symfony2plugin.dic.container;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class MemoryReducedCollectionService implements ServiceInterface {
    final private String id;

    @Nullable
    final private String className;

    private final boolean lazy;
    private final boolean isAbstract;
    private final boolean autowire;
    private final Collection<String> tags;
    private final Collection<String> exclude;
    private final Collection<String> resource;

    @Nullable
    private final String decorationInnerName;
    @Nullable
    private final String decorates;
    @Nullable
    private final String parent;
    @Nullable
    private final String alias;

    private final boolean isPublic;
    private final boolean deprecated;

    public MemoryReducedCollectionService(@NotNull ServiceInterface serviceInterface) {
        this.id = serviceInterface.getId();
        this.className = serviceInterface.getClassName();
        this.lazy = serviceInterface.isLazy();
        this.isAbstract = serviceInterface.isAbstract();
        this.autowire = serviceInterface.isAutowire();
        this.decorationInnerName = serviceInterface.getDecorationInnerName();
        this.decorates = serviceInterface.getDecorates();
        this.parent = serviceInterface.getParent();
        this.alias = serviceInterface.getAlias();
        this.isPublic = serviceInterface.isPublic();
        this.deprecated = serviceInterface.isDeprecated();

        Collection<String> tags1 = serviceInterface.getTags();
        this.tags = tags1.isEmpty() ? Collections.emptyList() : Collections.unmodifiableCollection(tags1);

        Collection<String> exclude1 = serviceInterface.getExclude();
        this.exclude = exclude1.isEmpty() ? Collections.emptyList() : Collections.unmodifiableCollection(exclude1);

        Collection<String> resource1 = serviceInterface.getResource();
        this.resource = resource1.isEmpty() ? Collections.emptyList() : Collections.unmodifiableCollection(resource1);
    }

    @Override
    public @NotNull String getId() {
        return this.id;
    }

    @Override
    public @Nullable String getClassName() {
        return this.className;
    }

    @Override
    public boolean isLazy() {
        return this.lazy;
    }

    @Override
    public boolean isAbstract() {
        return this.isAbstract;
    }

    @Override
    public boolean isAutowire() {
        return this.autowire;
    }

    @Override
    public boolean isDeprecated() {
        return this.deprecated;
    }

    @Override
    public boolean isPublic() {
        return this.isPublic;
    }

    @Override
    public @Nullable String getAlias() {
        return this.alias;
    }

    @Override
    public @Nullable String getParent() {
        return this.parent;
    }

    @Override
    public @Nullable String getDecorates() {
        return this.decorates;
    }

    @Override
    public @Nullable String getDecorationInnerName() {
        return this.decorationInnerName;
    }

    @Override
    public @NotNull Collection<String> getResource() {
        return this.resource;
    }

    @Override
    public @NotNull Collection<String> getExclude() {
        return this.exclude;
    }

    @Override
    public @NotNull Collection<String> getTags() {
        return this.tags;
    }
}
