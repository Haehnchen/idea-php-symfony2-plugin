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
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.psi.PsiElement;
import fr.adrienbrault.idea.symfony2plugin.dic.ContainerFile;
import fr.adrienbrault.idea.symfony2plugin.extension.ServiceContainerLoader;
import fr.adrienbrault.idea.symfony2plugin.extension.ServiceContainerLoaderParameter;
import fr.adrienbrault.idea.symfony2plugin.profiler.widget.SymfonyProfilerWidget;
import fr.adrienbrault.idea.symfony2plugin.routing.RouteHelper;
import fr.adrienbrault.idea.symfony2plugin.util.IdeHelper;
import fr.adrienbrault.idea.symfony2plugin.util.service.ServiceXmlParserFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;

/**
 * @author Adrien Brault <adrien.brault@gmail.com>
 */
public class Symfony2ProjectComponent implements ProjectComponent {

    public static String HELP_URL = "http://symfony2-plugin.espend.de/";
    final private static Logger LOG = Logger.getInstance("Symfony-Plugin");
    private static final ExtensionPointName<ServiceContainerLoader> SERVICE_CONTAINER_POINT_NAME = new ExtensionPointName<>("fr.adrienbrault.idea.symfony2plugin.extension.ServiceContainerLoader");

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
        this.checkProject();

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

    public Collection<File> getContainerFiles() {
        Collection<ContainerFile> containerFiles = new ArrayList<>();

        ServiceContainerLoaderParameter containerLoaderExtensionParameter = new ServiceContainerLoaderParameter(project, containerFiles);
        for(ServiceContainerLoader loaderExtension : SERVICE_CONTAINER_POINT_NAME.getExtensions()) {
            loaderExtension.attachContainerFile(containerLoaderExtensionParameter);
        }

        if(containerFiles.size() == 0) {
            for (String s : Settings.DEFAULT_CONTAINER_PATHS) {
                containerFiles.add(new ContainerFile(s));
            }
        }

        Collection<File> validFiles = new ArrayList<>();
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
