package fr.adrienbrault.idea.symfony2plugin.templating.inspection;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.jetbrains.php.lang.psi.elements.MethodReference;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

/**
 * PHP version of "Twig" "asset" function
 *
 * "Package*::getUrl"
 * "Package*::getVersion"
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class PhpAssetMissingInspection extends LocalInspectionTool {
    @NotNull
    public PsiElementVisitor buildVisitor(final @NotNull ProblemsHolder holder, boolean isOnTheFly) {
        if(!Symfony2ProjectComponent.isEnabled(holder.getProject())) {
            return super.buildVisitor(holder, isOnTheFly);
        }

        return new AssetPackageElementVisitor(holder);
    }

    private static class AssetPackageElementVisitor extends PsiElementVisitor {
        private final ProblemsHolder holder;

        AssetPackageElementVisitor(ProblemsHolder holder) {
            this.holder = holder;
        }

        @Override
        public void visitElement(@NotNull PsiElement element) {
            if (element instanceof StringLiteralExpression) {
                MethodReference methodReference = PsiElementUtils.getMethodReferenceWithFirstStringParameter((StringLiteralExpression) element);
                if (methodReference != null && (PhpElementsUtil.isMethodReferenceInstanceOf(methodReference, "\\Symfony\\Component\\Asset\\Packages", "getUrl")
                    || PhpElementsUtil.isMethodReferenceInstanceOf(methodReference, "\\Symfony\\Component\\Asset\\Packages", "getVersion")
                    || PhpElementsUtil.isMethodReferenceInstanceOf(methodReference, "\\Symfony\\Component\\Asset\\PackageInterface", "getUrl")
                    || PhpElementsUtil.isMethodReferenceInstanceOf(methodReference, "\\Symfony\\Component\\Asset\\PackageInterface", "getVersion")
                )) {
                    invoke((StringLiteralExpression) element, holder);
                }
            }
            super.visitElement(element);
        }

        private void invoke(@NotNull StringLiteralExpression element, @NotNull ProblemsHolder holder) {
            String asset = element.getContents();
            if(StringUtils.isBlank(asset) || !TwigUtil.resolveAssetsFiles(element.getProject(), asset).isEmpty()) {
                return;
            }

            holder.registerProblem(element, "Symfony: Missing asset");
        }
    }
}
