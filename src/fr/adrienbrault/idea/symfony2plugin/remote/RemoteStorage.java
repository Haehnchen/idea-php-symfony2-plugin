package fr.adrienbrault.idea.symfony2plugin.remote;

import com.intellij.openapi.project.Project;
import fr.adrienbrault.idea.symfony2plugin.remote.provider.DoctrineProvider;
import fr.adrienbrault.idea.symfony2plugin.remote.provider.ProviderInterface;
import fr.adrienbrault.idea.symfony2plugin.remote.provider.RouterProvider;
import fr.adrienbrault.idea.symfony2plugin.remote.provider.TwigProvider;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class RemoteStorage {

    protected static Map<Project, RemoteStorage> instance = new HashMap<Project, RemoteStorage>();

    protected Project project;
    protected Map<Class, ProviderInterface> instances = new HashMap<Class, ProviderInterface>();

    public boolean has(Class clazz) {
        return instances.containsKey(clazz);
    }

    @Nullable
    public <T extends ProviderInterface> T get(Class<T> serviceParser) {

        if(instances.containsKey(serviceParser)) {
            return (T)instances.get(serviceParser);
        }

        try {
            this.instances.put(serviceParser, serviceParser.newInstance());
            return (T) instances.get(serviceParser);
        } catch (InstantiationException ignored) {
        } catch (IllegalAccessException ignored) {
        }

        return null;
    }

    synchronized public static RemoteStorage getInstance(Project project){

        if(instance.containsKey(project)) {
            return instance.get(project);
        }

        instance.put(project, new RemoteStorage());

        return instance.get(project);

    }

    synchronized public static void removeInstance(Project project){
        if(instance.containsKey(project)) {
            instance.remove(project);
        }
    }

}