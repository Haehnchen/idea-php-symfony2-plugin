package fr.adrienbrault.idea.symfony2plugin.webDeployment.utils;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Condition;
import com.intellij.util.Function;
import com.intellij.util.containers.*;
import com.intellij.util.containers.HashMap;
import com.jetbrains.plugins.webDeployment.ConnectionOwnerFactory;
import com.jetbrains.plugins.webDeployment.config.FileTransferConfig;
import com.jetbrains.plugins.webDeployment.config.PublishConfig;
import com.jetbrains.plugins.webDeployment.config.WebServerConfig;
import com.jetbrains.plugins.webDeployment.connections.RemoteConnection;
import com.jetbrains.plugins.webDeployment.connections.RemoteConnectionManager;
import fr.adrienbrault.idea.symfony2plugin.dic.webDeployment.ServiceContainerRemoteFileStorage;
import fr.adrienbrault.idea.symfony2plugin.routing.webDeployment.RoutingRemoteFileStorage;
import fr.adrienbrault.idea.symfony2plugin.ui.dict.UiFilePathInterface;
import fr.adrienbrault.idea.symfony2plugin.webDeployment.storage.RemoteFileStorageInterface;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class RemoteWebServerUtil {

    public static Map<Project, RemoteFileStorageInterface[]> STORAGE_INSTANCES = new ConcurrentHashMap<Project, RemoteFileStorageInterface[]>();

    @NotNull
    public synchronized static RemoteFileStorageInterface[] getExtension(@NotNull Project project) {
        if(STORAGE_INSTANCES.containsKey(project)) {
            return STORAGE_INSTANCES.get(project);
        }

        STORAGE_INSTANCES.put(project, new RemoteFileStorageInterface[] {
            new ServiceContainerRemoteFileStorage(),
            new RoutingRemoteFileStorage(),
        });

        return STORAGE_INSTANCES.get(project);
    }

    @Nullable
    public static <T> T getExtensionInstance(@Nullable Project project, @NotNull Class<T> aClass) {
        if(!STORAGE_INSTANCES.containsKey(project)) {
            return null;
        }

        for (RemoteFileStorageInterface remoteFileStorage : STORAGE_INSTANCES.get(project)) {
            if(aClass.isInstance(remoteFileStorage)) {
                return (T) remoteFileStorage;
            }
        }

        return null;
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

        for (RemoteFileStorageInterface fileStorage : RemoteWebServerUtil.getExtension(project)) {
            Collection<FileObject> contents = new ArrayList<FileObject>();

            for (Object s : fileStorage.files(project)) {

                FileObject file;
                try {
                    file = defaultServer.findFile(connection.getFileSystem(), new WebServerConfig.RemotePath((String) s));
                } catch (FileSystemException e) {
                    continue;
                }

                contents.add(file);
            }

            fileStorage.clear();
            fileStorage.build(project, contents);
        }

        connection.clone();
    }

    @NotNull
    public static Collection<String> getRemoteAbleFiles(@Nullable List<? extends UiFilePathInterface> files) {
        if(files == null) {
            return Collections.emptyList();
        }

        return ContainerUtil.map(ContainerUtil.filter(files, new Condition<UiFilePathInterface>() {
            @Override
            public boolean value(UiFilePathInterface routingFile) {
                return routingFile.isRemote();
            }
        }), new Function<UiFilePathInterface, String>() {
            @Override
            public String fun(UiFilePathInterface routingFile) {
                return routingFile.getPath().substring("remote://".length());
            }
        });
    }


}
