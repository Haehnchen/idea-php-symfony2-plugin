package fr.adrienbrault.idea.symfonyplugin.dic.tags.yaml;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class StaticAttributeResolver implements AttributeResolverInterface {
    @NotNull
    private final Map<String, String> map;

    public StaticAttributeResolver(@NotNull String key, @NotNull String value) {
        this.map = new HashMap<>();
        this.map.put(key, value);
    }
    public StaticAttributeResolver(@NotNull Map<String, String> map) {
        this.map = map;
    }

    @Nullable
    @Override
    public String getAttribute(@NotNull String attr) {
        if(this.map.containsKey(attr)) {
            return this.map.get(attr);
        }

        return null;
    }
}
