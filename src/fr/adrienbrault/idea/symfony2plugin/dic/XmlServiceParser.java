package fr.adrienbrault.idea.symfony2plugin.dic;

import fr.adrienbrault.idea.symfony2plugin.util.service.AbstractServiceParser;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class XmlServiceParser extends AbstractServiceParser {

    protected ServiceMap serviceMap = new ServiceMap();

    @Override
    public String getXPathFilter() {
        return "";
    }

    public void parser(InputStream file) {
        try {
            // @TODO: make this one beautiful
            ServiceMap serviceMap1 = new ServiceMapParser().parse(file);
            this.serviceMap.getMap().putAll(serviceMap1.getMap());
            this.serviceMap.getPublicMap().putAll(serviceMap1.getPublicMap());
        } catch (SAXException | IOException | ParserConfigurationException ignored) {
        }
    }

    public ServiceMap getServiceMap() {
        return serviceMap;
    }

}