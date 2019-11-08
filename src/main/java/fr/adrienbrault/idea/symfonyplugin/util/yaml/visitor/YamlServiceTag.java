package fr.adrienbrault.idea.symfonyplugin.util.yaml.visitor;

import fr.adrienbrault.idea.symfonyplugin.dic.tags.yaml.AttributeResolverInterface;
import fr.adrienbrault.idea.symfonyplugin.dic.tags.ServiceTagInterface;
import fr.adrienbrault.idea.symfonyplugin.dic.tags.yaml.YamlMappingAttributeResolver;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.yaml.psi.YAMLMapping;

/**
 * Abstract visitor, so we can support as YAMLHash in future
 * Don't to directly use YAMLHash here
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class YamlServiceTag implements ServiceTagInterface {
    @NotNull
    private final String serviceId;

    @NotNull
    private final String tagName;

    @NotNull
    private final AttributeResolverInterface attributeResolver;

    public YamlServiceTag(@NotNull String serviceId, @NotNull String tagName, @NotNull AttributeResolverInterface attributeResolver) {
        this.serviceId = serviceId;
        this.tagName = tagName;
        this.attributeResolver = attributeResolver;
    }

    public YamlServiceTag(@NotNull String serviceId, @NotNull String tagName, @NotNull YAMLMapping yamlMapping) {
        this(serviceId, tagName, new YamlMappingAttributeResolver(yamlMapping));
    }

    @NotNull
    @Override
    public String getServiceId() {
        return this.serviceId;
    }

    @NotNull
    public String getName() {
        return tagName;
    }

    /**
     * Our abstract method to get tag attributes
     *
     * @param attr yaml hash attribute to get value of
     * @return value
     */
    @Nullable
    public String getAttribute(@NotNull String attr) {
        return attributeResolver.getAttribute(attr);
    }
}
