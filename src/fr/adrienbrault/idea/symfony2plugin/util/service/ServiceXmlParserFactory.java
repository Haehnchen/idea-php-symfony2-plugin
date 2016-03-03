package fr.adrienbrault.idea.symfony2plugin.util.service;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.StreamUtil;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.dic.webDeployment.ServiceContainerRemoteFileStorage;
import fr.adrienbrault.idea.symfony2plugin.webDeployment.utils.RemoteWebServerUtil;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ServiceXmlParserFactory {

    protected static Map<Project, Map<Class, ServiceXmlParserFactory>> instance = new HashMap<Project, Map<Class, ServiceXmlParserFactory>>();

    protected Project project;
    protected ServiceParserInterface serviceParserInstance;

    protected HashMap<String, Long> serviceFiles = new HashMap<String, Long>();

    private long lastWebRemoteBuild = -1;

    public ServiceXmlParserFactory(Project project) {
        this.project = project;
    }

    protected boolean isModified(List<File> serviceFiles) {
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

        // remote files
        long remoteBuildTime = -1;
        ServiceContainerRemoteFileStorage extension = RemoteWebServerUtil.getExtensionInstance(project, ServiceContainerRemoteFileStorage.class);
        if(extension != null) {
            remoteBuildTime = extension.getState().getBuildTime();
        }

        if(remoteBuildTime != this.lastWebRemoteBuild) {
            this.lastWebRemoteBuild = remoteBuildTime;
            return true;
        }

        return false;
    }

    @Nullable
    synchronized public <T extends ServiceParserInterface> T parser(Class<T> serviceParser) {

        Symfony2ProjectComponent symfony2ProjectComponent = this.project.getComponent(Symfony2ProjectComponent.class);

        List<File> settingsServiceFiles = symfony2ProjectComponent.getContainerFiles();

        if (this.serviceParserInstance != null && !this.isModified(settingsServiceFiles)) {
            return (T) this.serviceParserInstance;
        }

        try {
            this.serviceParserInstance = serviceParser.newInstance();
            Symfony2ProjectComponent.getLogger().info("new instance: " + serviceParser.getName());
        } catch (InstantiationException ignored) {
        } catch (IllegalAccessException ignored) {
        }

        if (this.serviceParserInstance != null) {

            // collect remote files
            ServiceContainerRemoteFileStorage extension = RemoteWebServerUtil.getExtensionInstance(project, ServiceContainerRemoteFileStorage.class);
            if(extension != null) {
                for (InputStream inputStream : extension.getState().getInputStreams()) {
                    this.serviceParserInstance.parser(inputStream);
                }
            }

            this.serviceFiles = new HashMap<String, Long>();
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
        this.serviceFiles = new HashMap<String, Long>();
    }

    synchronized public static <T extends ServiceParserInterface> T getInstance(Project project, Class<T> serviceParser){

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

    synchronized public static void cleanInstance(Project project){
        if(instance.containsKey(project)) {
            Symfony2ProjectComponent.getLogger().info("clean ServiceXmlParserFactory for " + project.getName());
            instance.remove(project);
        }
    }

}
