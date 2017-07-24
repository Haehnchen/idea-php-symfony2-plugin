package fr.adrienbrault.idea.symfony2plugin.action;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.xml.XmlFile;
import com.jetbrains.php.lang.psi.PhpFile;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.action.ui.SymfonyCreateService;
import fr.adrienbrault.idea.symfony2plugin.util.IdeHelper;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.yaml.psi.YAMLFile;

import java.awt.*;

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

        Pair<PsiFile, PhpClass> pair = findPhpClass(event);
        if(pair == null) {
            return;
        }

        PsiFile psiFile = pair.getFirst();
        if(!(psiFile instanceof YAMLFile) && !(psiFile instanceof XmlFile) && !(psiFile instanceof PhpFile)) {
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

        Component applicationWindow = IdeHelper.getWindowComponentFromProject(event.getProject());
        if(applicationWindow == null) {
            return;
        }

        Pair<PsiFile, PhpClass> pair = findPhpClass(event);
        if(pair == null) {
            return;
        }

        if(pair.getSecond() == null) {
            SymfonyCreateService.create(applicationWindow, project, pair.getFirst(), editor);
            return;
        }

        SymfonyCreateService.create(applicationWindow, project, pair.getFirst(), pair.getSecond(), editor);
    }

    @Nullable
    private Pair<PsiFile, PhpClass> findPhpClass(@NotNull AnActionEvent event) {
        PsiFile psiFile = event.getData(PlatformDataKeys.PSI_FILE);
        if(!(psiFile instanceof YAMLFile) && !(psiFile instanceof XmlFile) && !(psiFile instanceof PhpFile)) {
            return null;
        }

        if("ProjectViewPopup".equals(event.getPlace())) {
            // menu item
            return Pair.create(psiFile, PhpElementsUtil.getFirstClassFromFile((PhpFile) psiFile));
        } else {
            PsiElement psiElement = event.getData(PlatformDataKeys.PSI_ELEMENT);
            if(psiElement instanceof PhpClass) {
                // directly got the class
                return Pair.create(psiFile, (PhpClass) psiElement);
            } else {
                // click inside class
                return Pair.create(psiFile, PhpElementsUtil.getFirstClassFromFile((PhpFile) psiFile));
            }
        }
    }
}


