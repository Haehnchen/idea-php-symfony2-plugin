package fr.adrienbrault.idea.symfony2plugin.dic.attribute.value;

import com.intellij.psi.xml.XmlTag;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class XmlTagAttributeValue extends AttributeValueAbstract {
    @NotNull
    private final XmlTag xmlTag;

    public XmlTagAttributeValue(@NotNull XmlTag xmlTag) {
        super(xmlTag);
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

    @NotNull
    @Override
    public Collection<String> getTags() {
        return Arrays.stream(xmlTag.getSubTags())
            .filter(subTag -> "tag".equals(subTag.getName()))
            .map(subTag -> subTag.getAttributeValue("name"))
            .filter(StringUtils::isNotBlank)
            .collect(Collectors.toSet());
    }
}