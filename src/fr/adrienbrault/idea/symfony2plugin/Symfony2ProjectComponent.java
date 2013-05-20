package fr.adrienbrault.idea.symfony2plugin;

import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ContentIterator;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.jetbrains.php.PhpIndex;
import fr.adrienbrault.idea.symfony2plugin.config.component.parser.ParameterParser;
import fr.adrienbrault.idea.symfony2plugin.dic.ServiceMap;
import fr.adrienbrault.idea.symfony2plugin.dic.ServiceMapParser;
import fr.adrienbrault.idea.symfony2plugin.doctrine.component.EntityNamesParser;
import fr.adrienbrault.idea.symfony2plugin.routing.Route;
import fr.adrienbrault.idea.symfony2plugin.util.SymfonyBundleUtil;
import fr.adrienbrault.idea.symfony2plugin.util.dict.SymfonyBundle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Adrien Brault <adrien.brault@gmail.com>
 */
public class Symfony2ProjectComponent implements ProjectComponent {

    private Project project;

    private ServiceMap servicesMap;
    private Long servicesMapLastModified;

    private Map<String, Route> routes;
    private Long routesLastModified;

    private Long entityNamespacesMapLastModified;
    private Map<String, String> entityNamespaces;

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
    }

    public void projectClosed() {
        System.out.println("projectClosed");
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
        String defaultServiceMapFilePath = getPath(project, Settings.getInstance(project).pathToProjectContainer);

        File xmlFile = new File(defaultServiceMapFilePath);
        if (!xmlFile.exists()) {
            return new ServiceMap();
        }

        Long xmlFileLastModified = xmlFile.lastModified();
        if (xmlFileLastModified.equals(servicesMapLastModified)) {
            return servicesMap;
        }

        try {
            ServiceMapParser serviceMapParser = new ServiceMapParser();
            servicesMap = serviceMapParser.parse(xmlFile);
            servicesMapLastModified = xmlFileLastModified;

            return servicesMap;
        } catch (SAXException ignored) {
        } catch (IOException ignored) {
        } catch (ParserConfigurationException ignored) {
        }

        return new ServiceMap();
    }

    public Map<String, Route> getRoutes() {
        Map<String, Route> routes = new HashMap<String, Route>();

        String urlGeneratorPath = getPath(project, Settings.getInstance(project).pathToUrlGenerator);
        File urlGeneratorFile = new File(urlGeneratorPath);
        VirtualFile virtualUrlGeneratorFile = VfsUtil.findFileByIoFile(urlGeneratorFile, false);

        if (!urlGeneratorFile.exists()) {
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
            String controller = matcher.group(2).replace("\\\\", "\\");
            Route route = new Route(routeName, controller);
            routes.put(route.getName(), route);
        }

        this.routes = routes;
        this.routesLastModified = routesLastModified;

        return routes;
    }

    private Map<String, String> configParameter;
    private Long configParameterLastModified;
    public Map<String, String> getConfigParameter() {

        String defaultServiceMapFilePath = getPath(project, Settings.getInstance(project).pathToProjectContainer);

        File xmlFile = new File(defaultServiceMapFilePath);
        if (!xmlFile.exists()) {
            return new HashMap<String, String>();
        }

        // this is called async, so double check for configParameter and configParameterLastModified
        Long xmlFileLastModified = xmlFile.lastModified();
        if (configParameter != null && xmlFileLastModified.equals(configParameterLastModified)) {
            return configParameter;
        }

        configParameterLastModified = xmlFileLastModified;

        try {
            ParameterParser parser = new ParameterParser();
            return configParameter = parser.parse(xmlFile);
        } catch (Exception ignored) {
            return configParameter = new HashMap<String, String>();
        }
    }

    private String getPath(Project project, String path) {
        if (!FileUtil.isAbsolute(path)) { // Project relative path
            path = project.getBasePath() + "/" + path;
        }

        return path;
    }

    public Map<String, String> getEntityNamespacesMap() {

        String defaultServiceMapFilePath = getPath(project, Settings.getInstance(project).pathToProjectContainer);

        File xmlFile = new File(defaultServiceMapFilePath);
        if (!xmlFile.exists()) {
            return new HashMap<String, String>();
        }

        Long xmlFileLastModified = xmlFile.lastModified();
        if (xmlFileLastModified.equals(entityNamespacesMapLastModified)) {
            return entityNamespaces;
        }

        try {
            EntityNamesParser entityNamesParser = new EntityNamesParser();
            entityNamespaces = entityNamesParser.parse(xmlFile);
            entityNamespacesMapLastModified = xmlFileLastModified;

            return entityNamespaces;
        } catch (SAXException ignored) {
        } catch (IOException ignored) {
        } catch (ParserConfigurationException ignored) {
        }

        return new HashMap<String, String>();
    }

}
