package fr.adrienbrault.idea.symfony2plugin.templating.variable.collector;

import com.jetbrains.twig.TwigFile;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigTypeResolveUtil;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.TwigFileVariableCollector;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.TwigFileVariableCollectorParameter;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class FileDocVariableCollector implements TwigFileVariableCollector {
    @Override
    public void collect(@NotNull TwigFileVariableCollectorParameter parameter, @NotNull Map<String, Set<String>> variables) {
        if(!(parameter.getElement().getContainingFile() instanceof TwigFile)) {
            return;
        }
        variables.putAll(convertHashMapToTypeSet(TwigTypeResolveUtil.findFileVariableDocBlock((TwigFile) parameter.getElement().getContainingFile())));
    }

    private static Map<String, Set<String>> convertHashMapToTypeSet(@NotNull Map<String, String> hashMap) {
        HashMap<String, Set<String>> globalVars = new HashMap<>();

        for(final Map.Entry<String, String> entry: hashMap.entrySet()) {
            String value = entry.getValue();
            if (value != null) {
                globalVars.put(entry.getKey(), new HashSet<>(Collections.singletonList(value)));
            } else {
                globalVars.put(entry.getKey(), new HashSet<>());
            }
        }

        return globalVars;
    }
}
