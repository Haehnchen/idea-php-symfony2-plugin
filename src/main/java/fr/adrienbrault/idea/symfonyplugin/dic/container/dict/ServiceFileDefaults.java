package fr.adrienbrault.idea.symfonyplugin.dic.container.dict;

import org.jetbrains.annotations.Nullable;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ServiceFileDefaults {

    public static ServiceFileDefaults EMPTY = new ServiceFileDefaults();

    @Nullable
    final private Boolean isPublic;

    @Nullable
    final private Boolean isAutowire;

    private ServiceFileDefaults() {
        isPublic = null;
        isAutowire = null;
    }

    public ServiceFileDefaults(@Nullable Boolean isPublic, @Nullable Boolean isAutowire) {
        this.isPublic = isPublic;
        this.isAutowire = isAutowire;
    }

    @Nullable
    public Boolean isPublic() {
        return isPublic;
    }

    @Nullable
    public Boolean isAutowire() {
        return isAutowire;
    }
}
