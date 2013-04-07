package fr.adrienbrault.idea.symfony2plugin;

import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Adrien Brault <adrien.brault@gmail.com>
 */
public class ServiceMapParser {

    private DocumentBuilder documentBuilder;

    public ServiceMapParser() throws ParserConfigurationException {
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

        NodeList servicesNodes = document.getElementsByTagName("service");
        for (int i = 0; i < servicesNodes.getLength(); i++) {
            Element node = (Element) servicesNodes.item(i);
            if (node.hasAttribute("class") && node.hasAttribute("id")) {
                map.put(node.getAttribute("id"), "\\" + node.getAttribute("class"));
            }
        }

        // Support services whose class isn't specified
        map.put("request", "\\Symfony\\Component\\HttpFoundation\\Request");
        map.put("service_container", "\\Symfony\\Component\\DependencyInjection\\ContainerInterface");
        map.put("kernel", "\\Symfony\\Component\\HttpKernel\\KernelInterface");

        return map;
    }

}
