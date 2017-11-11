package fr.adrienbrault.idea.symfony2plugin.templating.dict;

import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TemplateFileMap {
    @NotNull
    private final Map<String, Set<VirtualFile>> templateNames = new HashMap<>();

    public TemplateFileMap(@NotNull Map<String, VirtualFile> templates) {
        for (Map.Entry<String, VirtualFile> entry : templates.entrySet()) {
            String namespace = entry.getKey();
            if(!templateNames.containsKey(namespace)) {
                templateNames.put(namespace, new HashSet<>());
            }

            templateNames.get(namespace).add(entry.getValue());
        }
    }

    @NotNull
    @Deprecated
    public Map<String, VirtualFile> getTemplates() {
        return templateNames.entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().iterator().next(), (a, b) -> b));
    }
}
