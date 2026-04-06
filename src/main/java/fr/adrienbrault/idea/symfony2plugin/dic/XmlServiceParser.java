package fr.adrienbrault.idea.symfony2plugin.dic;

import fr.adrienbrault.idea.symfony2plugin.dic.container.ServiceInterface;
import fr.adrienbrault.idea.symfony2plugin.util.service.AbstractServiceParser;
import org.jetbrains.annotations.NotNull;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;

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

    public void parser(InputStream file, VirtualFile sourceFile, Project project) {
        try {
            ServiceMap parsedServiceMap = new ServiceMapParser().parse(file);
            Map<String, ServiceInterface> services = new LinkedHashMap<>();

            this.serviceMap.getServices().forEach(service -> services.put(service.getId(), service));
            parsedServiceMap.getServices().forEach(service -> services.put(service.getId(), service));

            this.serviceMap = new ServiceMap(services.values());
        } catch (SAXException | IOException | ParserConfigurationException ignored) {
        }
    }

    @NotNull
    public ServiceMap getServiceMap() {
        return serviceMap;
    }
}
