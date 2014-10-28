package fr.adrienbrault.idea.symfony2plugin;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.psi.PsiElement;
import fr.adrienbrault.idea.symfony2plugin.dic.ContainerFile;
import fr.adrienbrault.idea.symfony2plugin.extension.ServiceContainerLoader;
import fr.adrienbrault.idea.symfony2plugin.extension.ServiceContainerLoaderParameter;
import fr.adrienbrault.idea.symfony2plugin.remote.RemoteStorage;
import fr.adrienbrault.idea.symfony2plugin.remote.provider.RouterProvider;
import fr.adrienbrault.idea.symfony2plugin.routing.Route;
import fr.adrienbrault.idea.symfony2plugin.routing.RouteHelper;
import fr.adrienbrault.idea.symfony2plugin.util.IdeHelper;
import fr.adrienbrault.idea.symfony2plugin.util.service.ServiceXmlParserFactory;
import fr.adrienbrault.idea.symfony2plugin.widget.SymfonyProfilerWidget;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Adrien Brault <adrien.brault@gmail.com>
 */
public class Symfony2ProjectComponent implements ProjectComponent {

    public static String HELP_URL = "http://symfony2-plugin.espend.de/";
    final private static Logger LOG = Logger.getInstance("Symfony2-Plugin");
    private static final ExtensionPointName<ServiceContainerLoader> SERVICE_CONTAINER_POINT_NAME = new ExtensionPointName<ServiceContainerLoader>("fr.adrienbrault.idea.symfony2plugin.extension.ServiceContainerLoader");

    private Project project;
    private Map<String, Route> routes;
    private Long routesLastModified;

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

    public void projectClosed() {
        ServiceXmlParserFactory.cleanInstance(project);
    }

    public static Logger getLogger() {
        return LOG;
    }

    public void showInfoNotification(String content) {
        Notification errorNotification = new Notification("Symfony2 Plugin", "Symfony2 Plugin", content, NotificationType.INFORMATION);
        Notifications.Bus.notify(errorNotification, this.project);
    }

    public boolean isEnabled() {
        return Settings.getInstance(project).pluginEnabled;
    }

    /**
     * Dont use is method anymore, RouteHelper provides several util methods in replacement
     */
    @Deprecated
    public Map<String, Route> getRoutes() {
        Map<String, Route> routes = new HashMap<String, Route>();

        if(Symfony2ApplicationSettings.getInstance().serverEnabled && RemoteStorage.getInstance(project).has(RouterProvider.class)) {
            RouterProvider routerProvider = RemoteStorage.getInstance(project).get(RouterProvider.class);
            if(routerProvider != null) {
                routes.putAll(routerProvider.getRoutes());
                return routes;
            }
        }

        String urlGeneratorPath = getPath(project, Settings.getInstance(project).pathToUrlGenerator);
        File urlGeneratorFile = new File(urlGeneratorPath);
        VirtualFile virtualUrlGeneratorFile = VfsUtil.findFileByIoFile(urlGeneratorFile, false);

        if (virtualUrlGeneratorFile == null || !urlGeneratorFile.exists()) {
            return routes;
        }

        Long routesLastModified = urlGeneratorFile.lastModified();
        if (routesLastModified.equals(this.routesLastModified)) {
            return this.routes;
        }

        Symfony2ProjectComponent.getLogger().info("update routing: " + urlGeneratorFile.toString());

        routes = RouteHelper.getRoutes(project, virtualUrlGeneratorFile);

        this.routes = routes;
        this.routesLastModified = routesLastModified;

        return routes;
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
            containerFiles.add(new ContainerFile(Settings.DEFAULT_CONTAINER_PATH));
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
                && VfsUtil.findRelativeFile(this.project.getBaseDir(), "app", "cache") != null
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
