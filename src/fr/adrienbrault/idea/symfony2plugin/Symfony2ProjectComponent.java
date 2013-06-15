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
import fr.adrienbrault.idea.symfony2plugin.config.component.parser.ParameterServiceParser;
import fr.adrienbrault.idea.symfony2plugin.dic.ServiceMap;
import fr.adrienbrault.idea.symfony2plugin.dic.XmlServiceParser;
import fr.adrienbrault.idea.symfony2plugin.doctrine.component.EntityNamesServiceParser;
import fr.adrienbrault.idea.symfony2plugin.routing.Route;
import fr.adrienbrault.idea.symfony2plugin.util.service.ServiceXmlParserFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Adrien Brault <adrien.brault@gmail.com>
 */
public class Symfony2ProjectComponent implements ProjectComponent {

    private Project project;

    private Map<String, Route> routes;
    private Long routesLastModified;

    public static boolean isIndexing() {
        return isInStackTrace("StubUpdatingIndex");
    }

    public static boolean isInStackTrace(String search)    {
        StackTraceElement[] stackTrace = new Throwable().getStackTrace(); // Thread.currentThread().getStackTrace()

        for(StackTraceElement stackElement : stackTrace){
            String codeFrom = stackElement.toString();
            stackElement.getClassName();
            if(codeFrom.contains(search)){
                return true;
            }
        }

        return false;
    }

    public Symfony2ProjectComponent(Project project) {
        this.project = project;
    }

    public void initComponent() {
        System.out.println("initComponent");
    }

    public void disposeComponent() {
        System.out.println("disposeComponent");
    }

    @NotNull
    public String getComponentName() {
        return "Symfony2ProjectComponent";
    }

    public void projectOpened() {
        System.out.println("projectOpened");
        this.checkProject();
    }

    public void projectClosed() {
        System.out.println("projectClosed");
    }

    public void showInfoNotification(String content) {
        Notification errorNotification = new Notification("Symfony2 Plugin", "Symfony2 Plugin", content, NotificationType.INFORMATION);
        Notifications.Bus.notify(errorNotification, this.project);
    }

    public boolean isEnabled() {
        return Settings.getInstance(project).pluginEnabled;
    }

    @Nullable
    public File getPathToProjectContainer() {
        File projectContainer = new File(getPath(project, Settings.getInstance(project).pathToProjectContainer));

        if (!projectContainer.exists()) {
            return null;
        }

        return projectContainer;
    }

    public ServiceMap getServicesMap() {
        return ServiceXmlParserFactory.getInstance(this.project, XmlServiceParser.class).getServiceMap();
    }

    public Map<String, Route> getRoutes() {
        Map<String, Route> routes = new HashMap<String, Route>();

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

        Matcher matcher = null;
        try {
            // Damn, what has he done!
            // Right ?
            // If you find a better way to parse the file ... submit a PR :P
            matcher = Pattern.compile("'((?:[^'\\\\]|\\\\.)*)' => [^\\n]+'_controller' => '((?:[^'\\\\]|\\\\.)*)'[^\\n]+\n").matcher(VfsUtil.loadText(virtualUrlGeneratorFile));
        } catch (IOException ignored) {
        }

        if (null == matcher) {
            return routes;
        }

        while (matcher.find()) {
            String routeName = matcher.group(1);
            // dont add _assetic_04d92f8, _assetic_04d92f8_0
            if(!routeName.matches("_assetic_[0-9a-z]+[_\\d+]*")) {
                String controller = matcher.group(2).replace("\\\\", "\\");
                Route route = new Route(routeName, controller);
                routes.put(route.getName(), route);
            }
        }

        this.routes = routes;
        this.routesLastModified = routesLastModified;

        return routes;
    }

    public Map<String, String> getConfigParameter() {
        return ServiceXmlParserFactory.getInstance(this.project, ParameterServiceParser.class).getParameterMap();
    }

    private String getPath(Project project, String path) {
        if (!FileUtil.isAbsolute(path)) { // Project relative path
            path = project.getBasePath() + "/" + path;
        }

        return path;
    }

    public Map<String, String> getEntityNamespacesMap() {
        return ServiceXmlParserFactory.getInstance(this.project, EntityNamesServiceParser.class).getEntityNameMap();
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

        if(getPathToProjectContainer() == null) {
            showInfoNotification("missing container file: " + getPath(project, Settings.getInstance(project).pathToProjectContainer));
        }

        String urlGeneratorPath = getPath(project, Settings.getInstance(project).pathToUrlGenerator);
        File urlGeneratorFile = new File(urlGeneratorPath);
        if (!urlGeneratorFile.exists()) {
            showInfoNotification("missing routing file: " + urlGeneratorPath);
        }

    }

    public static boolean isEnabled(Project project) {
        return Settings.getInstance(project).pluginEnabled;
    }

    public static boolean isEnabled(@Nullable PsiElement psiElement) {
        return psiElement != null && isEnabled(psiElement.getProject());
    }

}
