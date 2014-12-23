package fr.adrienbrault.idea.symfony2plugin.templating.dict;

import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigExtensionParser;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class TwigExtension {

    private String signature = null;
    private TwigExtensionParser.TwigExtensionType twigExtensionType = null;
    private Map<String, String> options = new HashMap<String, String>();

    public TwigExtension(TwigExtensionParser.TwigExtensionType twigExtensionType) {
        this.twigExtensionType = twigExtensionType;
    }

    public TwigExtension(TwigExtensionParser.TwigExtensionType twigExtensionType, @Nullable String signature) {
        this(twigExtensionType);
        this.signature = signature;
    }

    public TwigExtensionParser.TwigExtensionType getTwigExtensionType() {
        return twigExtensionType;
    }

    public String getType() {
        return twigExtensionType.toString();
    }

    @Nullable
    public String getSignature() {
        return signature;
    }

    public TwigExtension putOption(String key , String value) {
        options.put(key, value);
        return this;
    }

    @Nullable
    public String getOption(String key) {
        return options.containsKey(key) ? options.get(key) : null;
    }

}
