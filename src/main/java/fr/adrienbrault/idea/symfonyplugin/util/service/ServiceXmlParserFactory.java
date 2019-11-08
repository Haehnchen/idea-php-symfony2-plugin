package fr.adrienbrault.idea.symfonyplugin.util.service;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.project.Project;
import fr.adrienbrault.idea.symfonyplugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfonyplugin.extension.CompiledServiceBuilderArguments;
import fr.adrienbrault.idea.symfonyplugin.extension.CompiledServiceBuilderFactory;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.*;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ServiceXmlParserFactory {

    protected static Map<Project, Map<Class, ServiceXmlParserFactory>> instance = new HashMap<>();

    private Project project;
    private ServiceParserInterface serviceParserInstance;

    private HashMap<String, Long> serviceFiles = new HashMap<>();

    private Collection<CompiledServiceBuilderFactory.Builder> extensions = new ArrayList<>();
    private static final ExtensionPointName<CompiledServiceBuilderFactory> EXTENSIONS = new ExtensionPointName<>(
        "fr.adrienbrault.idea.symfonyplugin.extension.CompiledServiceBuilderFactory"
    );

    public ServiceXmlParserFactory(Project project) {
        this.project = project;
    }

    private boolean isModified(Collection<File> serviceFiles) {
        if(this.serviceFiles.size() != serviceFiles.size()) {
            return true;
        }

        for(File serviceFile: serviceFiles) {
            if(serviceFile.exists()) {
                if(!this.serviceFiles.containsKey(serviceFile.getAbsolutePath())) {
                    return true;
                }
                if(!this.serviceFiles.get(serviceFile.getAbsolutePath()).equals(serviceFile.lastModified())) {
                    return true;
                }
            } else {
                Symfony2ProjectComponent.getLogger().warn("file not found: " + serviceFiles.toString());
            }
        }

        if(this.extensions.size() > 0) {
            for (CompiledServiceBuilderFactory.Builder builder : this.extensions) {
                if(builder.isModified(project)) {
                    return true;
                }
            }
        }

        return false;
    }

    @Nullable
    synchronized public <T extends ServiceParserInterface> T parser(Class<T> serviceParser) {

        Symfony2ProjectComponent symfony2ProjectComponent = this.project.getComponent(Symfony2ProjectComponent.class);

        Collection<File> settingsServiceFiles = symfony2ProjectComponent.getContainerFiles();

        if (this.serviceParserInstance != null && !this.isModified(settingsServiceFiles)) {
            return (T) this.serviceParserInstance;
        }

        try {
            this.serviceParserInstance = serviceParser.newInstance();
            Symfony2ProjectComponent.getLogger().info("new instance: " + serviceParser.getName());
        } catch (InstantiationException | IllegalAccessException ignored) {
        }

        if (this.serviceParserInstance != null) {

            // extensions
            if(this.extensions.size() > 0) {
                CompiledServiceBuilderArguments args = new CompiledServiceBuilderArguments(project);
                for (CompiledServiceBuilderFactory.Builder builder : this.extensions) {
                    builder.build(args);
                }

                for (InputStream inputStream : args.getStreams()) {
                    this.serviceParserInstance.parser(inputStream);
                }
            }

            this.serviceFiles = new HashMap<>();
            for(File settingsServiceFile: settingsServiceFiles) {
                if(!settingsServiceFile.exists()) {
                    continue;
                }

                try {
                    this.serviceParserInstance.parser(new FileInputStream(settingsServiceFile));
                } catch (FileNotFoundException e) {
                    continue;
                }

                serviceFiles.put(settingsServiceFile.getAbsolutePath(), settingsServiceFile.lastModified());
            }
        }

        Symfony2ProjectComponent.getLogger().info("update: " + serviceParser.getName());

        return (T) this.serviceParserInstance;
    }

    public void setCacheInvalid() {
        this.serviceFiles = new HashMap<>();
    }

    synchronized public static <T extends ServiceParserInterface> T getInstance(Project project, Class<T> serviceParser){

        Map<Class, ServiceXmlParserFactory> projectInstance = instance.computeIfAbsent(project, k -> new HashMap<>());

        ServiceXmlParserFactory serviceXmlParserFactory = projectInstance.get(serviceParser);
        if(serviceXmlParserFactory == null) {
            serviceXmlParserFactory = new ServiceXmlParserFactory(project);

            // add extension for new instance
            for (CompiledServiceBuilderFactory ext : EXTENSIONS.getExtensions()) {
                serviceXmlParserFactory.extensions.add(ext.create());
            }

            projectInstance.put(serviceParser, serviceXmlParserFactory);
        }

        return serviceXmlParserFactory.parser(serviceParser);

    }

    synchronized public static void cleanInstance(Project project){
        if(instance.containsKey(project)) {
            Symfony2ProjectComponent.getLogger().info("clean ServiceXmlParserFactory for " + project.getName());
            instance.remove(project);
        }
    }

}
