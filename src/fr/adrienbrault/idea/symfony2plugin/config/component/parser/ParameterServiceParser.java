package fr.adrienbrault.idea.symfony2plugin.config.component.parser;

import fr.adrienbrault.idea.symfony2plugin.translation.dict.DomainFileMap;
import fr.adrienbrault.idea.symfony2plugin.util.service.AbstractServiceParser;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ParameterServiceParser extends AbstractServiceParser {

    @Override
    public String getXPathFilter() {
        return "/container/parameters/parameter[@key]";
    }

    public Map<String, String> parser(File file) {
        NodeList nodeList = this.parserer(file);

        if(nodeList == null) {
            return new HashMap<String, String>();
        }

        Map<String, String> map = new HashMap<String, String>();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Element node = (Element) nodeList.item(i);
            String parameterValue = node.hasAttribute("type") && node.getAttribute("type").equals("collection") ?  "collection" : node.getTextContent();
            map.put(node.getAttribute("key"), parameterValue);
        }

        return map;
    }

}