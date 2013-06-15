package fr.adrienbrault.idea.symfony2plugin.util.service;

import com.intellij.openapi.project.Project;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class ServiceXmlParserFactory {

    protected static Map<Project, Map<Class, ServiceXmlParserFactory>> instance = new HashMap<Project, Map<Class, ServiceXmlParserFactory>>();

    protected Project project;
    protected ServiceParserInterface serviceParser;

    protected Long lastUpdate;
    protected ServiceParserInterface serviceParserInstance;

    public ServiceXmlParserFactory(Project project) {
        this.project = project;
    }

    @Nullable
    public <T extends ServiceParserInterface> T parser(Class<T> serviceParser) {

        Symfony2ProjectComponent symfony2ProjectComponent = this.project.getComponent(Symfony2ProjectComponent.class);

        File serviceFile = symfony2ProjectComponent.getPathToProjectContainer();

        Long serviceModTime = 0L;
        if (serviceFile != null) {
            serviceModTime = serviceFile.lastModified();
        }

        if (this.serviceParserInstance != null && serviceModTime.equals(this.lastUpdate)) {
            return (T) this.serviceParserInstance;
        }

        try {
            this.serviceParserInstance = serviceParser.newInstance();
        } catch (InstantiationException ignored) {
        } catch (IllegalAccessException ignored) {
        }

        if (this.serviceParserInstance != null && serviceFile != null) {
            this.serviceParserInstance.parser(serviceFile);
        }

        this.lastUpdate = serviceModTime;
        return (T) this.serviceParserInstance;
    }

    public void setCacheInvalid() {
       this.lastUpdate = null;
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
