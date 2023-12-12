package fr.adrienbrault.idea.symfony2plugin.stubs.dict;

import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ConfigIndex implements Serializable {
    @NotNull
    private final String name;

    @NotNull
    private final TreeMap<String, TreeMap<String, String>> configs;

    @NotNull
    private final Set<String> values;

    public ConfigIndex(@NotNull String name, @NotNull TreeMap<String, TreeMap<String, String>> configs, @NotNull Set<String> values) {
        this.name = name;
        this.configs = configs;
        this.values = values;
    }

    @NotNull
    public String getName() {
        return name;
    }

    @NotNull
    public Map<String, TreeMap<String, String>> getConfigs() {
        return configs;
    }

    @NotNull
    public Set<String> getValues() {
        return values;
    }

    public int hashCode() {
        return new HashCodeBuilder()
            .append(this.name)
            .append(this.configs.hashCode())
            .toHashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof ConfigIndex &&
            Objects.equals(((ConfigIndex) obj).name, this.name) &&
            Objects.equals(((ConfigIndex) obj).configs, this.configs);
    }
}
