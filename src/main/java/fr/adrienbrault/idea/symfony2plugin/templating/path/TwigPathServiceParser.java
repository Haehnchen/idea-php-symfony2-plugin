package fr.adrienbrault.idea.symfony2plugin.templating.path;

import fr.adrienbrault.idea.symfony2plugin.util.service.AbstractServiceParser;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.InputStream;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TwigPathServiceParser extends AbstractServiceParser {
    @NotNull
    private TwigPathIndex twigPathIndex = new TwigPathIndex();

    @Override
    public String getXPathFilter() {
        return "/container/services/service[@id[starts-with(.,'twig.loader')]]//call[@method='addPath']";
    }

    public synchronized void parser(InputStream file) {
        NodeList nodeList = this.parserer(file);

        if(nodeList == null) {
            return;
        }

        for (int i = 0; i < nodeList.getLength(); i++) {
            Element node = (Element) nodeList.item(i);

            NodeList arguments = node.getElementsByTagName("argument");

            if(arguments.getLength() == 1) {
                twigPathIndex.addPath(new TwigPath(arguments.item(0).getTextContent()));
            } else if(arguments.getLength() == 2) {
                String namespace = arguments.item(1).getTextContent();

                // we ignore overwrites; they are added also without "!", so just skip it
                if (!namespace.startsWith("!")) {
                    twigPathIndex.addPath(new TwigPath(arguments.item(0).getTextContent(), namespace));
                }
            }
        }
    }

    @NotNull
    public TwigPathIndex getTwigPathIndex() {
        return twigPathIndex;
    }
}