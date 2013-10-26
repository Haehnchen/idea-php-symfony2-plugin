package fr.adrienbrault.idea.symfony2plugin;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import fr.adrienbrault.idea.symfony2plugin.dic.ContainerFile;
import fr.adrienbrault.idea.symfony2plugin.routing.Route;
import fr.adrienbrault.idea.symfony2plugin.routing.RouteHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.intellij.openapi.diagnostic.Logger;

/**
 * @author Adrien Brault <adrien.brault@gmail.com>
 */
public class Symfony2ProjectComponent implements ProjectComponent {

    public static String HELP_URL = "http://symfony2-plugin.espend.de/";

    final private static Logger LOG = Logger.getInstance("Symfony2-Plugin");

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
        // System.out.println("projectOpened");
        this.checkProject();
    }

    public void projectClosed() {
        // System.out.println("projectClosed");
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

    public Map<String, Route> getRoutes() {
        Map<String, Route> routes = new HashMap<String, Route>();

        String urlGeneratorPath = getPath(project, Settings.getInstance(project).pathToUrlGenerator);
        File urlGeneratorFile = new File(urlGeneratorPath);
        VirtualFile virtualUrlGeneratorFile = VfsUtil.findFileByIoFile(urlGeneratorFile, false);

        if (virtualUrlGeneratorFile == null || !urlGeneratorFile.exists()) {
            Symfony2ProjectComponent.getLogger().warn("missing routing: " + urlGeneratorFile.toString());
            return routes;
        }

        Long routesLastModified = urlGeneratorFile.lastModified();
        if (routesLastModified.equals(this.routesLastModified)) {
            return this.routes;
        }

        Symfony2ProjectComponent.getLogger().info("update routing: " + urlGeneratorFile.toString());

        try {
            routes = RouteHelper.getRoutes(VfsUtil.loadText(virtualUrlGeneratorFile));
        } catch (IOException e) {
            return routes;
        }

        this.routes = routes;
        this.routesLastModified = routesLastModified;

        return routes;
    }

    public ArrayList<File> getContainerFiles() {
        return this.getContainerFiles(true);
    }

    public ArrayList<File> getContainerFiles(boolean attachSetting) {

        List<ContainerFile> containerFiles = null;

        // provide a default list
        if(attachSetting) {
           containerFiles = Settings.getInstance(this.project).containerFiles;
        }

        if(containerFiles == null) {
            containerFiles = new ArrayList<ContainerFile>();
        }

        if(containerFiles.size() == 0) {
            Symfony2ProjectComponent.getLogger().info("no custom container files add default");
            containerFiles.add(new ContainerFile(Settings.DEFAULT_CONTAINER_PATH));
        }

        ArrayList<File> validFiles = new ArrayList<File>();
        for(ContainerFile containerFile : containerFiles) {
            if(containerFile.exists(this.project)) {
                validFiles.add(containerFile.getFile(this.project));
            } else {
                Symfony2ProjectComponent.getLogger().warn("container file not exists skipping: " + containerFile.getPath());
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

        if(!this.isEnabled()) {
            if(VfsUtil.findRelativeFile(this.project.getBaseDir(), "vendor") != null
                && VfsUtil.findRelativeFile(this.project.getBaseDir(), "app", "cache", "dev") != null
                && VfsUtil.findRelativeFile(this.project.getBaseDir(), "app", "cache", "prod") != null
                && VfsUtil.findRelativeFile(this.project.getBaseDir(), "vendor", "symfony", "symfony") != null
              ) {
                showInfoNotification("Looks like this a Symfony2 project. Enable the Symfony2 Plugin in Project Settings");
            }

            return;
        }

        if(this.getContainerFiles().size() == 0) {
            showInfoNotification("missing at least one container file");
            Symfony2ProjectComponent.getLogger().warn("missing at least one container file");
        }

        String urlGeneratorPath = getPath(project, Settings.getInstance(project).pathToUrlGenerator);
        File urlGeneratorFile = new File(urlGeneratorPath);
        if (!urlGeneratorFile.exists()) {
            showInfoNotification("missing routing file: " + urlGeneratorPath);
            Symfony2ProjectComponent.getLogger().warn("missing routing file: " + urlGeneratorPath);
        }

    }

    public static boolean isEnabled(Project project) {
        return Settings.getInstance(project).pluginEnabled;
    }

    public static boolean isEnabled(@Nullable PsiElement psiElement) {
        return psiElement != null && isEnabled(psiElement.getProject());
    }

}
