package fr.adrienbrault.idea.symfony2plugin.dic;

import com.intellij.openapi.vfs.VirtualFile;
import fr.adrienbrault.idea.symfony2plugin.util.service.AbstractServiceParser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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

    @Override
    public void parser(@NotNull InputStream inputStream, @Nullable VirtualFile sourceFile) {
        try {
            this.serviceMap = new ServiceMapParser().parse(inputStream);
        } catch (SAXException | IOException | ParserConfigurationException ignored) {
        }
    }

    @NotNull
    public ServiceMap getServiceMap() {
        return serviceMap;
    }
}