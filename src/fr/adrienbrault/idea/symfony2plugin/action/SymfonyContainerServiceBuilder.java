package fr.adrienbrault.idea.symfony2plugin.action;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataKey;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.xml.XmlFile;
import com.jetbrains.php.lang.psi.PhpFile;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.action.ui.SymfonyCreateService;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import org.jetbrains.yaml.psi.YAMLFile;

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

        PsiFile psiFile = event.getData(PlatformDataKeys.PSI_FILE);
        if(psiFile instanceof PhpFile) {

            if("ProjectViewPopup".equals(event.getPlace())) {

                if(PhpElementsUtil.getFirstClassFromFile((PhpFile) psiFile) == null) {
                    this.setStatus(event, false);
                }

            } else {
                PsiElement psiElement = event.getData(PlatformDataKeys.PSI_ELEMENT);
                if(!(psiElement instanceof PhpClass)) {
                    this.setStatus(event, false);
                }
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

        Project project = event.getProject();
        if(project == null) {
            return;
        }

        // only since phpstorm 7.1; PlatformDataKeys.PSI_FILE
        Object psiFile = event.getData(DataKey.create("psi.File"));

        if(!(psiFile instanceof YAMLFile) && !(psiFile instanceof XmlFile) && !(psiFile instanceof PhpFile)) {
            return;
        }

        PhpClass phpClass = null;
        if(psiFile instanceof PhpFile) {

            if("ProjectViewPopup".equals(event.getPlace())) {
                phpClass = PhpElementsUtil.getFirstClassFromFile((PhpFile) psiFile);
            } else {
                Object psiElement = event.getData(DataKey.create("psi.Element"));
                if(psiElement instanceof PhpClass) {
                    phpClass = (PhpClass) psiElement;
                }
            }

        }

        Editor editor = event.getData(CommonDataKeys.EDITOR);


        if(phpClass == null) {
            SymfonyCreateService.create(project, (PsiFile) psiFile, editor);
            return;
        }

        SymfonyCreateService.create(project, (PsiFile) psiFile, phpClass, editor);
    }

}


