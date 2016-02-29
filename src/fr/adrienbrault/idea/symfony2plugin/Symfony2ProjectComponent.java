package fr.adrienbrault.idea.symfony2plugin;

import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.deployment.DeploymentUtil;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.io.StreamUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.impl.http.RemoteContentProvider;
import com.intellij.openapi.vfs.impl.http.RemoteFileManager;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.psi.PsiElement;
import com.intellij.remoteServer.ServerType;
import com.intellij.remoteServer.agent.RemoteAgentManager;
import com.intellij.remoteServer.configuration.RemoteServer;
import com.intellij.remoteServer.configuration.RemoteServerListener;
import com.intellij.remoteServer.configuration.RemoteServersManager;
import com.intellij.remoteServer.configuration.deployment.DeploymentConfiguration;
import com.intellij.remoteServer.configuration.deployment.DeploymentConfigurationManager;
import com.intellij.remoteServer.configuration.deployment.DeploymentSource;
import com.intellij.remoteServer.impl.configuration.RemoteServerImpl;
import com.intellij.remoteServer.impl.configuration.RemoteServerListConfigurableProvider;
import com.intellij.remoteServer.impl.runtime.clientLibrary.ClientLibraryManagerImpl;
import com.intellij.remoteServer.impl.runtime.ui.RemoteServersViewContributor;
import com.intellij.remoteServer.runtime.ServerConnection;
import com.intellij.remoteServer.runtime.ServerConnectionManager;
import com.intellij.remoteServer.runtime.ServerConnector;
import com.intellij.remoteServer.runtime.clientLibrary.ClientLibraryDescription;
import com.intellij.remoteServer.runtime.clientLibrary.ClientLibraryManager;
import com.intellij.remoteServer.runtime.deployment.DeploymentTask;
import com.intellij.remoteServer.runtime.deployment.ServerRuntimeInstance;
import com.intellij.remoteServer.runtime.ui.RemoteServersView;
import com.intellij.util.MessageBusUtil;
import com.intellij.util.messages.MessageBusConnection;
import com.intellij.util.messages.impl.MessageBusConnectionImpl;
import com.jetbrains.plugins.webDeployment.ConnectionOwnerFactory;
import com.jetbrains.plugins.webDeployment.actions.WebDeploymentDataKeys;
import com.jetbrains.plugins.webDeployment.config.FileTransferConfig;
import com.jetbrains.plugins.webDeployment.config.WebServerConfig;
import com.jetbrains.plugins.webDeployment.config.WebServersConfigManager;
import com.jetbrains.plugins.webDeployment.connections.RemoteConnection;
import com.jetbrains.plugins.webDeployment.connections.RemoteConnectionManager;
import fr.adrienbrault.idea.symfony2plugin.dic.ContainerFile;
import fr.adrienbrault.idea.symfony2plugin.extension.ServiceContainerLoader;
import fr.adrienbrault.idea.symfony2plugin.extension.ServiceContainerLoaderParameter;
import fr.adrienbrault.idea.symfony2plugin.routing.RouteHelper;
import fr.adrienbrault.idea.symfony2plugin.util.IdeHelper;
import fr.adrienbrault.idea.symfony2plugin.util.service.ServiceXmlParserFactory;
import fr.adrienbrault.idea.symfony2plugin.widget.SymfonyProfilerWidget;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.impl.DefaultFileSystemConfigBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.builtInWebServer.WebServerPathHandler;
import org.jetbrains.builtInWebServer.WebServerPathToFileManager;
import org.jetbrains.builtInWebServer.WebServerRootsProvider;

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

    public void projectOpened() {

        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                ApplicationManager.getApplication().invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        foo();
                    }
                });
            }
        }, 10000);






        this.checkProject();

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

    private void foo() {
        System.out.println("1111aaaaa");

        // RemoteFilePanel
        //RemoteContentProvider.DownloadingCallback
        project.getMessageBus().connect().subscribe(RemoteServerListener.TOPIC, new RemoteServerListener() {
            @Override
            public void serverAdded(@NotNull RemoteServer<?> server) {
                System.out.println(server);
            }

            @Override
            public void serverRemoved(@NotNull RemoteServer<?> server) {
                System.out.println(server);
            }
        });




        for (WebServerConfig webServerConfig : WebServersConfigManager.getInstance(project).getServers(false)) {
            RemoteConnection foo = null;
            try {
                foo = RemoteConnectionManager.getInstance().openConnection(ConnectionOwnerFactory.createConnectionOwner(project), "foo", webServerConfig, FileTransferConfig.Origin.Default, null, null);
                FileObject file = webServerConfig.findFile(foo.getFileSystem(), new WebServerConfig.RemotePath("composer.json"));
                System.out.println(StreamUtil.readText(file.getContent().getInputStream(), "UTF-8"));
            } catch (FileSystemException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }


        }

                /*
        //WebServerPathToFileManager.getInstance().findVirtualFile()
        for (ServerType serverType : ServerType.EP_NAME.getExtensions()) {
            for (Object Foo : RemoteServersManager.getInstance().getServers(serverType)) {
                System.out.println(Foo);
            }
        }

        for (ServerType serverType : ServerType.EP_NAME.getExtensions()) {
           // System.out.println(serverType.getId());
         //   System.out.println(serverType.getPresentableName());
        }

        //DeploymentConfigurationManager.getInstance(project).getDeploymentConfigurations(ServerType)

        for (RemoteServersViewContributor contributor : RemoteServersViewContributor.EP_NAME.getExtensions()) {
            contributor.
        }
        */

        //ClientLibraryManagerImpl.getInstance().download(new ClientLibraryDescription());
        /*
        for (RemoteServersViewContributor contributor : RemoteServersViewContributor.EP_NAME.getExtensions()) {

            System.out.println(contributor);
        }

        for (RemoteServer<?> remoteServer : RemoteServersManager.getInstance().getServers()) {
            System.out.println(remoteServer);
        }


        for (ServerConnection serverConnection : ServerConnectionManager.getInstance().getConnections()) {
            System.out.println(serverConnection);
        }


        RemoteAgentManager.getInstance().

            ServerConnectionManager.getInstance().getConnection().deploy(new DeploymentTask() {
            @NotNull
            @Override
            public DeploymentSource getSource() {
                return null;
            }

            @NotNull
            @Override
            public DeploymentConfiguration getConfiguration() {
                return null;
            }

            @NotNull
            @Override
            public Project getProject() {
                return null;
            }

            @Override
            public boolean isDebugMode() {
                return false;
            }

            @NotNull
            @Override
            public ExecutionEnvironment getExecutionEnvironment() {
                return null;
            }
        });
        ServerConnectionManager.getInstance().getConnection().connectIfNeeded(new ServerConnector.ConnectionCallback() {
            @Override
            public void connected(@NotNull ServerRuntimeInstance serverRuntimeInstance) {
                serverRuntimeInstance.depl
            }

            @Override
            public void errorOccurred(@NotNull String s) {

            }
        });
        */
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

}
