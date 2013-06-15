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

    protected  Map<String, String> parameterMap = new HashMap<String, String>();

    @Override
    public String getXPathFilter() {
        return "/container/parameters/parameter[@key]";
    }

    public void parser(File file) {
        NodeList nodeList = this.parserer(file);

        if(nodeList == null) {
            return;
        }

        for (int i = 0; i < nodeList.getLength(); i++) {
            Element node = (Element) nodeList.item(i);
            String parameterValue = node.hasAttribute("type") && node.getAttribute("type").equals("collection") ?  "collection" : node.getTextContent();
            this.parameterMap.put(node.getAttribute("key"), parameterValue);
        }

    }

    public Map<String, String> getParameterMap() {
        return parameterMap;
    }

}