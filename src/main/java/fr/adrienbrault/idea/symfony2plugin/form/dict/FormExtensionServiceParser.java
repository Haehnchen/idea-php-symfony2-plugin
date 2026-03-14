package fr.adrienbrault.idea.symfony2plugin.form.dict;

import fr.adrienbrault.idea.symfony2plugin.util.service.AbstractServiceParser;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class FormExtensionServiceParser extends AbstractServiceParser {

    protected final Map<String, String> formExtensions = new ConcurrentHashMap<>();

    @Override
    public String getXPathFilter() {
        return "/container/services/service/tag[@name='form.type_extension']";
    }

    public void parser(InputStream file, VirtualFile sourceFile, Project project) {
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