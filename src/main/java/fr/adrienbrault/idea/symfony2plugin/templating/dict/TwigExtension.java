package fr.adrienbrault.idea.symfony2plugin.templating.dict;

import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigExtensionParser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

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

    @NotNull
    private Collection<String> types = Collections.emptyList();

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

    public TwigExtension(@NotNull TwigExtensionParser.TwigExtensionType twigExtensionType, @Nullable String signature, @NotNull Map<String, String> options, @NotNull Collection<String> types) {
        this(twigExtensionType, signature);
        this.options.putAll(options);
        this.types = new HashSet<>(types);
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
    public String getOption(String key) {
        return options.getOrDefault(key, null);
    }

    @NotNull
    public Collection<String> getTypes() {
        return types;
    }

    public boolean isDeprecated() {
        return options.containsKey("deprecated") || options.containsKey("deprecation_info");
    }
}
