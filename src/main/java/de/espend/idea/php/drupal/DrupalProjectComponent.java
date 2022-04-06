package de.espend.idea.php.drupal;


import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import org.jetbrains.annotations.Nullable;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class DrupalProjectComponent {

    public static boolean isEnabled(Project project) {
        return Symfony2ProjectComponent.isEnabled(project);
    }

    public static boolean isEnabled(@Nullable PsiElement psiElement) {
        return psiElement != null && isEnabled(psiElement.getProject());
    }

}
