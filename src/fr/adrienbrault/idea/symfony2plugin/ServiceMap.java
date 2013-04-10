package fr.adrienbrault.idea.symfony2plugin;

import java.util.Map;

/**
 * @author Adrien Brault <adrien.brault@gmail.com>
 */
public class ServiceMap {

    private Map<String, String> map;
    private Map<String, String> publicMap;

    public ServiceMap(Map<String, String> map, Map<String, String> publicMap) {
        this.map = map;
        this.publicMap = publicMap;
    }

    public Map<String, String> getMap() {
        return map;
    }

    public Map<String, String> getPublicMap() {
        return publicMap;
    }

}
