package fr.adrienbrault.idea.symfony2plugin.dic.container;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 *
 * We are process values from index, dont allow modify
 */
public class ImmutableDecoratorService implements ServiceInterface {

    @NotNull
    private final ServiceInterface service;

    public ImmutableDecoratorService(@NotNull ServiceInterface service) {
        this.service = service;
    }

    @NotNull
    @Override
    public String getId() {
        return service.getId();
    }

    @Override
    public String getClassName() {
        return service.getClassName();
    }

    @Override
    public boolean isLazy() {
        return service.isLazy();
    }

    @Override
    public boolean isAbstract() {
        return service.isAbstract();
    }

    @Override
    public boolean isAutowire() {
        return service.isAutowire();
    }

    @Override
    public boolean isDeprecated() {
        return service.isDeprecated();
    }

    @Override
    public boolean isPublic() {
        return service.isPublic();
    }

    @Nullable
    @Override
    public String getAlias() {
        return service.getAlias();
    }

    @Nullable
    @Override
    public String getParent() {
        return service.getParent();
    }

    @Nullable
    @Override
    public String getDecorates() {
        return service.getDecorates();
    }

    @Nullable
    @Override
    public String getDecorationInnerName() {
        return service.getDecorationInnerName();
    }
}
