package fr.adrienbrault.idea.symfony2plugin.routing.inspection;

import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElement;
import fr.adrienbrault.idea.symfony2plugin.routing.PhpRouteInspection;
import org.jetbrains.annotations.NotNull;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class PhpRouteMissingInspection extends AbstractPhpRouteInspection {
    protected void annotateRouteName(PsiElement target, @NotNull ProblemsHolder holder, final String routeName) {
        PhpRouteInspection.annotateRouteName(target, holder, routeName);
    }
}
