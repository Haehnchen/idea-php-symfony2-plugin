package fr.adrienbrault.idea.symfonyplugin.dic;

import fr.adrienbrault.idea.symfonyplugin.util.service.AbstractServiceParser;
import org.jetbrains.annotations.NotNull;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class XmlServiceParser extends AbstractServiceParser {

    @NotNull
    private ServiceMap serviceMap = new ServiceMap();

    @Override
    public String getXPathFilter() {
        return "";
    }

    public void parser(InputStream file) {
        try {
            this.serviceMap = new ServiceMapParser().parse(file);
        } catch (SAXException | IOException | ParserConfigurationException ignored) {
        }
    }

    @NotNull
    public ServiceMap getServiceMap() {
        return serviceMap;
    }
}