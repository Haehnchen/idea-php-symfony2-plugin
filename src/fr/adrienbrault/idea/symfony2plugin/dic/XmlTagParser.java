package fr.adrienbrault.idea.symfony2plugin.dic;

import fr.adrienbrault.idea.symfony2plugin.util.service.AbstractServiceParser;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;

public class XmlTagParser extends AbstractServiceParser {

    protected HashSet<String> list = new HashSet<String>();

    @Override
    public String getXPathFilter() {
        return "/container/services/service[@id]/tag[@name]";
    }

    public void parser(File file) {
        NodeList nodeList = this.parserer(file);

        if(nodeList == null) {
            return;
        }

        for (int i = 0; i < nodeList.getLength(); i++) {
            Element node = (Element) nodeList.item(i);
            this.list.add(node.getAttribute("name"));
        }

    }

    public HashSet<String> get() {
        return list;
    }

}