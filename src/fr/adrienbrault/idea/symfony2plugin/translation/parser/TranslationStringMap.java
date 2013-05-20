package fr.adrienbrault.idea.symfony2plugin.translation.parser;

import java.util.*;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TranslationStringMap {

    private Map<String, String> stringMap;
    private Map<String, ArrayList> domainMap;

    public TranslationStringMap() {
        this.stringMap = new HashMap<String, String>();
        this.domainMap = new HashMap<String, ArrayList>();
    }

    public Map<String, String> getStringMap() {
        return stringMap;
    }

    public ArrayList<String> getDomainMap(String domainKey) {

        if(!domainMap.containsKey(domainKey)) {
            return new ArrayList<String>();
        }

        return domainMap.get(domainKey);
    }

    void addString(String domain, String stringId) {

        if(!domainMap.containsKey(domain)) {
            domainMap.put(domain, new ArrayList<String>());
        }

        stringMap.put(domain + ":" + stringId, stringId);

        if(!domainMap.get(domain).contains(stringId)) {
            domainMap.get(domain).add(stringId);
        }

    }

    public Set<String> getDomainList() {
        return domainMap.keySet();
    }


}

