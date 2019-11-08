package fr.adrienbrault.idea.symfonyplugin.routing.inspection;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import fr.adrienbrault.idea.symfonyplugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfonyplugin.templating.TwigPattern;
import fr.adrienbrault.idea.symfonyplugin.routing.RouteHelper;
import fr.adrienbrault.idea.symfonyplugin.templating.util.TwigUtil;
import org.apache.commons.lang.StringUtils;
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

        return new PsiElementVisitor() {
            @Override
            public void visitElement(PsiElement element) {
                if(TwigPattern.getAutocompletableRoutePattern().accepts(element) && TwigUtil.isValidStringWithoutInterpolatedOrConcat(element)) {
                    invoke(element, holder);
                }

                super.visitElement(element);
            }
        };
    }

    private void invoke(@NotNull final PsiElement element, @NotNull ProblemsHolder holder) {
        String text = element.getText();
        if(StringUtils.isBlank(text)) {
            return;
        }

        if(RouteHelper.getRoute(element.getProject(), text).size() == 0) {
            holder.registerProblem(element, "Missing Route");
        }
    }
}
