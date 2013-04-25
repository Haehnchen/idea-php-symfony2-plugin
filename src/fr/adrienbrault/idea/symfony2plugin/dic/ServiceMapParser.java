package fr.adrienbrault.idea.symfony2plugin.dic;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
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

    public ServiceMap parse(InputStream stream) throws IOException, SAXException {
        return parse(documentBuilder.parse(stream));
    }

    public ServiceMap parse(File file) throws IOException, SAXException {
        return parse(documentBuilder.parse(file));
    }

    public ServiceMap parse(Document document) {
        Map<String, String> map = new HashMap<String, String>();
        Map<String, String> publicMap = new HashMap<String, String>();

        NodeList servicesNodes = document.getElementsByTagName("service");
        for (int i = 0; i < servicesNodes.getLength(); i++) {
            Element node = (Element) servicesNodes.item(i);
            if (node.hasAttribute("class") && node.hasAttribute("id")) {
                map.put(node.getAttribute("id"), "\\" + node.getAttribute("class"));
            }
            if (!(node.hasAttribute("public") && node.getAttribute("public").equals("false"))) {
                publicMap.put(node.getAttribute("id"), "\\" + node.getAttribute("class"));
            }
            if (node.hasAttribute("alias") && publicMap.get(node.getAttribute("alias")) != null) {
                map.put(node.getAttribute("id"), map.get(node.getAttribute("alias")));
                publicMap.put(node.getAttribute("id"), map.get(node.getAttribute("alias")));
            }
        }

        // Support services whose class isn't specified
        populateMapWithDefaultServices(map);
        populateMapWithDefaultServices(publicMap);

        return new ServiceMap(map, publicMap);
    }

    private void populateMapWithDefaultServices(Map<String, String> map) {
        map.put("request", "\\Symfony\\Component\\HttpFoundation\\Request");
        map.put("service_container", "\\Symfony\\Component\\DependencyInjection\\ContainerInterface");
        map.put("kernel", "\\Symfony\\Component\\HttpKernel\\KernelInterface");
    }

}
