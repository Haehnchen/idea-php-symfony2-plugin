package fr.adrienbrault.idea.symfony2plugin.dic.container;

import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Element;

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

    private XmlService(@NotNull String id) {
        this.id = id;
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

    @Nullable
    public static XmlService createFromXml(@NotNull Element node) {
        // empty id does not interest us
        String id = node.getAttribute("id");
        if(StringUtils.isBlank(id)) {
            return null;
        }

        XmlService xmlService = new XmlService(id);

        String aClass = node.getAttribute("class");
        if(StringUtils.isNotBlank(aClass)) {
            xmlService.className = StringUtils.stripStart(aClass, "\\");
        }

        String isPublic = node.getAttribute("public");
        if(isPublic != null && "false".equalsIgnoreCase(isPublic)) {
            xmlService.isPublic = false;
        }

        String alias = node.getAttribute("alias");
        if(StringUtils.isNotBlank(alias)) {
            xmlService.alias = alias;
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
