package fr.adrienbrault.idea.symfony2plugin.dic;

import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Adrien Brault <adrien.brault@gmail.com>
 */
public class ServiceMap {

    private Map<String, String> map;
    private Map<String, String> publicMap;

    public ServiceMap(Map<String, String> map, Map<String, String> publicMap) {
        this.map = Collections.unmodifiableMap(map);
        this.publicMap = Collections.unmodifiableMap(publicMap);
    }

    public ServiceMap() {
       this.map = new HashMap<String, String>();
       this.publicMap = new HashMap<String, String>();
    }

    public Map<String, String> getMap() {
        return map;
    }

    public Map<String, String> getPublicMap() {
        return publicMap;
    }

    @Nullable
    public String resolveClassName(String findValue) {

        if(!findValue.startsWith("\\")) {
            findValue = "\\" + findValue;
        }

        for (Map.Entry<String, String> entry : this.getMap().entrySet()) {
            if (entry.getValue().equals(findValue)) {
                return entry.getKey();
            }
        }

        return null;
    }

}
