package fr.adrienbrault.idea.symfony2plugin.dic.container.dict;

import org.jetbrains.annotations.Nullable;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ServiceFileDefaults {

    public static final ServiceFileDefaults EMPTY = new ServiceFileDefaults();

    @Nullable
    final private Boolean isPublic;

    @Nullable
    final private Boolean isAutowire;

    @Nullable
    final private Boolean isAutoconfigure;

    private ServiceFileDefaults() {
        isPublic = null;
        isAutowire = null;
        isAutoconfigure = null;
    }

    public ServiceFileDefaults(@Nullable Boolean isPublic, @Nullable Boolean isAutowire, @Nullable Boolean isAutoconfigure) {
        this.isPublic = isPublic;
        this.isAutowire = isAutowire;
        this.isAutoconfigure = isAutoconfigure;
    }

    @Nullable
    public Boolean isPublic() {
        return isPublic;
    }

    @Nullable
    public Boolean isAutowire() {
        return isAutowire;
    }

    @Nullable
    public Boolean isAutoconfigure() {
        return isAutoconfigure;
    }
}
