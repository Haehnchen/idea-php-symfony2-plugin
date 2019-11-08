package fr.adrienbrault.idea.symfonyplugin.extension;

import com.intellij.openapi.project.Project;
import fr.adrienbrault.idea.symfonyplugin.dic.ContainerParameter;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ServiceParameterCollectorParameter {
    public static class Id {

        @NotNull
        private final Project project;

        @NotNull
        private final Collection<ContainerParameter> ids;

        public Id(@NotNull Project project, @NotNull Collection<ContainerParameter> ids) {
            this.project = project;
            this.ids = ids;
        }

        @NotNull
        public Project getProject() {
            return project;
        }

        public void add(@NotNull ContainerParameter id) {
            this.ids.add(id);
        }

        public void addAll(@NotNull Collection<ContainerParameter> names) {
            this.ids.addAll(names);
        }
    }
}
