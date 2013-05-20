package fr.adrienbrault.idea.symfony2plugin.form.dict;

import fr.adrienbrault.idea.symfony2plugin.util.service.AbstractServiceParser;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class FormTypeServiceParser extends AbstractServiceParser {

    @Override
    public String getXPathFilter() {
        return "/container/services/service[@id='form.registry']//service[@class]/argument[@type='collection'][1]/argument[@key]";
    }

    public FormTypeMap parser(File file) {
        NodeList nodeList = this.parserer(file);

        Map<String, String> fromTypesMap = new HashMap<String, String>();

        if(nodeList == null) {
            return new FormTypeMap(fromTypesMap);
        }

        for (int i = 0; i < nodeList.getLength(); i++) {
            Element node = (Element) nodeList.item(i);
            fromTypesMap.put(node.getTextContent(), node.getAttribute("key"));
        }

        return new FormTypeMap(fromTypesMap);
    }

}