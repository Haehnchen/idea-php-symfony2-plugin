package fr.adrienbrault.idea.symfony2plugin.extension;

import com.intellij.openapi.project.Project;
import fr.adrienbrault.idea.symfony2plugin.dic.ContainerFile;

import java.util.Collection;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ServiceContainerLoaderParameter {

    private final Collection<ContainerFile> containerFiles;
    private final Project project;

    public ServiceContainerLoaderParameter(Project project, Collection<ContainerFile> containerFiles) {
        this.containerFiles = containerFiles;
        this.project = project;
    }

    public void addContainerFile(ContainerFile containerFile) {
        this.containerFiles.add(containerFile);
    }

    public void addContainerFiles(Collection<ContainerFile> containerFiles) {
        this.containerFiles.addAll(containerFiles);
    }

    public Project getProject() {
        return project;
    }

}
