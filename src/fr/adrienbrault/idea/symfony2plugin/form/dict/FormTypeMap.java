package fr.adrienbrault.idea.symfony2plugin.form.dict;


import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class FormTypeMap {

    private Map<String, String> map;

    public FormTypeMap(Map<String, String> map) {
        this.map = Collections.unmodifiableMap(map);
    }

    public FormTypeMap() {
        this.map = new HashMap<String, String>();
    }

    public Map<String, String> getMap() {
        return map;
    }

}
