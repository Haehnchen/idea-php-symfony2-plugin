package fr.adrienbrault.idea.symfony2plugin.webDeployment.utils;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.StreamUtil;
import com.jetbrains.plugins.webDeployment.ConnectionOwnerFactory;
import com.jetbrains.plugins.webDeployment.config.FileTransferConfig;
import com.jetbrains.plugins.webDeployment.config.PublishConfig;
import com.jetbrains.plugins.webDeployment.config.WebServerConfig;
import com.jetbrains.plugins.webDeployment.connections.RemoteConnection;
import com.jetbrains.plugins.webDeployment.connections.RemoteConnectionManager;
import fr.adrienbrault.idea.symfony2plugin.routing.webDeployment.RoutingRemoteFileStorage;
import fr.adrienbrault.idea.symfony2plugin.webDeployment.storage.RemoteFileStorageInterface;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class RemoteWebServerUtil {

    @NotNull
    public static RemoteFileStorageInterface[] getExtension() {
        return new RemoteFileStorageInterface[] {
            new RoutingRemoteFileStorage()
        };
    }

    public static void collectRemoteFiles(@NotNull Project project) {
        WebServerConfig defaultServer = PublishConfig.getInstance(project).findDefaultServer();
        if(defaultServer == null) {
            return;
        }

        RemoteConnection connection;
        try {
            connection = RemoteConnectionManager.getInstance().openConnection(ConnectionOwnerFactory.createConnectionOwner(project), "foo", defaultServer, FileTransferConfig.Origin.Default, null, null);
        } catch (FileSystemException e) {
            return;
        }

        for (RemoteFileStorageInterface fileStorage : RemoteWebServerUtil.getExtension()) {
            Collection<String> contents = new ArrayList<String>();

            for (Object s : fileStorage.files(project)) {

                FileObject file;
                try {
                    file = defaultServer.findFile(connection.getFileSystem(), new WebServerConfig.RemotePath((String) s));
                } catch (FileSystemException e) {
                    continue;
                }

                String content;
                try {
                    content = StreamUtil.readText(file.getContent().getInputStream(), "UTF-8");
                } catch (IOException e) {
                    continue;
                }

                if(StringUtils.isNotBlank(content)) {
                    contents.add(content);
                }
            }

            fileStorage.clear();
            fileStorage.build(project, contents);
        }

        connection.clone();
    }

}
