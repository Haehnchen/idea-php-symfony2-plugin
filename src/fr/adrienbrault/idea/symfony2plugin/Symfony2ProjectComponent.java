package fr.adrienbrault.idea.symfony2plugin;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.io.StreamUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFileFactory;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.HashMap;
import com.jetbrains.php.lang.PhpFileType;
import com.jetbrains.plugins.webDeployment.ConnectionOwnerFactory;
import com.jetbrains.plugins.webDeployment.config.FileTransferConfig;
import com.jetbrains.plugins.webDeployment.config.PublishConfig;
import com.jetbrains.plugins.webDeployment.config.WebServerConfig;
import com.jetbrains.plugins.webDeployment.connections.RemoteConnection;
import com.jetbrains.plugins.webDeployment.connections.RemoteConnectionManager;
import fr.adrienbrault.idea.symfony2plugin.dic.ContainerFile;
import fr.adrienbrault.idea.symfony2plugin.extension.ServiceContainerLoader;
import fr.adrienbrault.idea.symfony2plugin.extension.ServiceContainerLoaderParameter;
import fr.adrienbrault.idea.symfony2plugin.routing.Route;
import fr.adrienbrault.idea.symfony2plugin.routing.RouteHelper;
import fr.adrienbrault.idea.symfony2plugin.routing.dict.RoutingFile;
import fr.adrienbrault.idea.symfony2plugin.util.IdeHelper;
import fr.adrienbrault.idea.symfony2plugin.util.service.ServiceXmlParserFactory;
import fr.adrienbrault.idea.symfony2plugin.widget.SymfonyProfilerWidget;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * @author Adrien Brault <adrien.brault@gmail.com>
 */
public class Symfony2ProjectComponent implements ProjectComponent {

    public static String HELP_URL = "http://symfony2-plugin.espend.de/";
    final private static Logger LOG = Logger.getInstance("Symfony-Plugin");
    private static final ExtensionPointName<ServiceContainerLoader> SERVICE_CONTAINER_POINT_NAME = new ExtensionPointName<ServiceContainerLoader>("fr.adrienbrault.idea.symfony2plugin.extension.ServiceContainerLoader");

    private Project project;

    public Symfony2ProjectComponent(Project project) {
        this.project = project;
    }

    public void initComponent() {
        //System.out.println("initComponent");
    }

    public void disposeComponent() {
        //System.out.println("disposeComponent");
    }

    @NotNull
    public String getComponentName() {
        return "Symfony2ProjectComponent";
    }

    interface RemoteFileStorage<V> {
        Collection<String> files(@NotNull Project project);
        void build(@NotNull Project project, @NotNull Collection<String> content);
        V getState();
        void clear();
    }

    class RoutingRemoteFileStorage implements RemoteFileStorage<Map<String, Route>> {

        private Map<String, Route> routeMap = new HashMap<String, Route>();

        @Override
        public Collection<String> files(@NotNull Project project) {
            List<RoutingFile> routingFiles = Settings.getInstance(project).routingFiles;
            if(routingFiles == null) {
                return Collections.emptyList();
            }

            return ContainerUtil.map(ContainerUtil.filter(routingFiles, new Condition<RoutingFile>() {
                @Override
                public boolean value(RoutingFile routingFile) {
                    return routingFile.getPath().startsWith("remote://");
                }
            }), new Function<RoutingFile, String>() {
                @Override
                public String fun(RoutingFile routingFile) {
                    return routingFile.getPath().substring("remote://".length());
                }
            });
        }

        @Override
        public void build(@NotNull Project project, @NotNull Collection<String> content) {

            Map<String, Route> routeMap = new HashMap<String, Route>();

            for (String s : content) {
                routeMap.putAll(RouteHelper.getRoutesInsideUrlGeneratorFile(
                    PsiFileFactory.getInstance(project).createFileFromText("DUMMY__." + PhpFileType.INSTANCE.getDefaultExtension(), PhpFileType.INSTANCE, s)
                ));
            }

            this.routeMap = routeMap;
        }

        @NotNull
        public Map<String, Route> getState() {
            return this.routeMap = new HashMap<String, Route>();
        }

        @Override
        public void clear() {
            this.routeMap = new HashMap<String, Route>();
        }
    }

    public void projectOpened() {
        this.checkProject();

        final RemoteFileStorage[] remoteFileStorage = new RoutingRemoteFileStorage[] {
            new RoutingRemoteFileStorage()
        };

        new Timer().schedule(new MyTimerTask(remoteFileStorage), 1000, 5000);

        // phpstorm pre 7.1 dont support statusbar api;
        if(!IdeHelper.supportsStatusBar()) {
            return;
        }

        // attach toolbar popup (right bottom)
        StatusBar statusBar = WindowManager.getInstance().getStatusBar(this.project);
        if(statusBar == null) {
            return;
        }

        // clean bar on project open; we can have multiple projects att some time
        if(statusBar.getWidget(SymfonyProfilerWidget.ID) != null) {
            statusBar.removeWidget(SymfonyProfilerWidget.ID);
        }

        if(isEnabled()) {
            SymfonyProfilerWidget symfonyProfilerWidget = new SymfonyProfilerWidget(this.project);
            statusBar.addWidget(symfonyProfilerWidget);
        }

    }

    public void projectClosed() {

        ServiceXmlParserFactory.cleanInstance(project);

        // clean routing
        if(RouteHelper.COMPILED_CACHE.containsKey(project)) {
            RouteHelper.COMPILED_CACHE.remove(project);
        }
    }

    public static Logger getLogger() {
        return LOG;
    }

    public void showInfoNotification(String content) {
        Notification errorNotification = new Notification("Symfony Plugin", "Symfony Plugin", content, NotificationType.INFORMATION);
        Notifications.Bus.notify(errorNotification, this.project);
    }

    public boolean isEnabled() {
        return Settings.getInstance(project).pluginEnabled;
    }

    public List<File> getContainerFiles() {
        return this.getContainerFiles(true);
    }

    public List<File> getContainerFiles(boolean attachSetting) {

        List<ContainerFile> containerFiles = new ArrayList<ContainerFile>();

        ServiceContainerLoaderParameter containerLoaderExtensionParameter = new ServiceContainerLoaderParameter(project, containerFiles);
        for(ServiceContainerLoader loaderExtension : SERVICE_CONTAINER_POINT_NAME.getExtensions()) {
            loaderExtension.attachContainerFile(containerLoaderExtensionParameter);
        }

        if(containerFiles.size() == 0) {
            for (String s : Settings.DEFAULT_CONTAINER_PATHS) {
                containerFiles.add(new ContainerFile(s));
            }
        }

        List<File> validFiles = new ArrayList<File>();
        for(ContainerFile containerFile : containerFiles) {
            if(containerFile.exists(this.project)) {
                validFiles.add(containerFile.getFile(this.project));
            }
        }

        return validFiles;
    }

    private String getPath(Project project, String path) {
        if (!FileUtil.isAbsolute(path)) { // Project relative path
            path = project.getBasePath() + "/" + path;
        }

        return path;
    }

    private void checkProject() {

        if(!this.isEnabled() && !Settings.getInstance(project).dismissEnableNotification) {
            if(VfsUtil.findRelativeFile(this.project.getBaseDir(), "vendor") != null
                && VfsUtil.findRelativeFile(this.project.getBaseDir(), "app", "config") != null
                && VfsUtil.findRelativeFile(this.project.getBaseDir(), "app", "Resources") != null
                && VfsUtil.findRelativeFile(this.project.getBaseDir(), "vendor", "symfony", "symfony") != null
              ) {
                IdeHelper.notifyEnableMessage(project);
            }

            return;
        }

        if(this.getContainerFiles().size() == 0) {
            Symfony2ProjectComponent.getLogger().warn("missing at least one container file");
        }

        String urlGeneratorPath = getPath(project, Settings.getInstance(project).pathToUrlGenerator);
        File urlGeneratorFile = new File(urlGeneratorPath);
        if (!urlGeneratorFile.exists()) {
            Symfony2ProjectComponent.getLogger().warn("missing routing file: " + urlGeneratorPath);
        }

    }

    public static boolean isEnabled(@Nullable Project project) {
        return project != null && Settings.getInstance(project).pluginEnabled;
    }

    /**
     * If plugin is not enabled on first project start/indexing we will never get a filled
     * index until a forced cache rebuild, we check also for vendor path
     */
    public static boolean isEnabledForIndex(Project project) {

        if(Settings.getInstance(project).pluginEnabled) {
            return true;
        }

        if(VfsUtil.findRelativeFile(project.getBaseDir(), "vendor", "symfony") != null) {
            return true;
        }

        // drupal8; this should not really here
        if(VfsUtil.findRelativeFile(project.getBaseDir(), "core", "vendor", "symfony") != null) {
            return true;
        }

        return false;
    }

    public static boolean isEnabled(@Nullable PsiElement psiElement) {
        return psiElement != null && isEnabled(psiElement.getProject());
    }

    private class MyTimerTask extends TimerTask {
        private final RemoteFileStorage[] remoteFileStorage;

        public MyTimerTask(RemoteFileStorage[] remoteFileStorage) {
            this.remoteFileStorage = remoteFileStorage;
        }
        @Override
        public void run() {

            DumbService.getInstance(project).smartInvokeLater(new Runnable() {
                @Override
                public void run() {
                    ApplicationManager.getApplication().runReadAction(new Runnable() {
                        @Override
                        public void run() {
                            run1();
                        }
                    });
                }
            });

        }

        private void run1() {
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

            for (RemoteFileStorage fileStorage : remoteFileStorage) {
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
}
