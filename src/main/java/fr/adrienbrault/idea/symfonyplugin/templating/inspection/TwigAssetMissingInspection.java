package fr.adrienbrault.idea.symfonyplugin.templating.inspection;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import fr.adrienbrault.idea.symfonyplugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfonyplugin.templating.TwigPattern;
import fr.adrienbrault.idea.symfonyplugin.templating.util.TwigUtil;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

/**
 * asset('<caret>')
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TwigAssetMissingInspection extends LocalInspectionTool {
    @NotNull
    public PsiElementVisitor buildVisitor(final @NotNull ProblemsHolder holder, boolean isOnTheFly) {
        if(!Symfony2ProjectComponent.isEnabled(holder.getProject())) {
            return super.buildVisitor(holder, isOnTheFly);
        }

        return new MyPsiElementVisitor(holder);
    }

    private class MyPsiElementVisitor extends PsiElementVisitor {

        private final ProblemsHolder holder;

        MyPsiElementVisitor(ProblemsHolder holder) {
            this.holder = holder;
        }

        @Override
        public void visitElement(PsiElement element) {
            if(TwigPattern.getAutocompletableAssetPattern().accepts(element) && TwigUtil.isValidStringWithoutInterpolatedOrConcat(element)) {
                invoke(element, holder);
            }

            super.visitElement(element);
        }

        private void invoke(@NotNull PsiElement element, @NotNull ProblemsHolder holder) {
            String asset = element.getText();

            if(StringUtils.isBlank(asset) || TwigUtil.resolveAssetsFiles(element.getProject(), asset).size() > 0) {
                return;
            }

            holder.registerProblem(element, "Missing asset");
        }
    }
}
