package fr.adrienbrault.idea.symfony2plugin.dic.attribute.value;

import fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlHelper;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.yaml.psi.YAMLKeyValue;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class YamlKeyValueAttributeValue extends AttributeValueAbstract {

    @NotNull
    private final YAMLKeyValue yamlKeyValue;

    public YamlKeyValueAttributeValue(@NotNull YAMLKeyValue yamlKeyValue) {
        this.yamlKeyValue = yamlKeyValue;
    }

    @Nullable
    @Override
    public String getString(@NotNull String key) {
        String value = YamlHelper.getYamlKeyValueAsString(yamlKeyValue, key);
        if(StringUtils.isBlank(value)) {
            return null;
        }

        return value;
    }
}
