package fr.adrienbrault.idea.symfony2plugin.doctrine.component;

import fr.adrienbrault.idea.symfony2plugin.util.service.AbstractServiceParser;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class EntityNamesServiceParser extends AbstractServiceParser {

    @Override
    public String getXPathFilter() {
        return "/container/services/service[@id[starts-with(.,'doctrine.orm.')]]//call[@method='setEntityNamespaces']//argument[@key]";
    }

    public Map<String, String> parser(File file) {
        NodeList nodeList = this.parserer(file);

        if(nodeList == null) {
            return new HashMap<String, String>();
        }

        Map<String, String> map = new HashMap<String, String>();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Element node = (Element) nodeList.item(i);
            map.put(node.getAttribute("key"), "\\" + node.getTextContent());
        }

        return map;
    }

}