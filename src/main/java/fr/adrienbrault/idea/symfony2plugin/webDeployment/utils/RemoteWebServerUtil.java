package fr.adrienbrault.idea.symfony2plugin.webDeployment.utils;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.Pair;
import com.intellij.ssh.interaction.ConnectionOwnerFactory;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import com.jetbrains.plugins.webDeployment.PublishUtils;
import com.jetbrains.plugins.webDeployment.config.*;
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

    public static Map<Project, RemoteFileStorageInterface[]> STORAGE_INSTANCES = new ConcurrentHashMap<>();

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

    public static void collectRemoteFiles(final @NotNull Project project) {
        WebServerConfig defaultServer = findDefaultServer(project);
        if(defaultServer == null) {
            return;
        }

        Deployable deployable = Deployable.create(defaultServer, project);
        RemoteConnection connection;
        try {
            connection = RemoteConnectionManager.getInstance().openConnection(ConnectionOwnerFactory.createConnectionOwnerWithDialogMessages(project), "foo", deployable, FileTransferConfig.Origin.Default, null, null);
        } catch (FileSystemException e) {
            return;
        }

        for (final RemoteFileStorageInterface fileStorage : RemoteWebServerUtil.getExtension(project)) {
            final Collection<FileObject> contents = new ArrayList<>();

            for (Object s : fileStorage.files(project)) {

                FileObject file;
                try {
                    file = PublishUtils.findFile(connection, new WebServerConfig.RemotePath((String) s), deployable);
                } catch (FileSystemException e) {
                    continue;
                }

                contents.add(file);
            }

            fileStorage.clear();

            ApplicationManager.getApplication().runReadAction(() -> fileStorage.build(project, contents));
        }

        connection.clone();
    }

    /**
     * replacement for "com.jetbrains.plugins.webDeployment.config.PublishConfig#findDefaultServer()"
     */
    @Nullable
    public static WebServerConfig findDefaultServer(@NotNull Project project) {
        Pair<WebServerGroupingWrap, WebServerConfig> server = PublishConfig.getInstance(project).findDefaultServerOrGroup();
        return server == null ? null : server.getSecond();
    }

    @NotNull
    public static Collection<String> getRemoteAbleFiles(@Nullable List<? extends UiFilePathInterface> files) {
        if(files == null) {
            return Collections.emptyList();
        }

        return ContainerUtil.map(ContainerUtil.filter(
            files,
            (Condition<UiFilePathInterface>) UiFilePathInterface::isRemote),
            (Function<UiFilePathInterface, String>) routingFile -> routingFile.getPath().substring("remote://".length())
        );
    }

    public static boolean hasConfiguredRemoteFile(@NotNull Project project) {
        for (RemoteFileStorageInterface remoteFileStorage : getExtension(project)) {
            if(remoteFileStorage.files(project).size() > 0) {
                return true;
            }
        }

        return false;
    }
}
