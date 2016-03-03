package fr.adrienbrault.idea.symfony2plugin.dic;

import fr.adrienbrault.idea.symfony2plugin.util.service.AbstractServiceParser;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

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
        } catch (SAXException ignored) {
        } catch (IOException ignored) {
        } catch (ParserConfigurationException ignored) {
        }
    }

    public ServiceMap getServiceMap() {
        return serviceMap;
    }

}