package fr.adrienbrault.idea.symfony2plugin.templating.dict;

import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TemplateFileMap {

    private final Map<String, Set<VirtualFile>> templateNames = new HashMap<String, Set<VirtualFile>>();

    public Map<String, Set<VirtualFile>> getTemplateNames() {
        return templateNames;
    }

    public Set<String> getNames(@NotNull VirtualFile virtualFile) {
        Set<String> fileNames = new HashSet<String>();

        for (Map.Entry<String, Set<VirtualFile>> entry : templateNames.entrySet()) {
            if(entry.getValue().contains(virtualFile)) {
                fileNames.add(entry.getKey());
            }
        }

        return fileNames;
    }

    @Deprecated
    public Map<String, VirtualFile> getTemplates() {

        Map<String, VirtualFile> templates = new HashMap<String, VirtualFile>();

        for (Map.Entry<String, Set<VirtualFile>> entry : templateNames.entrySet()) {
            templates.put(entry.getKey(), entry.getValue().iterator().next());
        }

        return templates;
    }

    public void put(@NotNull String namespace, @NotNull VirtualFile virtualFile) {
        if(!templateNames.containsKey(namespace)) {
            templateNames.put(namespace, new HashSet<VirtualFile>());
        }

        templateNames.get(namespace).add(virtualFile);
    }

    public void putAll(@NotNull Map<String, VirtualFile> files) {
        for (Map.Entry<String, VirtualFile> entry : files.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }
}
