package fr.adrienbrault.idea.symfonyplugin.templating.path.dict;

import org.jetbrains.annotations.Nullable;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TwigPathJson {

    @Nullable
    private String path;

    @Nullable
    private String namespace;

    @Nullable
    private String type;

    @Nullable
    public String getPath() {
        return path;
    }

    @Nullable
    public String getNamespace() {
        return namespace;
    }

    @Nullable
    public String getType() {
        return type;
    }
}
