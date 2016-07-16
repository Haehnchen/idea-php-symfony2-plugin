package fr.adrienbrault.idea.symfony2plugin.dic;

import fr.adrienbrault.idea.symfony2plugin.util.service.AbstractServiceParser;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class XmlTagParser extends AbstractServiceParser {

    protected Set<String> list = new HashSet<>();
    protected Map<String, ArrayList<String>> taggedClasses = new ConcurrentHashMap<>();

    @Override
    public String getXPathFilter() {
        return "/container/services/service[@id]/tag[@name]";
    }

    public void parser(InputStream file) {
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


    public Map<String, ArrayList<String>> getTaggedClasses() {
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
            this.taggedClasses.put(tagName, new ArrayList<>());
        }

        this.taggedClasses.get(tagName).add(className);
    }

    public Set<String> get() {
        return list;
    }

}