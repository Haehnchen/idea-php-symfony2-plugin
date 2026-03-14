package fr.adrienbrault.idea.symfony2plugin.util.service;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.project.Project;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.extension.CompiledServiceBuilderArguments;
import fr.adrienbrault.idea.symfony2plugin.extension.CompiledServiceBuilderFactory;
import org.jetbrains.annotations.Nullable;

import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.*;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ServiceXmlParserFactory {

    protected static final Map<Project, Map<Class, ServiceXmlParserFactory>> instance = new HashMap<>();

    private final Project project;
    private ServiceParserInterface serviceParserInstance;

    private HashMap<String, Long> serviceFiles = new HashMap<>();

    private final Collection<CompiledServiceBuilderFactory.Builder> extensions = new ArrayList<>();
    private static final ExtensionPointName<CompiledServiceBuilderFactory> EXTENSIONS = new ExtensionPointName<>(
        "fr.adrienbrault.idea.symfony2plugin.extension.CompiledServiceBuilderFactory"
    );

    public ServiceXmlParserFactory(Project project) {
        this.project = project;
    }

    private boolean isModified(Collection<File> serviceFiles) {
        if(this.serviceFiles.size() != serviceFiles.size()) {
            return true;
        }

        for(File serviceFile: serviceFiles) {
            // Use VFS instead of File I/O - VirtualFile.getTimeStamp() is cached
            VirtualFile vf = VfsUtil.findFileByIoFile(serviceFile, false);
            if(vf != null && vf.exists()) {
                if(!this.serviceFiles.containsKey(serviceFile.getAbsolutePath())) {
                    return true;
                }
                if(!this.serviceFiles.get(serviceFile.getAbsolutePath()).equals(vf.getTimeStamp())) {
                    return true;
                }
            } else {
                Symfony2ProjectComponent.getLogger().warn("file not found: " + serviceFiles.toString());
            }
        }

        if(!this.extensions.isEmpty()) {
            for (CompiledServiceBuilderFactory.Builder builder : this.extensions) {
                if(builder.isModified(project)) {
                    return true;
                }
            }
        }

        return false;
    }

    @Nullable
    @SuppressWarnings("unchecked")
    synchronized public <T extends ServiceParserInterface> T parser(Class<T> serviceParser) {
        Collection<File> settingsServiceFiles = Symfony2ProjectComponent.getContainerFiles(this.project);

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
            if(!this.extensions.isEmpty()) {
                CompiledServiceBuilderArguments args = new CompiledServiceBuilderArguments(project);
                for (CompiledServiceBuilderFactory.Builder builder : this.extensions) {
                    builder.build(args);
                }

                for (InputStream inputStream : args.getStreams()) {
                    this.serviceParserInstance.parser(inputStream, null, project);
                }
            }

            this.serviceFiles = new HashMap<>();
            for(File settingsServiceFile: settingsServiceFiles) {
                // Use VFS instead of File I/O - VirtualFile.getTimeStamp() is cached
                VirtualFile vf = VfsUtil.findFileByIoFile(settingsServiceFile, false);
                if(vf == null || !vf.exists()) {
                    continue;
                }

                try {
                    this.serviceParserInstance.parser(new FileInputStream(settingsServiceFile), vf, project);
                } catch (FileNotFoundException e) {
                    continue;
                }

                serviceFiles.put(settingsServiceFile.getAbsolutePath(), vf.getTimeStamp());
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
