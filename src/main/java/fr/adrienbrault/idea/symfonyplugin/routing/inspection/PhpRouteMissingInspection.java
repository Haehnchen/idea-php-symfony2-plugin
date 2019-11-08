package fr.adrienbrault.idea.symfonyplugin.routing.inspection;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import fr.adrienbrault.idea.symfonyplugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfonyplugin.routing.PhpRouteReferenceContributor;
import fr.adrienbrault.idea.symfonyplugin.routing.Route;
import fr.adrienbrault.idea.symfonyplugin.routing.RouteHelper;
import fr.adrienbrault.idea.symfonyplugin.util.MethodMatcher;
import fr.adrienbrault.idea.symfonyplugin.util.PhpElementsUtil;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class PhpRouteMissingInspection extends LocalInspectionTool {
    @NotNull
    @Override
    public PsiElementVisitor buildVisitor(final @NotNull ProblemsHolder holder, boolean isOnTheFly) {
        if(!Symfony2ProjectComponent.isEnabled(holder.getProject())) {
            return super.buildVisitor(holder, isOnTheFly);
        }

        return new PsiElementVisitor() {
            @Override
            public void visitElement(PsiElement element) {
                if(PhpElementsUtil.getMethodWithFirstStringPattern().accepts(element)) {
                    String contents = PhpElementsUtil.getStringValue(element);
                    if(contents != null && StringUtils.isNotBlank(contents)) {
                        invoke(contents, element, holder);
                    }
                }

                super.visitElement(element);
            }
        };
    }

    private void invoke(@NotNull String routeName, @NotNull final PsiElement element, @NotNull ProblemsHolder holder) {
        MethodMatcher.MethodMatchParameter methodMatchParameter = new MethodMatcher.StringParameterMatcher(element, 0)
            .withSignature(PhpRouteReferenceContributor.GENERATOR_SIGNATURES)
            .match();

        if(methodMatchParameter == null) {
            return;
        }

        Collection<Route> route = RouteHelper.getRoute(element.getProject(), routeName);
        if(route.size() == 0) {
            holder.registerProblem(element, "Missing Route", ProblemHighlightType.GENERIC_ERROR_OR_WARNING);
        }
    }
}
