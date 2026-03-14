package fr.adrienbrault.idea.symfony2plugin.doctrine.component;

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
public class EntityNamesServiceParser extends AbstractServiceParser {

    protected final Map<String, String> entityNameMap = new ConcurrentHashMap<>();

    @Override
    public String getXPathFilter() {
        return "/container/services/service[@id[starts-with(.,'doctrine.orm.')]]//call[@method='setEntityNamespaces']//argument[@key]";
    }

    public void parser(InputStream file, VirtualFile sourceFile, Project project) {
        NodeList nodeList = this.parserer(file);

        if(nodeList == null) {
            return;
        }

        for (int i = 0; i < nodeList.getLength(); i++) {
            Element node = (Element) nodeList.item(i);
            this.entityNameMap.put(node.getAttribute("key"), "\\" + node.getTextContent());
        }

    }

    public Map<String, String> getEntityNameMap() {
        return entityNameMap;
    }

}