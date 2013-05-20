package fr.adrienbrault.idea.symfony2plugin.util.service;

import com.intellij.openapi.project.Project;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.*;

public class ServiceXmlParserFactory implements ServiceFileInterface {

    protected static Map<Project, Map<Class, ServiceXmlParserFactory>> instance = new HashMap<Project, Map<Class, ServiceXmlParserFactory>>();

    protected Project project;
    protected ServiceParserInterface serviceParser;

    protected Long lastUpdate;
    protected Collection items;

    public ServiceXmlParserFactory(Project project, Class serviceParser) {
        this.project = project;

        try {
            this.serviceParser = (ServiceParserInterface) serviceParser.newInstance();
        } catch (InstantiationException ignored) {
        } catch (IllegalAccessException ignored) {
        }

    }

    @Nullable
    public Collection parser() {

        Symfony2ProjectComponent symfony2ProjectComponent = this.project.getComponent(Symfony2ProjectComponent.class);

        File serviceFile = symfony2ProjectComponent.getPathToProjectContainer();
        if (serviceFile == null) {
            return null;
        }

        Long serviceModTime = serviceFile.lastModified();
        if (this.items != null && serviceModTime.equals(this.lastUpdate)) {
            return this.items;
        }

        // @TODO: check null
        return this.items = this.serviceParser.parser(serviceFile);
    }

    public void setCacheInvalid() {
       this.lastUpdate = null;
    }

    public static ServiceXmlParserFactory getInstance(Project project, Class object){

        Map<Class, ServiceXmlParserFactory> projectInstance = instance.get(project);

        if(projectInstance == null) {
            projectInstance = new HashMap<Class, ServiceXmlParserFactory>();
            instance.put(project, projectInstance);
        }

        ServiceXmlParserFactory projectPhpTypeInstance = projectInstance.get(object);
        if(projectPhpTypeInstance == null) {
            projectPhpTypeInstance = new ServiceXmlParserFactory(project, object);
            projectInstance.put(object, projectPhpTypeInstance);
        }

        return projectPhpTypeInstance;

    }

}
