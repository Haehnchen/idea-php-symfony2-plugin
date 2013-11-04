package fr.adrienbrault.idea.symfony2plugin.templating.variable.collector;

import com.jetbrains.twig.TwigFile;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigTypeResolveUtil;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.TwigFileVariableCollector;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.TwigFileVariableCollectorParameter;

import java.util.*;

public class FileDocVariableCollector implements TwigFileVariableCollector {

    @Override
    public void collect(TwigFileVariableCollectorParameter parameter, HashMap<String, Set<String>> variables) {
        if(!(parameter.getElement().getContainingFile() instanceof TwigFile)) {
            return;
        }
        variables.putAll(convertHashMapToTypeSet(TwigTypeResolveUtil.findFileVariableDocBlock((TwigFile) parameter.getElement().getContainingFile())));
    }

    private static HashMap<String, Set<String>> convertHashMapToTypeSet(HashMap<String, String> hashMap) {
        HashMap<String, Set<String>> globalVars = new HashMap<String, Set<String>>();

        for(final Map.Entry<String, String> entry: hashMap.entrySet()) {
            globalVars.put(entry.getKey(), new HashSet<String>(Arrays.asList(entry.getValue())));
        }

        return globalVars;
    }

}
