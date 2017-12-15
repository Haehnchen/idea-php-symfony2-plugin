package fr.adrienbrault.idea.symfony2plugin.external.locator;

import com.intellij.psi.PsiElement;
import de.espend.idea.php.toolbox.extension.PhpToolboxTargetLocator;
import de.espend.idea.php.toolbox.navigation.locator.TargetLocatorParameter;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigHelper;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TwigFileToolboxTargetLocator implements PhpToolboxTargetLocator {
    @NotNull
    @Override
    public Collection<PsiElement> getTargets(@NotNull TargetLocatorParameter parameter) {
        if(!Symfony2ProjectComponent.isEnabled(parameter.getProject())) {
            return Collections.emptyList();
        }

        String target = parameter.getTarget();
        if(!target.toLowerCase().endsWith(".twig")) {
            return Collections.emptyList();
        }

        Collection<PsiElement> psiElements = new HashSet<>();
        Collections.addAll(psiElements, TwigHelper.getTemplatePsiElements(parameter.getProject(), target));
        return psiElements;
    }
}
