package fr.adrienbrault.idea.symfony2plugin.dic;

import fr.adrienbrault.idea.symfony2plugin.util.service.AbstractServiceParser;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;

public class XmlServiceParser extends AbstractServiceParser {

    protected ServiceMap serviceMap = new ServiceMap();

    @Override
    public String getXPathFilter() {
        return "";
    }

    public void parser(File file) {
        try {
            this.serviceMap = new ServiceMapParser().parse(file);
        } catch (SAXException ignored) {
        } catch (IOException ignored) {
        } catch (ParserConfigurationException ignored) {
        }
    }

    public ServiceMap getServiceMap() {
        return serviceMap;
    }

}