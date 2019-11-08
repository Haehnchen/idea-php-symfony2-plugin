package fr.adrienbrault.idea.symfonyplugin.dic.tags.yaml;

import fr.adrienbrault.idea.symfonyplugin.util.yaml.YamlHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.yaml.psi.YAMLMapping;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class YamlMappingAttributeResolver implements AttributeResolverInterface {
    @NotNull
    private final YAMLMapping yamlMapping;

    public YamlMappingAttributeResolver(@NotNull YAMLMapping yamlMapping) {
        this.yamlMapping = yamlMapping;
    }

    @Nullable
    public String getAttribute(@NotNull String attr) {
        return YamlHelper.getYamlKeyValueAsString(yamlMapping, attr);
    }
}
