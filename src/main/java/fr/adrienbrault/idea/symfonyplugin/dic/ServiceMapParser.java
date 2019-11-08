package fr.adrienbrault.idea.symfony2plugin.dic;

import fr.adrienbrault.idea.symfony2plugin.dic.container.ServiceInterface;
import fr.adrienbrault.idea.symfony2plugin.dic.container.XmlService;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
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
 * @author Daniel Espendiller <daniel@espendiller.net>
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
        NodeList servicesNodes = document.getElementsByTagName("service");

        Map<String, ServiceInterface> services = new HashMap<>();
        Map<String, ServiceInterface> aliases = new HashMap<>();

        for (int i = 0; i < servicesNodes.getLength(); i++) {
            Node node = servicesNodes.item(i);
            if(!(node instanceof Element)) {
                continue;
            }

            // invalid service
            XmlService service = XmlService.createFromXml((Element) node);
            if(service == null) {
                continue;
            }

            if(service.getAlias() == null) {
                services.put(service.getId(), service);
            } else {
                aliases.put(service.getId(), service);
            }
        }

        // resolve alias, as xml as a fully validated stated
        // all alias are valid per file
        aliases.values().forEach(service -> {
            ServiceInterface serviceAlias = services.get(service.getAlias());
            if(serviceAlias != null) {
                String className = serviceAlias.getClassName();
                if(className != null) {
                    XmlService v = XmlService.create(
                        service.getId(),
                        className,
                        service.isPublic()
                    );
                    services.put(service.getId(), v);
                }
            }
        });

        return new ServiceMap(services.values());
    }
}
