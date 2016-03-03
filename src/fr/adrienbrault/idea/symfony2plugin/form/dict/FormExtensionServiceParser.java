package fr.adrienbrault.idea.symfony2plugin.form.dict;

import fr.adrienbrault.idea.symfony2plugin.util.service.AbstractServiceParser;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.File;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FormExtensionServiceParser extends AbstractServiceParser {

    protected Map<String, String> formExtensions = new ConcurrentHashMap<String, String>();

    @Override
    public String getXPathFilter() {
        return "/container/services/service/tag[@name='form.type_extension']";
    }

    public void parser(InputStream file) {
        NodeList nodeList = this.parserer(file);

        if(nodeList == null) {
            return;
        }

        for (int i = 0; i < nodeList.getLength(); i++) {
            Element node = (Element) nodeList.item(i);
            formExtensions.put(node.getParentNode().getAttributes().getNamedItem("class").getTextContent(), node.getAttribute("alias"));
        }

    }

    public Map<String, String> getFormExtensions() {
        return this.formExtensions;
    }

}