package fr.adrienbrault.idea.symfony2plugin.form.dict;

import fr.adrienbrault.idea.symfony2plugin.util.service.AbstractServiceParser;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.File;
import java.util.HashMap;

public class FormExtensionServiceParser extends AbstractServiceParser {

    protected HashMap<String, String> formExtensions = new HashMap<String, String>();

    @Override
    public String getXPathFilter() {
        return "/container/services/service/tag[@name='form.type_extension']";
    }

    public void parser(File file) {
        NodeList nodeList = this.parserer(file);

        if(nodeList == null) {
            return;
        }

        for (int i = 0; i < nodeList.getLength(); i++) {
            Element node = (Element) nodeList.item(i);
            formExtensions.put(node.getParentNode().getAttributes().getNamedItem("class").getTextContent(), node.getAttribute("alias"));
        }

    }

    public HashMap<String, String> getFormExtensions() {
        return this.formExtensions;
    }

}