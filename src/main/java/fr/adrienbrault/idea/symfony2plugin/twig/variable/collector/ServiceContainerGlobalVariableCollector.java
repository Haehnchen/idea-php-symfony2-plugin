package fr.adrienbrault.idea.symfony2plugin.twig.variable.collector;

import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.twig.variable.globals.TwigGlobalEnum;
import fr.adrienbrault.idea.symfony2plugin.twig.variable.globals.TwigGlobalVariable;
import fr.adrienbrault.idea.symfony2plugin.twig.variable.globals.TwigGlobalsServiceParser;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.TwigFileVariableCollector;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.TwigFileVariableCollectorParameter;
import fr.adrienbrault.idea.symfony2plugin.util.dict.ServiceUtil;
import fr.adrienbrault.idea.symfony2plugin.util.service.ServiceXmlParserFactory;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ServiceContainerGlobalVariableCollector implements TwigFileVariableCollector {

    @Override
    public void collect(@NotNull TwigFileVariableCollectorParameter parameter, @NotNull Map<String, Set<String>> variables) {
        Map<String, Set<String>> map = new HashMap<>();

        TwigGlobalsServiceParser twigPathServiceParser = ServiceXmlParserFactory.getInstance(parameter.getProject(), TwigGlobalsServiceParser.class);
        for(Map.Entry<String, TwigGlobalVariable> globalVariableEntry: twigPathServiceParser.getTwigGlobals().entrySet()) {
            if(globalVariableEntry.getValue().getTwigGlobalEnum() == TwigGlobalEnum.SERVICE) {
                String serviceName = globalVariableEntry.getValue().getValue();
                PhpClass phpClass = ServiceUtil.getServiceClass(parameter.getProject(), serviceName);
                if(phpClass != null) {
                    String key = globalVariableEntry.getKey();
                    map.putIfAbsent(key, new HashSet<>());
                    map.get(key).add("\\" + StringUtils.stripStart(phpClass.getFQN(), "\\"));
                }
            }
        }

        variables.putAll(map);
    }
}
