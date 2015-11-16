package fr.adrienbrault.idea.symfony2plugin.dic.attribute.value;

import com.intellij.psi.xml.XmlTag;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class XmlTagAttributeValue implements AttributeValueInterface {

    @NotNull
    private final XmlTag xmlTag;

    public XmlTagAttributeValue(@NotNull XmlTag xmlTag) {
        this.xmlTag = xmlTag;
    }

    @Nullable
    @Override
    public String getString(@NotNull String key) {
        String value = this.xmlTag.getAttributeValue(key);
        if(StringUtils.isBlank(value)) {
            return null;
        }

        return value;
    }
}