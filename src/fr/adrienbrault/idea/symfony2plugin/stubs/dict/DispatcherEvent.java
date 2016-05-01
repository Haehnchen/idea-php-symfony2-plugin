package fr.adrienbrault.idea.symfony2plugin.stubs.dict;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class DispatcherEvent implements Serializable {

    @Nullable
    private String fqn;

    @Nullable
    private String instance;

    public DispatcherEvent() {
    }

    public DispatcherEvent(@NotNull String fqn, @Nullable String instance) {
        this.fqn = fqn;
        this.instance = instance;
    }

    @Nullable
    public String getFqn() {
        return fqn;
    }

    @Nullable
    public String getInstance() {
        return instance;
    }
}
