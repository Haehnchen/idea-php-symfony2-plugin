package fr.adrienbrault.idea.symfony2plugin.routing.inspection;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.patterns.ElementPattern;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.routing.PhpRouteReferenceContributor;
import fr.adrienbrault.idea.symfony2plugin.routing.RouteHelper;
import fr.adrienbrault.idea.symfony2plugin.util.MethodMatcher;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

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

        return new MyPsiElementVisitor(holder);
    }

    private static class MyPsiElementVisitor extends PsiElementVisitor {
        @NotNull private final ProblemsHolder holder;
        private ElementPattern<?> methodWithFirstStringPattern;

        MyPsiElementVisitor(@NotNull ProblemsHolder holder) {
            this.holder = holder;
        }

        @Override
        public void visitElement(@NotNull PsiElement element) {
            if(getMethodWithFirstStringPattern().accepts(element)) {
                String contents = PhpElementsUtil.getStringValue(element);
                if(StringUtils.isNotBlank(contents)) {
                    invoke(contents, element, holder);
                }
            }

            super.visitElement(element);
        }

        private ElementPattern<?> getMethodWithFirstStringPattern() {
            return methodWithFirstStringPattern != null ? methodWithFirstStringPattern : (methodWithFirstStringPattern = PhpElementsUtil.getMethodWithFirstStringOrNamedArgumentPattern());
        }
    }

    private static void invoke(@NotNull String routeName, @NotNull final PsiElement element, @NotNull ProblemsHolder holder) {
        MethodMatcher.MethodMatchParameter methodMatchParameter = new MethodMatcher.StringParameterMatcher(element, 0)
            .withSignature(PhpRouteReferenceContributor.GENERATOR_SIGNATURES)
            .match();

        if(methodMatchParameter == null) {
            return;
        }

        if (!RouteHelper.isExistingRouteName(element.getProject(), routeName)) {
            holder.registerProblem(element, "Symfony: Missing Route", ProblemHighlightType.GENERIC_ERROR_OR_WARNING, new RouteGuessTypoQuickFix(routeName));
        }
    }
}
