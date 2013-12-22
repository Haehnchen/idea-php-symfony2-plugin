package fr.adrienbrault.idea.symfony2plugin.translation.parser;

import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TranslationStringMap {

    private Map<String, Set<String>> domainMap;

    public TranslationStringMap() {
        this.domainMap = new ConcurrentHashMap<String, Set<String>>();
    }

    @Nullable
    public Set<String> getDomainMap(String domainKey) {

        if(!domainMap.containsKey(domainKey)) {
            return null;
        }

        return domainMap.get(domainKey);
    }

    public void addString(String domain, String stringId) {

        if(!domainMap.containsKey(domain)) {
            domainMap.put(domain, new HashSet<String>());
        }

        domainMap.get(domain).add(stringId);
    }

    public Set<String> getDomainList() {
        return domainMap.keySet();
    }

    public void addDomain(String domain) {

        if(!domainMap.containsKey(domain)) {
            domainMap.put(domain, new HashSet<String>());
        }

    }


}

