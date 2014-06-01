package fr.adrienbrault.idea.symfony2plugin.templating.dict;

import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigExtensionParser;
import org.jetbrains.annotations.Nullable;

public class TwigExtension {

    private String signature = null;
    private TwigExtensionParser.TwigExtensionType twigExtensionType = null;

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

}
