package fr.adrienbrault.idea.symfony2plugin.stubs.dict;

import org.jetbrains.annotations.Nullable;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class FileResource {

    private final String resource;
    private String prefix = null;

    public FileResource(@Nullable String resource) {
        this.resource = resource;
    }

    @Nullable
    public String getResource() {
        return resource;
    }

    public FileResource setPrefix(@Nullable String prefix) {
        this.prefix = prefix;
        return this;
    }

    public String getPrefix() {
        return prefix;
    }
}
