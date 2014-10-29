package fr.adrienbrault.idea.symfony2plugin.remote.completion.json;

import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.Nullable;

public class JsonRawLookupElement {

    @SerializedName("lookup_string")
    private String lookupString;

    @SerializedName("presentable_text")
    private String presentableText;

    @SerializedName("type_text")
    private String typeText;

    @SerializedName("tail_text")
    private String tailText;

    private String icon;
    private String target;

    public String getLookupString() {
        return lookupString;
    }

    @Nullable
    public String getPresentableText() {
        return presentableText;
    }

    @Nullable
    public String getTypeText() {
        return typeText;
    }

    @Nullable
    public String getTailText() {
        return tailText;
    }

    @Nullable
    public String getIcon() {
        return icon;
    }

    @Nullable
    public String getTarget() {
        return target;
    }

}