package fr.adrienbrault.idea.symfonyplugin.templating.path;

import com.intellij.openapi.project.Project;
import com.intellij.util.xmlb.annotations.Attribute;
import com.intellij.util.xmlb.annotations.Tag;
import fr.adrienbrault.idea.symfonyplugin.templating.util.TwigUtil;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@Tag("twig_namespace")
public class TwigNamespaceSetting {

    private String path;
    private String namespace = TwigUtil.MAIN;
    private boolean isEnabled = true;
    private TwigUtil.NamespaceType namespaceType;
    private boolean custom = false;

    public TwigNamespaceSetting(String namespace, String path, Boolean enabled, TwigUtil.NamespaceType namespaceType) {
        this.namespace = namespace;
        this.path = path;
        this.isEnabled = enabled;
        this.namespaceType = namespaceType;
    }

    public TwigNamespaceSetting(String namespace, String path, Boolean enabled, TwigUtil.NamespaceType namespaceType, boolean custom) {
        this(namespace, path, enabled,namespaceType);
        this.custom = custom;
    }

    public TwigNamespaceSetting() {
    }

    @Attribute("namespaceType")
    public TwigUtil.NamespaceType getNamespaceType() {
        return namespaceType;
    }

    public void setNamespaceType(TwigUtil.NamespaceType namespaceType) {
        this.namespaceType = namespaceType;
    }

    @Attribute("namespace")
    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    @Attribute("path")
    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    @Attribute("enabled")
    public boolean isEnabled() {
        return isEnabled;
    }

    public boolean equals(Project project, TwigPath twigPath) {
        if(!twigPath.getNamespaceType().equals(this.getNamespaceType()) || !twigPath.getNamespace().equals(this.getNamespace())) {
            return false;
        }

        String relativePath = twigPath.getRelativePath(project);
        return relativePath != null && relativePath.equals(this.getPath());
    }

    public TwigNamespaceSetting setEnabled(boolean disabled) {
        isEnabled = disabled;
        return this;
    }

    @Attribute("custom")
    public boolean isCustom() {
        return custom;
    }

    public void setCustom(boolean custom) {
        this.custom = custom;
    }

}

