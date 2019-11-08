package fr.adrienbrault.idea.symfonyplugin.external.locator;

import com.intellij.psi.PsiElement;
import de.espend.idea.php.toolbox.extension.PhpToolboxTargetLocator;
import de.espend.idea.php.toolbox.navigation.locator.TargetLocatorParameter;
import fr.adrienbrault.idea.symfonyplugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfonyplugin.templating.util.TwigUtil;
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

        return new HashSet<>(TwigUtil.getTemplatePsiElements(parameter.getProject(), target));
    }
}
