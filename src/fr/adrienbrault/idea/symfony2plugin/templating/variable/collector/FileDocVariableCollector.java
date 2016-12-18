package fr.adrienbrault.idea.symfony2plugin.templating.variable.collector;

import com.jetbrains.twig.TwigFile;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigTypeResolveUtil;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.TwigFileVariableCollector;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.TwigFileVariableCollectorParameter;

import java.util.*;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class FileDocVariableCollector implements TwigFileVariableCollector {

    @Override
    public void collect(TwigFileVariableCollectorParameter parameter, Map<String, Set<String>> variables) {
        if(!(parameter.getElement().getContainingFile() instanceof TwigFile)) {
            return;
        }
        variables.putAll(convertHashMapToTypeSet(TwigTypeResolveUtil.findFileVariableDocBlock((TwigFile) parameter.getElement().getContainingFile())));
    }

    private static Map<String, Set<String>> convertHashMapToTypeSet(Map<String, String> hashMap) {
        HashMap<String, Set<String>> globalVars = new HashMap<>();

        for(final Map.Entry<String, String> entry: hashMap.entrySet()) {
            globalVars.put(entry.getKey(), new HashSet<>(Collections.singletonList(entry.getValue())));
        }

        return globalVars;
    }

}
