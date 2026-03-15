package fr.adrienbrault.idea.symfony2plugin.doctrine.component;

import com.intellij.openapi.vfs.VirtualFile;
import fr.adrienbrault.idea.symfony2plugin.util.service.AbstractServiceParser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class DocumentNamespacesParser extends AbstractServiceParser {

    protected final Map<String, String> entityNameMap = new ConcurrentHashMap<>();

    @Override
    public String getXPathFilter() {
        return "/container/services/service[@id[starts-with(.,'doctrine_mongodb.odm.')] or @id[starts-with(.,'doctrine_couchdb.odm.')]]//call[@method='setDocumentNamespaces']//argument[@key]";
    }

    @Override
    public void parser(@NotNull InputStream inputStream, @Nullable VirtualFile sourceFile) {
        NodeList nodeList = this.parserer(inputStream);

        if(nodeList == null) {
            return;
        }

        for (int i = 0; i < nodeList.getLength(); i++) {
            Element node = (Element) nodeList.item(i);
            this.entityNameMap.put(node.getAttribute("key"), "\\" + node.getTextContent());
        }

    }

    public Map<String, String> getNamespaceMap() {
        return entityNameMap;
    }

}