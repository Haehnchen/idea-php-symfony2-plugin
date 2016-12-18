package fr.adrienbrault.idea.symfony2plugin.action;

import com.intellij.openapi.actionSystem.AnActionEvent;
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

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
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

        Editor editor = event.getData(PlatformDataKeys.EDITOR);
        if(editor == null) {
            return;
        }

        PsiFile psiFile = event.getData(PlatformDataKeys.PSI_FILE);
        if(!(psiFile instanceof YAMLFile) && !(psiFile instanceof XmlFile) && !(psiFile instanceof PhpFile)) {
            return;
        }

        PhpClass phpClass = null;
        if(psiFile instanceof PhpFile) {

            if("ProjectViewPopup".equals(event.getPlace())) {
                phpClass = PhpElementsUtil.getFirstClassFromFile((PhpFile) psiFile);
            } else {
                PsiElement psiElement = event.getData(PlatformDataKeys.PSI_ELEMENT);
                if(psiElement instanceof PhpClass) {
                    phpClass = (PhpClass) psiElement;
                }
            }

        }

        if(phpClass == null) {
            SymfonyCreateService.create(editor.getComponent(), project, psiFile, editor);
            return;
        }

        SymfonyCreateService.create(editor.getComponent(), project, psiFile, phpClass, editor);
    }
}


