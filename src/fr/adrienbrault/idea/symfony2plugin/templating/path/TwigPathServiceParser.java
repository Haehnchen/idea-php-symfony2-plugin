package fr.adrienbrault.idea.symfony2plugin.templating.path;

import fr.adrienbrault.idea.symfony2plugin.util.service.AbstractServiceParser;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.InputStream;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TwigPathServiceParser extends AbstractServiceParser {

    protected TwigPathIndex twigPathIndex = new TwigPathIndex();

    @Override
    public String getXPathFilter() {
        return "/container/services/service[@id='twig.loader']//call[@method='addPath']";
    }

    public void parser(InputStream file) {
        NodeList nodeList = this.parserer(file);

        if(nodeList == null) {
            return;
        }

        for (int i = 0; i < nodeList.getLength(); i++) {
            Element node = (Element) nodeList.item(i);

            NodeList arguments = node.getElementsByTagName("argument");

            if(arguments.getLength() == 1) {
                this.twigPathIndex.addPath(new TwigPath(arguments.item(0).getTextContent()));
            } else if(arguments.getLength() == 2) {
                this.twigPathIndex.addPath(new TwigPath(arguments.item(0).getTextContent(), arguments.item(1).getTextContent()));
            }

        }

    }

    public TwigPathIndex getTwigPathIndex() {
        return twigPathIndex;
    }

}