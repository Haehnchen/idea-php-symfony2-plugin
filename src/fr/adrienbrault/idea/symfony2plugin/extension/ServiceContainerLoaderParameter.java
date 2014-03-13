package fr.adrienbrault.idea.symfony2plugin.extension;

import com.intellij.openapi.project.Project;
import fr.adrienbrault.idea.symfony2plugin.dic.ContainerFile;

import java.util.Collection;

public class ServiceContainerLoaderParameter {

    private Collection<ContainerFile> containerFiles;
    private Project project;

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
