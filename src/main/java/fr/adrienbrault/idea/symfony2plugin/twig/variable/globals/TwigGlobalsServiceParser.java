package fr.adrienbrault.idea.symfony2plugin.twig.variable.globals;

import fr.adrienbrault.idea.symfony2plugin.util.service.AbstractServiceParser;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TwigGlobalsServiceParser extends AbstractServiceParser {
    private final Map<String, TwigGlobalVariable> twigGlobals = new ConcurrentHashMap<>();

    @Override
    public String getXPathFilter() {
        return "/container/services/service[@id='twig']//call[@method='addGlobal']";
    }

    public void parser(InputStream file) {
        NodeList nodeList = this.parserer(file);

        if(nodeList == null) {
            return;
        }

        for (int i = 0; i < nodeList.getLength(); i++) {
            Element node = (Element) nodeList.item(i);

            NodeList arguments = node.getElementsByTagName("argument");

            if(arguments.getLength() == 2) {
                String globalName = arguments.item(0).getTextContent();

                Element nodeValue = (Element) arguments.item(1);
                if(nodeValue.hasAttribute("id")) {
                    this.twigGlobals.put(globalName, new TwigGlobalVariable(globalName, nodeValue.getAttribute("id"), TwigGlobalEnum.SERVICE));
                } else {
                    this.twigGlobals.put(globalName, new TwigGlobalVariable(globalName, arguments.item(1).getTextContent(), TwigGlobalEnum.TEXT));
                }

            }

        }
    }

    public Map<String, TwigGlobalVariable> getTwigGlobals() {
        return twigGlobals;
    }
}