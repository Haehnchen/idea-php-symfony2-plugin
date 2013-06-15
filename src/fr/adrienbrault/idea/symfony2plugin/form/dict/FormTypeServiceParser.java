package fr.adrienbrault.idea.symfony2plugin.form.dict;

import fr.adrienbrault.idea.symfony2plugin.util.service.AbstractServiceParser;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class FormTypeServiceParser extends AbstractServiceParser {

    protected FormTypeMap formTypeMap = new FormTypeMap();

    @Override
    public String getXPathFilter() {
        return "/container/services/service[@id='form.registry']//service[@class]/argument[@type='collection'][1]/argument[@key]";
    }

    public void parser(File file) {
        NodeList nodeList = this.parserer(file);

        if(nodeList == null) {
            return;
        }

        for (int i = 0; i < nodeList.getLength(); i++) {
            Element node = (Element) nodeList.item(i);
            this.formTypeMap.getMap().put(node.getTextContent(), node.getAttribute("key"));
        }

    }

    public FormTypeMap getFormTypeMap() {
        return formTypeMap;
    }

}