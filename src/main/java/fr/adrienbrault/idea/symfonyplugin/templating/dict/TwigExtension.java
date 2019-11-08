package fr.adrienbrault.idea.symfonyplugin.templating.dict;

import fr.adrienbrault.idea.symfonyplugin.templating.util.TwigExtensionParser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TwigExtension {
    @Nullable
    final private String signature;

    @NotNull
    final private TwigExtensionParser.TwigExtensionType twigExtensionType;

    @NotNull
    final private Map<String, String> options = new HashMap<>();

    public TwigExtension(@NotNull TwigExtensionParser.TwigExtensionType twigExtensionType) {
        this(twigExtensionType, null);
    }

    public TwigExtension(@NotNull TwigExtensionParser.TwigExtensionType twigExtensionType, @Nullable String signature) {
        this.twigExtensionType = twigExtensionType;
        this.signature = signature;
    }

    public TwigExtension(@NotNull TwigExtensionParser.TwigExtensionType twigExtensionType, @Nullable String signature, @NotNull Map<String, String> options) {
        this(twigExtensionType, signature);
        this.options.putAll(options);
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

    @Nullable
    String getOption(String key) {
        return options.getOrDefault(key, null);
    }
}
