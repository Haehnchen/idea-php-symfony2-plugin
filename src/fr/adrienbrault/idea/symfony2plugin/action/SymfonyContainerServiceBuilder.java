package fr.adrienbrault.idea.symfony2plugin.action;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
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
            event.getPresentation().setVisible(false);
            event.getPresentation().setEnabled(false);
            return;
        }

        //Editor editor = (Editor)event.getData(PlatformDataKeys.EDITOR);
        //VirtualFile file = (VirtualFile)event.getData(PlatformDataKeys.VIRTUAL_FILE);
        PsiFile psiFile = event.getData(PlatformDataKeys.PSI_FILE);
        if(!(psiFile instanceof YAMLFile)) {
            event.getPresentation().setVisible(false);
            event.getPresentation().setEnabled(false);
        }

    }

    public void actionPerformed(AnActionEvent event) {



        SymfonyCreateService symfonyCreateService = new SymfonyCreateService(event.getProject());
        Dimension dim = new Dimension();
        dim.setSize(700, 590);
        symfonyCreateService.setTitle("Create Service");
        symfonyCreateService.setMinimumSize(dim);
        symfonyCreateService.pack();
        symfonyCreateService.setLocationRelativeTo(null);
        symfonyCreateService.setVisible(true);


    }

}
