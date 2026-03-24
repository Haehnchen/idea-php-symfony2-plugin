package fr.adrienbrault.idea.symfony2plugin.routing.inspection;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.routing.RouteHelper;
import fr.adrienbrault.idea.symfony2plugin.templating.TwigPattern;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

/**
 * Inspects route names used in Twig comparison expressions against app.request.attributes.get('_route'):
 *
 *   app.request.attributes.get('_route') == 'my_route'
 *   app.request.attributes.get('_route') != 'my_route'
 *   app.request.attributes.get('_route') is same as('my_route')
 *   app.request.attributes.get('_route') in ['route_a', 'route_b']
 *   app.request.attributes.get('_route') not in ['route_a', 'route_b']
 *
 * Note: 'starts with' is intentionally excluded because the string is a prefix, not a full route name.
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TwigRouteCompareInspection extends LocalInspectionTool {
    @NotNull
    @Override
    public PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        if (!Symfony2ProjectComponent.isEnabled(holder.getProject())) {
            return super.buildVisitor(holder, isOnTheFly);
        }

        return new PsiElementVisitor() {
            @Override
            public void visitElement(@NotNull PsiElement element) {
                if ((TwigPattern.getTwigRouteComparePattern().accepts(element)
                    || TwigPattern.getTwigRouteSameAsPattern().accepts(element)
                    || TwigPattern.getTwigRouteInArrayPattern().accepts(element))
                    && TwigPattern.isRouteCompareContext(element)
                    && TwigUtil.isValidStringWithoutInterpolatedOrConcat(element))
                {
                    invoke(element, holder);
                }

                super.visitElement(element);
            }
        };
    }

    private static void invoke(@NotNull PsiElement element, @NotNull ProblemsHolder holder) {
        String routeName = element.getText();
        if (StringUtils.isBlank(routeName)) {
            return;
        }

        if (!RouteHelper.isExistingRouteName(element.getProject(), routeName)) {
            holder.registerProblem(element, "Symfony: Missing Route", new RouteGuessTypoQuickFix(routeName));
        }
    }
}
