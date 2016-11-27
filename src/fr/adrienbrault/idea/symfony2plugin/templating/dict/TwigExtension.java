package fr.adrienbrault.idea.symfony2plugin.templating.dict;

import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigExtensionParser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class TwigExtension {
    @Nullable
    private String signature = null;

    @NotNull
    private TwigExtensionParser.TwigExtensionType twigExtensionType;

    @NotNull
    private Map<String, String> options = new HashMap<>();

    public TwigExtension(@NotNull TwigExtensionParser.TwigExtensionType twigExtensionType) {
        this.twigExtensionType = twigExtensionType;
    }

    public TwigExtension(@NotNull TwigExtensionParser.TwigExtensionType twigExtensionType, @Nullable String signature) {
        this(twigExtensionType);
        this.signature = signature;
    }

    @NotNull
    public TwigExtensionParser.TwigExtensionType getTwigExtensionType() {
        return twigExtensionType;
    }

    @NotNull
    public String getType() {
        return twigExtensionType.toString();
    }

    @Nullable
    public String getSignature() {
        return signature;
    }

    @NotNull
    public TwigExtension putOption(@NotNull String key, @NotNull String value) {
        options.put(key, value);
        return this;
    }

    @Nullable
    public String getOption(String key) {
        return options.containsKey(key) ? options.get(key) : null;
    }
}
