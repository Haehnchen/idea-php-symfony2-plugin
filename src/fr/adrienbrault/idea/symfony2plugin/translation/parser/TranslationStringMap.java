package fr.adrienbrault.idea.symfony2plugin.translation.parser;

import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TranslationStringMap {

    private Map<String, Set<String>> domainMap;
    private Map<String, Long> fileNames;

    public Map<String, Long>getFileNames() {
        return fileNames;
    }

    public TranslationStringMap() {
        this.domainMap = new ConcurrentHashMap<String, Set<String>>();
        this.fileNames = new HashMap<String, Long>();
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

    public void addFile(String filename, Long changed) {
        this.fileNames.put(filename, changed);
    }

}

