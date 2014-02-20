package fr.adrienbrault.idea.symfony2plugin.templating.globals;

import fr.adrienbrault.idea.symfony2plugin.templating.path.TwigPath;
import fr.adrienbrault.idea.symfony2plugin.templating.path.TwigPathIndex;
import fr.adrienbrault.idea.symfony2plugin.util.service.AbstractServiceParser;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TwigGlobalsServiceParser extends AbstractServiceParser {

    protected Map<String, TwigGlobalVariable> twigGlobals = new ConcurrentHashMap<String, TwigGlobalVariable>();

    @Override
    public String getXPathFilter() {
        return "/container/services/service[@id='twig']//call[@method='addGlobal']";
    }

    public void parser(File file) {
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