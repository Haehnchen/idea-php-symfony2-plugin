package fr.adrienbrault.idea.symfony2plugin.dic.container;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Create a service definition on a compiled debug xml file
 * We dont need a featureful implementation as xml file already cleaned up
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class XmlService implements ServiceInterface {
    @NotNull
    final private String id;

    @Nullable
    private String className = null;

    private boolean isPublic = true;

    private String alias = null;

    private Collection<String> tags;

    private XmlService(@NotNull String id) {
        this.id = id;
        this.tags = Collections.emptyList();
    }

    @NotNull
    @Override
    public String getId() {
        return id;
    }

    @Nullable
    @Override
    public String getClassName() {
        return className;
    }

    @Override
    public boolean isLazy() {
        return false;
    }

    @Override
    public boolean isAbstract() {
        return false;
    }

    @Override
    public boolean isAutowire() {
        return false;
    }

    @Override
    public boolean isDeprecated() {
        return false;
    }

    @Override
    public boolean isPublic() {
        return isPublic;
    }

    @Nullable
    @Override
    public String getAlias() {
        return alias;
    }

    @Nullable
    @Override
    public String getParent() {
        return null;
    }

    @Nullable
    @Override
    public String getDecorates() {
        return null;
    }

    @Nullable
    @Override
    public String getDecorationInnerName() {
        return null;
    }

    @NotNull
    @Override
    public Collection<String> getResource() {
        return Collections.emptyList();
    }

    @NotNull
    @Override
    public Collection<String> getExclude() {
        return Collections.emptyList();
    }

    @NotNull
    @Override
    public Collection<String> getTags() {
        return this.tags;
    }

    @Nullable
    public static XmlService createFromXml(@NotNull Element node) {
        // empty id does not interest us
        String id = node.getAttribute("id");
        if(StringUtils.isBlank(id)) {
            return null;
        }

        // <service id="Psr\Log\LoggerInterface $securityLogger" alias="monolog.logger.security"/>
        if (id.contains(" $") && id.matches("^.*\\s\\$.*$")) {
            return null;
        }

        if (id.startsWith(".")) {
            // <service id=".service_locator.XSes1R5" class="Symfony\Component\DependencyInjection\ServiceLocator" public="false">
            // <service id=".service_locator.tHpW6v3" alias=".service_locator.Y7gDuDN" public="false"/>
            if (id.startsWith(".service_locator.") || id.startsWith(".abstract.") || id.startsWith(".instanceof.") || id.startsWith(".debug.") || id.startsWith(".errored.")) {
                return null;
            }

            // <service id=".1_ArrayCache~kSL.YwK" class="Doctrine\Common\Cache\ArrayCache" public="false"/>
            // <service id=".2_~NpzP6Xn" public="false">
            if (id.matches("^\\.[\\w-]+~.*$")) {
                return null;
            }
        }

        XmlService xmlService = new XmlService(id);

        String aClass = node.getAttribute("class");
        if(StringUtils.isNotBlank(aClass)) {
            xmlService.className = StringUtils.stripStart(aClass, "\\");
        }

        String isPublic = node.getAttribute("public");
        if("false".equalsIgnoreCase(isPublic)) {
            xmlService.isPublic = false;
        }

        String alias = node.getAttribute("alias");
        if(StringUtils.isNotBlank(alias)) {
            xmlService.alias = alias;
        }

        // <tag name="xml_type_tag"/>
        Set<String> myTags = new HashSet<>();
        NodeList tags = node.getElementsByTagName("tag");
        int numTags = tags.getLength();
        for (int i = 0; i < numTags; i++) {
            Element section = (Element) tags.item(i);

            String name = section.getAttribute("name");
            if (StringUtils.isNotBlank(name)) {
                myTags.add(name);
            }
        }

        if (!myTags.isEmpty()) {
            xmlService.tags = Collections.unmodifiableSet(myTags);
        }

        return xmlService;
    }

    @NotNull
    public static XmlService create(@NotNull String id, @NotNull String className, boolean isPublic) {
        XmlService xmlService = new XmlService(id);

        xmlService.className = className;
        xmlService.isPublic = isPublic;

        return xmlService;
    }
}
