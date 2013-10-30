package fr.adrienbrault.idea.symfony2plugin.translation.parser;

import fr.adrienbrault.idea.symfony2plugin.translation.dict.DomainFileMap;
import fr.adrienbrault.idea.symfony2plugin.util.service.AbstractServiceParser;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DomainMappings extends AbstractServiceParser {

    protected ArrayList<DomainFileMap> domainFileMaps = new ArrayList<DomainFileMap>();

    @Override
    public String getXPathFilter() {
        return "/container/services/service[@class=\"Symfony\\Bundle\\FrameworkBundle\\Translation\\Translator\"]//call[@method=\"addResource\"]";
    }

    public void parser(File file) {
        NodeList nodeList = this.parserer(file);

        if(nodeList == null) {
            return;
        }

        for (int i = 0; i < nodeList.getLength(); i++) {
            Element node = (Element) nodeList.item(i);
            NodeList arguments = node.getElementsByTagName("argument");

            if(arguments.getLength() == 4) {
                this.domainFileMaps.add(new DomainFileMap(arguments.item(0).getTextContent(), arguments.item(1).getTextContent(), arguments.item(2).getTextContent(), arguments.item(3).getTextContent()));
            }
        }

    }

    public List<DomainFileMap> getDomainFileMaps() {
        return Collections.synchronizedList(domainFileMaps);
    }

}