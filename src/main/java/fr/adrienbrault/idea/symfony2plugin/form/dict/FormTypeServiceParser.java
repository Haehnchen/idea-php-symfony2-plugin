package fr.adrienbrault.idea.symfony2plugin.form.dict;

import fr.adrienbrault.idea.symfony2plugin.util.service.AbstractServiceParser;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.InputStream;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class FormTypeServiceParser extends AbstractServiceParser {

    protected final FormTypeMap formTypeMap = new FormTypeMap();

    @Override
    public String getXPathFilter() {
        return "/container/services/service[@id='form.registry']//service[@class]/argument[@type='collection'][1]/argument[@key]";
    }

    public void parser(InputStream file) {
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