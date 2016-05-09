package fr.adrienbrault.idea.symfony2plugin.extension;

import com.intellij.openapi.project.Project;
import fr.adrienbrault.idea.symfony2plugin.dic.container.SerializableService;
import fr.adrienbrault.idea.symfony2plugin.dic.container.ServiceInterface;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ServiceCollectorParameter {

    public static class Service {

        @NotNull
        private final Project project;
        private final Collection<ServiceInterface> services;

        public Service(@NotNull Project project, @NotNull Collection<ServiceInterface> services) {
            this.project = project;
            this.services = services;
        }

        @NotNull
        public Project getProject() {
            return project;
        }

        public void addService(@NotNull ServiceInterface service) {
            this.services.add(service);
        }

        public void addServices(@NotNull Collection<ServiceInterface> services) {
            this.services.addAll(services);
        }

        public void addService(@NotNull String id) {
            this.services.add(new SerializableService(id));
        }

        public void addService(@NotNull String id, @NotNull String clazz) {
            this.services.add(new SerializableService(id).setClassName(clazz));
        }

        public void addServices(@NotNull ServiceInterface service) {
            this.services.add(service);
        }

    }

    public static class Id {

        @NotNull
        private final Project project;
        @NotNull
        private final Collection<String> names;

        public Id(@NotNull Project project, @NotNull Collection<String> names) {
            this.project = project;
            this.names = names;
        }

        @NotNull
        public Project getProject() {
            return project;
        }

        public void addName(@NotNull String name) {
            this.names.add(name);
        }

        public void addNames(@NotNull Collection<String> names) {
            this.names.addAll(names);
        }
    }
}
