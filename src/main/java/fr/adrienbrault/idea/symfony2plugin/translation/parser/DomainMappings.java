package fr.adrienbrault.idea.symfony2plugin.translation.parser;

import fr.adrienbrault.idea.symfony2plugin.translation.dict.DomainFileMap;
import fr.adrienbrault.idea.symfony2plugin.util.service.AbstractServiceParser;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class DomainMappings extends AbstractServiceParser {

    protected List<DomainFileMap> domainFileMaps = new CopyOnWriteArrayList<>();

    @Override
    public String getXPathFilter() {
        return
            "/container/services/service[@class=\"Symfony\\Bundle\\FrameworkBundle\\Translation\\Translator\"]//call[@method=\"addResource\"]" // Symfony < 4
                + " | /container/services/service[@class=\"Symfony\\Bundle\\FrameworkBundle\\Translation\\Translator\"]//argument[@key=\"resource_files\"]/argument/argument";
    }

    public void parser(InputStream file) {

        NodeList nodeList = this.parserer(file);

        if(nodeList == null) {
            return;
        }

        for (int i = 0; i < nodeList.getLength(); i++) {
            Element node = (Element) nodeList.item(i);
            String tagName = node.getTagName();

            if ("call".equals(tagName)) {
                // Symfony < 4: via addResource
                NodeList arguments = node.getElementsByTagName("argument");

                if(arguments.getLength() == 4) {
                    this.domainFileMaps.add(new DomainFileMap(arguments.item(0).getTextContent(), arguments.item(1).getTextContent(), arguments.item(2).getTextContent(), arguments.item(3).getTextContent()));
                }
            } else if("argument".equals(tagName)) {
                // Symfony 5: arguments in constructor

                // normalize path name
                String path = node.getTextContent().replace("\\", "/");

                // get file name
                int filenameStart = path.lastIndexOf("/");
                if (filenameStart > 0) {
                    String filename = path.substring(filenameStart + 1);

                    // split by filename: validators.af.xlf
                    String[] split = filename.split("\\.");
                    if (split.length == 3) {
                        this.domainFileMaps.add(new DomainFileMap(split[2], path, split[1], split[0]));
                    }
                }
            }
        }
    }

    public Collection<DomainFileMap> getDomainFileMaps() {
        return Collections.synchronizedList(domainFileMaps);
    }
}