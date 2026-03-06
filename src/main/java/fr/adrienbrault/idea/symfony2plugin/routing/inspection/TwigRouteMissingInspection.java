package fr.adrienbrault.idea.symfony2plugin.routing.inspection;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.patterns.ElementPattern;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.templating.TwigPattern;
import fr.adrienbrault.idea.symfony2plugin.routing.RouteHelper;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TwigRouteMissingInspection extends LocalInspectionTool {
    @NotNull
    public PsiElementVisitor buildVisitor(final @NotNull ProblemsHolder holder, boolean isOnTheFly) {
        if(!Symfony2ProjectComponent.isEnabled(holder.getProject())) {
            return super.buildVisitor(holder, isOnTheFly);
        }

        return new MyPsiElementVisitor(holder);
    }

    private static class MyPsiElementVisitor extends PsiElementVisitor {
        @NotNull private final ProblemsHolder holder;
        private ElementPattern<?> autocompletableRoutePattern;

        MyPsiElementVisitor(@NotNull ProblemsHolder holder) {
            this.holder = holder;
        }

        @Override
        public void visitElement(@NotNull PsiElement element) {
            if(getAutocompletableRoutePattern().accepts(element) && TwigUtil.isValidStringWithoutInterpolatedOrConcat(element)) {
                invoke(element, holder);
            }

            super.visitElement(element);
        }

        private ElementPattern<?> getAutocompletableRoutePattern() {
            return autocompletableRoutePattern != null ? autocompletableRoutePattern : (autocompletableRoutePattern = TwigPattern.getAutocompletableRoutePattern());
        }
    }

    private static void invoke(@NotNull final PsiElement element, @NotNull ProblemsHolder holder) {
        String text = element.getText();
        if (StringUtils.isBlank(text)) {
            return;
        }

        if (!RouteHelper.isExistingRouteName(element.getProject(), RouteHelper.unescapeRouteName(text))) {
            holder.registerProblem(element, "Symfony: Missing Route", new RouteGuessTypoQuickFix(text));
        }
    }
}
