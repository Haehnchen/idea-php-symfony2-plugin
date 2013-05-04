package fr.adrienbrault.idea.symfony2plugin;

import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ContentIterator;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import fr.adrienbrault.idea.symfony2plugin.dic.ServiceMap;
import fr.adrienbrault.idea.symfony2plugin.dic.ServiceMapParser;
import fr.adrienbrault.idea.symfony2plugin.routing.Route;
import fr.adrienbrault.idea.symfony2plugin.translation.parser.TranslationStringMap;
import fr.adrienbrault.idea.symfony2plugin.translation.parser.TranslationStringParser;
import org.jetbrains.annotations.NotNull;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    private TranslationStringMap translationStringMap;
    private Long translationStringMapModified;

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

    public TranslationStringMap getTranslationMap() {

        // we are using this until we have some better cache

        File defaultServiceMapFilePath = new File(getPath(project, Settings.getInstance(project).pathToProjectContainer));

        if (!defaultServiceMapFilePath.exists()) {
            return new TranslationStringMap();
        }

        // root path of translation is our caching indicator
        File translationRootPath = new File(defaultServiceMapFilePath.getParentFile().getPath() + "/translations");
        if (!translationRootPath.exists()) {
            return new TranslationStringMap();
        }

        Long translationModified = translationRootPath.lastModified();
        if (translationModified.equals(translationStringMapModified)) {
            return translationStringMap;
        }

        translationStringMapModified = translationModified;
        return translationStringMap = new TranslationStringParser().parsePathMatcher(translationRootPath.getPath());
    }

    public ServiceMap getServicesMap() {
        if (null != servicesMap) {
            return servicesMap;
        }

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

    public List<VirtualFile> getAssetFiles() {
        final List<VirtualFile> files = new ArrayList<VirtualFile>();

        VirtualFile projectDirectory = project.getBaseDir();
        final VirtualFile webDirectory = VfsUtil.findRelativeFile(projectDirectory, "web");

        if (null == webDirectory) {
            return files;
        }

        ProjectFileIndex fileIndex = ProjectFileIndex.SERVICE.getInstance(project);
        fileIndex.iterateContentUnderDirectory(webDirectory, new ContentIterator() {
            @Override
            public boolean processFile(final VirtualFile virtualFile) {
                if (!virtualFile.isDirectory()) {
                    files.add(virtualFile);
                }

                return true;
            }
        });

        return files;
    }

    private String getPath(Project project, String path) {
        if (!FileUtil.isAbsolute(path)) { // Project relative path
            path = project.getBasePath() + "/" + path;
        }

        return path;
    }

}
