package fr.adrienbrault.idea.symfony2plugin.dic;

import fr.adrienbrault.idea.symfony2plugin.util.service.AbstractServiceParser;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class XmlTagParser extends AbstractServiceParser {

    protected HashSet<String> list = new HashSet<String>();
    protected HashMap<String, ArrayList<String>> taggedClasses = new HashMap<String, ArrayList<String>>();

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
            String tagName = node.getAttribute("name");
            this.list.add(tagName);
            Element parentNode = (Element) node.getParentNode();
            if(parentNode.hasAttribute("class")) {
                this.addTaggedClass(tagName, parentNode.getAttribute("class"));
            }
        }

    }


    public HashMap<String, ArrayList<String>> getTaggedClasses() {
        return taggedClasses;
    }

    @Nullable
    public ArrayList<String> getTaggedClass(String tagName) {
        if(!this.taggedClasses.containsKey(tagName)) {
            return null;
        }

        return this.taggedClasses.get(tagName);
    }

    private void addTaggedClass(String tagName, String className) {
        if(!this.taggedClasses.containsKey(tagName)) {
            this.taggedClasses.put(tagName, new ArrayList<String>());
        }

        this.taggedClasses.get(tagName).add(className);
    }

    public HashSet<String> get() {
        return list;
    }

}