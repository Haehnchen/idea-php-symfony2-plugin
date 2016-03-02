package fr.adrienbrault.idea.symfony2plugin.dic.webDeployment;

import com.intellij.openapi.project.Project;
import com.intellij.util.containers.HashMap;
import fr.adrienbrault.idea.symfony2plugin.Settings;
import fr.adrienbrault.idea.symfony2plugin.config.component.parser.ParameterServiceCollector;
import fr.adrienbrault.idea.symfony2plugin.dic.ServiceMapParser;
import fr.adrienbrault.idea.symfony2plugin.dic.webDeployment.dict.ServiceParameterStorage;
import fr.adrienbrault.idea.symfony2plugin.webDeployment.storage.RemoteFileStorageInterface;
import fr.adrienbrault.idea.symfony2plugin.webDeployment.utils.RemoteWebServerUtil;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.jetbrains.annotations.NotNull;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ServiceContainerRemoteFileStorage implements RemoteFileStorageInterface<ServiceParameterStorage> {

    private ServiceParameterStorage storage = new ServiceParameterStorage(
        Collections.<String, String>emptyMap(),
        Collections.<String, String>emptyMap()
    );

    @NotNull
    @Override
    public Collection<String> files(@NotNull Project project) {
        return RemoteWebServerUtil.getRemoteAbleFiles(Settings.getInstance(project).containerFiles);
    }

    @Override
    public void build(@NotNull Project project, @NotNull Collection<FileObject> fileObjects) {

        Map<String, String> serviceMap = new HashMap<String, String>();
        Map<String, String> parameterMap = new HashMap<String, String>();

        for (FileObject fileObject : fileObjects) {
            try {
                serviceMap.putAll(
                    new ServiceMapParser().parse(fileObject.getContent().getInputStream()).getMap()
                );
            } catch (ParserConfigurationException ignored) {
            } catch (FileSystemException ignored) {
            } catch (SAXException ignored) {
            } catch (IOException ignored) {
            }

            try {
                parameterMap.putAll(ParameterServiceCollector.collect(fileObject.getContent().getInputStream()));
            } catch (FileSystemException ignored) {
            }
        }

        storage = new ServiceParameterStorage(serviceMap, parameterMap);
    }

    @NotNull
    @Override
    public ServiceParameterStorage getState() {
        return storage;
    }

    @Override
    public void clear() {
        storage = new ServiceParameterStorage(
            Collections.<String, String>emptyMap(),
            Collections.<String, String>emptyMap()
        );
    }
}


