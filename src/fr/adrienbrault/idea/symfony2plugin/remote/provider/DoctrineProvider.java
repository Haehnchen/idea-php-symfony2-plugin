package fr.adrienbrault.idea.symfony2plugin.remote.provider;

import com.google.gson.JsonElement;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class DoctrineProvider implements ProviderInterface {

    private Map<String, String> namespaceMap = new HashMap<String, String>();
    private Collection<String> paths = new HashSet<String>();

    public void collect(JsonElement jsonElement) {

        namespaceMap = new HashMap<String, String>();
        paths = new HashSet<String>();

        for(Map.Entry<String, JsonElement> entityManger: jsonElement.getAsJsonObject().entrySet()) {

            for(Map.Entry<String, JsonElement> config: entityManger.getValue().getAsJsonObject().entrySet()) {

                if("mappings".equals(config.getKey())) {
                    for(JsonElement mapping: config.getValue().getAsJsonArray()) {
                        for(JsonElement path : mapping.getAsJsonObject().get("paths").getAsJsonArray()) {
                            paths.add(path.getAsString());
                        }
                    }
                }

                if("entity_namespaces".equals(config.getKey())) {
                    for(Map.Entry<String, JsonElement> namespaces: config.getValue().getAsJsonObject().entrySet()) {
                        namespaceMap.put(namespaces.getKey(), namespaces.getValue().getAsString());
                    }
                }

            }

        }
    }

    public Map<String, String> getNamespaceMap() {
        return namespaceMap;
    }

    public Collection<String> getPaths() {
        return paths;
    }

    public String getAlias() {
        return "doctrine";
    }


}
