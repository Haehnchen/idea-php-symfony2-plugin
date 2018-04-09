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

        @NotNull
        private final Collection<ServiceInterface> services;

        public Service(@NotNull Project project, @NotNull Collection<ServiceInterface> services) {
            this.project = project;
            this.services = services;
        }

        @NotNull
        public Project getProject() {
            return project;
        }

        public void add(@NotNull ServiceInterface service) {
            this.services.add(service);
        }

        public void addAll(@NotNull Collection<ServiceInterface> services) {
            this.services.addAll(services);
        }

        public void add(@NotNull String id) {
            this.services.add(new SerializableService(id));
        }

        public void add(@NotNull String id, @NotNull String clazz) {
            this.services.add(new SerializableService(id).setClassName(clazz));
        }
    }

    public static class Id {

        @NotNull
        private final Project project;

        @NotNull
        private final Collection<String> ids;

        public Id(@NotNull Project project, @NotNull Collection<String> ids) {
            this.project = project;
            this.ids = ids;
        }

        @NotNull
        public Project getProject() {
            return project;
        }

        public void add(@NotNull String id) {
            this.ids.add(id);
        }

        public void addAll(@NotNull Collection<String> names) {
            this.ids.addAll(names);
        }
    }
}
