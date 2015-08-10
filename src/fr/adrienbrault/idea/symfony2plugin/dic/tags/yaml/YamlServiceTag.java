package fr.adrienbrault.idea.symfony2plugin.dic.tags.yaml;

import fr.adrienbrault.idea.symfony2plugin.dic.tags.ServiceTagInterface;
import fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlHelper;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.yaml.psi.YAMLHash;

/**
 * Abstract visitor, so we can support as YAMLHash in future
 * Don't to directly use YAMLHash here
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class YamlServiceTag implements ServiceTagInterface {

    @NotNull
    private final String name;

    @NotNull
    private final String serviceId;

    @NotNull
    private final YAMLHash yamlHash;

    private YamlServiceTag(@NotNull String name, @NotNull String serviceId, @NotNull YAMLHash yamlHash) {
        this.name = name;
        this.serviceId = serviceId;
        this.yamlHash = yamlHash;
    }

    @NotNull
    @Override
    public String getServiceId() {
        return this.serviceId;
    }

    @NotNull
    public String getName() {
        return this.name;
    }

    @Nullable
    public String getAttribute(@NotNull String attr) {
        return YamlHelper.getYamlKeyValueAsString(yamlHash, attr);
    }

    @Nullable
    public static ServiceTagInterface create(@NotNull String serviceId, @NotNull YAMLHash yamlHash) {
        String name = YamlHelper.getYamlKeyValueAsString(yamlHash, "name");
        if(StringUtils.isBlank(name)) {
            return null;
        }

        return new YamlServiceTag(name, serviceId, yamlHash);
    }
}
