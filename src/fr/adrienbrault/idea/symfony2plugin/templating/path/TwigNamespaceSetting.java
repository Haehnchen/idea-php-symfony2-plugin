package fr.adrienbrault.idea.symfony2plugin.templating.path;

import com.intellij.openapi.project.Project;
import com.intellij.util.xmlb.annotations.Attribute;
import com.intellij.util.xmlb.annotations.Tag;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@Tag("twig_namespace")
public class TwigNamespaceSetting {

    private String path;
    private String namespace = TwigPathIndex.MAIN;
    private boolean isEnabled = true;
    private TwigPathIndex.NamespaceType namespaceType;
    private boolean custom = false;

    public TwigNamespaceSetting(String namespace, String path, Boolean enabled, TwigPathIndex.NamespaceType namespaceType) {
        this.namespace = namespace;
        this.path = path;
        this.isEnabled = enabled;
        this.namespaceType = namespaceType;
    }

    public TwigNamespaceSetting(String namespace, String path, Boolean enabled, TwigPathIndex.NamespaceType namespaceType, boolean custom) {
        this(namespace, path, enabled,namespaceType);
        this.custom = custom;
    }

    public TwigNamespaceSetting() {
    }

    @Attribute("namespaceType")
    public TwigPathIndex.NamespaceType getNamespaceType() {
        return namespaceType;
    }

    public void setNamespaceType(TwigPathIndex.NamespaceType namespaceType) {
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

