package fr.adrienbrault.idea.symfony2plugin.dic.webDeployment;

import com.intellij.openapi.project.Project;
import com.intellij.util.containers.HashMap;
import fr.adrienbrault.idea.symfony2plugin.Settings;
import fr.adrienbrault.idea.symfony2plugin.dic.ServiceMapParser;
import fr.adrienbrault.idea.symfony2plugin.webDeployment.storage.RemoteFileStorageInterface;
import fr.adrienbrault.idea.symfony2plugin.webDeployment.utils.RemoteWebServerUtil;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.jetbrains.annotations.NotNull;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ServiceContainerRemoteFileStorage implements RemoteFileStorageInterface<Map<String, String>> {

    private Map<String, String> storage = new HashMap<String, String>();

    @NotNull
    @Override
    public Collection<String> files(@NotNull Project project) {
        return RemoteWebServerUtil.getRemoteAbleFiles(Settings.getInstance(project).containerFiles);
    }

    @Override
    public void build(@NotNull Project project, @NotNull Collection<FileObject> fileObjects) {

        Map<String, String> map = new HashMap<String, String>();

        for (FileObject fileObject : fileObjects) {
            try {
                map.putAll(
                    new ServiceMapParser().parse(fileObject.getContent().getInputStream()).getMap()
                );
            } catch (ParserConfigurationException ignored) {
            } catch (FileSystemException ignored) {
            } catch (SAXException ignored) {
            } catch (IOException ignored) {
            }
        }

        storage = map;
    }

    @NotNull
    @Override
    public Map<String, String> getState() {
        return storage;
    }

    @Override
    public void clear() {
        storage = new HashMap<String, String>();
    }
}


