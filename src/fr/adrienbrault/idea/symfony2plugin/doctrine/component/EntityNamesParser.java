package fr.adrienbrault.idea.symfony2plugin.doctrine.component;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import javax.xml.parsers.DocumentBuilder;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class EntityNamesParser {

    private DocumentBuilder documentBuilder;

    public EntityNamesParser() throws ParserConfigurationException {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        documentBuilder = dbFactory.newDocumentBuilder();
    }

    public Map<String, String> parse(InputStream stream) throws IOException, SAXException {
        return parse(documentBuilder.parse(stream));
    }

    public Map<String, String> parse(File file) throws IOException, SAXException {
        return parse(documentBuilder.parse(file));
    }

    public Map<String, String> parse(Document document) {
        Map<String, String> map = new HashMap<String, String>();

        // any way to this better?
        NodeList servicesNodes = document.getElementsByTagName("service");
        for (int i = 0; i < servicesNodes.getLength(); i++) {
            Element node = (Element) servicesNodes.item(i);

            // doctrine.orm.default_entity_manager
            // doctrine.orm.customer_entity_manager
            if (node.hasAttribute("class") && node.hasAttribute("id") && node.getAttribute("id").startsWith("doctrine.orm") && node.getAttribute("id").endsWith("_entity_manager")) {

                // <call method="setEntityNamespaces">
                NodeList calls = document.getElementsByTagName("call");
                for (int x = 0; x < calls.getLength(); x++) {

                    Element call = (Element) calls.item(x);
                    if (call.hasAttribute("method") && call.getAttribute("method").equals("setEntityNamespaces")) {

                        // <argument key="HomeBundle">HomeBundle\Entity</argument>
                        NodeList arguments = call.getElementsByTagName("argument");
                        for (int y = 0; y < arguments.getLength(); y++) {
                            Element arg = (Element) arguments.item(y);
                            if (arg.hasAttribute("key")) {
                                map.put(arg.getAttribute("key"), "\\" + arg.getTextContent());
                            }
                        }
                    }

                }
            }
        }

        return map;
    }

}
