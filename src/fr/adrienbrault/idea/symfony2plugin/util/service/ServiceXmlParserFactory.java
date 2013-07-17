package fr.adrienbrault.idea.symfony2plugin.util.service;

import com.intellij.openapi.project.Project;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ServiceXmlParserFactory {

    protected static Map<Project, Map<Class, ServiceXmlParserFactory>> instance = new HashMap<Project, Map<Class, ServiceXmlParserFactory>>();

    protected Project project;
    protected ServiceParserInterface serviceParserInstance;

    protected HashMap<String, Long> serviceFiles = new HashMap<String, Long>();

    public ServiceXmlParserFactory(Project project) {
        this.project = project;
    }

    protected boolean isModified(ArrayList<File> serviceFiles) {
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
            }
        }

        return false;
    }

    @Nullable
    public <T extends ServiceParserInterface> T parser(Class<T> serviceParser) {

        Symfony2ProjectComponent symfony2ProjectComponent = this.project.getComponent(Symfony2ProjectComponent.class);

        File serviceFile = symfony2ProjectComponent.getPathToProjectContainer();
        ArrayList<File> settingsServiceFiles = new ArrayList<File>();
        settingsServiceFiles.add(serviceFile);

        if (this.serviceParserInstance != null && !this.isModified(settingsServiceFiles)) {
            return (T) this.serviceParserInstance;
        }

        try {
            this.serviceParserInstance = serviceParser.newInstance();
        } catch (InstantiationException ignored) {
        } catch (IllegalAccessException ignored) {
        }

        if (this.serviceParserInstance != null) {
            this.serviceFiles = new HashMap<String, Long>();
            for(File settingsServiceFile: settingsServiceFiles) {
                if(settingsServiceFile.exists()) {
                    this.serviceParserInstance.parser(settingsServiceFile);
                    serviceFiles.put(settingsServiceFile.getAbsolutePath(), settingsServiceFile.lastModified());
                }
            }
        }

        return (T) this.serviceParserInstance;
    }

    public void setCacheInvalid() {
        this.serviceFiles = new HashMap<String, Long>();
    }

    public static <T extends ServiceParserInterface> T getInstance(Project project, Class<T> serviceParser){

        Map<Class, ServiceXmlParserFactory> projectInstance = instance.get(project);

        if(projectInstance == null) {
            projectInstance = new HashMap<Class, ServiceXmlParserFactory>();
            instance.put(project, projectInstance);
        }

        ServiceXmlParserFactory serviceXmlParserFactory = projectInstance.get(serviceParser);
        if(serviceXmlParserFactory == null) {
            serviceXmlParserFactory = new ServiceXmlParserFactory(project);
            projectInstance.put(serviceParser, serviceXmlParserFactory);
        }

        return serviceXmlParserFactory.parser(serviceParser);

    }

}
