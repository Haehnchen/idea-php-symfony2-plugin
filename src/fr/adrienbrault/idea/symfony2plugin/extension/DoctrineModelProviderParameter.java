package fr.adrienbrault.idea.symfony2plugin.extension;

import com.intellij.openapi.project.Project;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.dic.ContainerFile;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;

public class DoctrineModelProviderParameter {

    final private Collection<DoctrineModel> doctrineModels;
    final private Project project;

    public DoctrineModelProviderParameter(Project project, Collection<DoctrineModel> doctrineModels) {
        this.doctrineModels = doctrineModels;
        this.project = project;
    }

    public void addModel(DoctrineModel doctrineModel) {
        this.doctrineModels.add(doctrineModel);
    }

    public void addModels(Collection<DoctrineModel> doctrineModels) {
        this.doctrineModels.addAll(doctrineModels);
    }

    public Project getProject() {
        return project;
    }

    public static class DoctrineModel {

        final private PhpClass phpClass;
        private String name;

        public DoctrineModel(PhpClass phpClass) {
            this.name = phpClass.getPresentableFQN();
            this.phpClass = phpClass;
        }

        public DoctrineModel(String name, PhpClass phpClass) {
            this.phpClass = phpClass;
            this.name = name;
        }

        public PhpClass getPhpClass() {
            return phpClass;
        }

        @Nullable
        public String getName() {
            return name;
        }

    }


}
