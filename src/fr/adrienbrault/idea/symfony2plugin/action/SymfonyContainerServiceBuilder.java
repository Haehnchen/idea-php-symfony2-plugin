package fr.adrienbrault.idea.symfony2plugin.action;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataKey;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.psi.xml.XmlFile;
import com.jetbrains.php.lang.psi.PhpFile;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.action.ui.SymfonyCreateService;
import org.jetbrains.yaml.psi.YAMLFile;

import java.awt.*;

public class SymfonyContainerServiceBuilder extends DumbAwareAction {

    public SymfonyContainerServiceBuilder() {
        super("Create Service", "Generate a new Service definition from class name", Symfony2Icons.SYMFONY);
    }

    public void update(AnActionEvent event) {
        Project project = event.getData(PlatformDataKeys.PROJECT);

        if (project == null || !Symfony2ProjectComponent.isEnabled(project)) {
            this.setStatus(event, false);
            return;
        }

        // only since phpstorm 7.1; PlatformDataKeys.PSI_FILE
        Object psiFile = event.getData(DataKey.create("psi.File"));

        if(psiFile instanceof PhpFile) {
            Object psiElement = event.getData(DataKey.create("psi.Element"));
            if(!(psiElement instanceof PhpClass)) {
                this.setStatus(event, false);
            }
            return;
        }

        if(!(psiFile instanceof YAMLFile) && !(psiFile instanceof XmlFile)) {
            this.setStatus(event, false);
        }

    }

    private void setStatus(AnActionEvent event, boolean status) {
        event.getPresentation().setVisible(status);
        event.getPresentation().setEnabled(status);
    }

    public void actionPerformed(AnActionEvent event) {

        // only since phpstorm 7.1; PlatformDataKeys.PSI_FILE
        Object psiFile = event.getData(DataKey.create("psi.File"));

        if(!(psiFile instanceof YAMLFile) && !(psiFile instanceof XmlFile) && !(psiFile instanceof PhpFile)) {
            return;
        }

        SymfonyCreateService symfonyCreateService = new SymfonyCreateService(event.getProject(), (PsiFile) psiFile);

        if(psiFile instanceof PhpFile) {
            Object psiElement = event.getData(DataKey.create("psi.Element"));
            if(psiElement instanceof PhpClass) {
                symfonyCreateService.setClassName(((PhpClass) psiElement).getPresentableFQN());
            }
        }

        symfonyCreateService.init();

        Dimension dim = new Dimension();
        symfonyCreateService.setTitle("Create Service");
        symfonyCreateService.pack();
        symfonyCreateService.setLocationRelativeTo(null);
        symfonyCreateService.setVisible(true);


    }

}


