package fr.adrienbrault.idea.symfony2plugin.dic.tags.xml;

import com.intellij.psi.xml.XmlTag;
import fr.adrienbrault.idea.symfony2plugin.dic.tags.ServiceTagInterface;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class XmlServiceTag implements ServiceTagInterface {

    @NotNull
    private final String name;

    @NotNull
    private final String serviceId;
    @NotNull
    private final XmlTag xmlTag;

    private XmlServiceTag(@NotNull String name, @NotNull String serviceId, @NotNull XmlTag xmlTag) {
        this.name = name;
        this.serviceId = serviceId;
        this.xmlTag = xmlTag;
    }

    @NotNull
    @Override
    public String getName() {
        return this.name;
    }

    @Nullable
    @Override
    public String getAttribute(@NotNull String attr) {
        return this.xmlTag.getAttributeValue(attr);
    }

    @Override
    @NotNull
    public String getServiceId() {
        return this.serviceId;
    }

    @Nullable
    public static ServiceTagInterface create(@NotNull String serviceId, @NotNull XmlTag xmlTag) {
        String name = xmlTag.getAttributeValue("name");
        if(StringUtils.isBlank(name)) {
            return null;
        }

        return new XmlServiceTag(name, serviceId, xmlTag);
    }
}
