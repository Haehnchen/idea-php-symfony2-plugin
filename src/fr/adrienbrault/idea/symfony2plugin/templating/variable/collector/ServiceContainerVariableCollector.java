package fr.adrienbrault.idea.symfony2plugin.templating.variable.collector;

import fr.adrienbrault.idea.symfony2plugin.dic.XmlServiceParser;
import fr.adrienbrault.idea.symfony2plugin.templating.globals.TwigGlobalEnum;
import fr.adrienbrault.idea.symfony2plugin.templating.globals.TwigGlobalVariable;
import fr.adrienbrault.idea.symfony2plugin.templating.globals.TwigGlobalsServiceParser;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.TwigFileVariableCollector;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.TwigFileVariableCollectorParameter;
import fr.adrienbrault.idea.symfony2plugin.util.service.ServiceXmlParserFactory;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ServiceContainerVariableCollector implements TwigFileVariableCollector {

    @Override
    public void collect(TwigFileVariableCollectorParameter parameter, Map<String, Set<String>> variables) {

        TwigGlobalsServiceParser twigPathServiceParser = ServiceXmlParserFactory.getInstance(parameter.getProject(), TwigGlobalsServiceParser.class);
        for(Map.Entry<String, TwigGlobalVariable> globalVariableEntry: twigPathServiceParser.getTwigGlobals().entrySet()) {
            if(globalVariableEntry.getValue().getTwigGlobalEnum() == TwigGlobalEnum.SERVICE) {
                String serviceClass = ServiceXmlParserFactory.getInstance(parameter.getProject(), XmlServiceParser.class).getServiceMap().getMap().get(globalVariableEntry.getValue().getValue());
                if (serviceClass != null) {
                    variables.put(globalVariableEntry.getKey(), new HashSet<>(Arrays.asList(serviceClass)));
                }
            }
        }

    }

}
