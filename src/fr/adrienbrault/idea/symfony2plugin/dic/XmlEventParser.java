package fr.adrienbrault.idea.symfony2plugin.dic;

import fr.adrienbrault.idea.symfony2plugin.util.service.AbstractServiceParser;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.File;
import java.util.HashMap;

public class XmlEventParser extends AbstractServiceParser {

    protected HashMap<String, String> list = new HashMap<String, String>();

    @Override
    public String getXPathFilter() {
        return "/container/services/service[@id]/tag[@event]";
    }

    public void parser(File file) {
        NodeList nodeList = this.parserer(file);

        if(nodeList == null) {
            return;
        }

        for (int i = 0; i < nodeList.getLength(); i++) {
            Element node = (Element) nodeList.item(i);
            this.list.put(node.getAttribute("event"), node.getAttribute("name"));
        }

    }

    public HashMap<String, String> get() {
        return list;
    }

}