package fr.adrienbrault.idea.symfony2plugin.templating.path;

import fr.adrienbrault.idea.symfony2plugin.util.service.AbstractServiceParser;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.File;

public class TwigPathServiceParser extends AbstractServiceParser {

    @Override
    public String getXPathFilter() {
        return "/container/services/service[@id='twig.loader']//call[@method='addPath']";
    }

    public TwigPathIndex parser(File file) {
        NodeList nodeList = this.parserer(file);

        TwigPathIndex twigPathIndex = new TwigPathIndex();

        if(nodeList == null) {
            return twigPathIndex;
        }

        for (int i = 0; i < nodeList.getLength(); i++) {
            Element node = (Element) nodeList.item(i);

            NodeList arguments = node.getElementsByTagName("argument");

            if(arguments.getLength() == 1) {
                twigPathIndex.addPath(new TwigPath(arguments.item(0).getTextContent()));
            } else if(arguments.getLength() == 2) {
                twigPathIndex.addPath(new TwigPath(arguments.item(0).getTextContent(), arguments.item(1).getTextContent()));
            }

        }

        return twigPathIndex;
    }

}