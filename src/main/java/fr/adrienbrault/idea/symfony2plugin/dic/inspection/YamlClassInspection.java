package fr.adrienbrault.idea.symfony2plugin.dic.inspection;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.jetbrains.php.PhpIndex;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.config.yaml.YamlElementPatternHelper;
import fr.adrienbrault.idea.symfony2plugin.stubs.ContainerCollectionResolver;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlHelper;
import org.jetbrains.annotations.NotNull;

/**
 * Check if class exists
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class YamlClassInspection extends LocalInspectionTool {
    @NotNull
    public PsiElementVisitor buildVisitor(final @NotNull ProblemsHolder holder, boolean isOnTheFly) {
        if (!Symfony2ProjectComponent.isEnabled(holder.getProject())) {
            return super.buildVisitor(holder, isOnTheFly);
        }

        return new PsiElementVisitor() {
            @Override
            public void visitElement(PsiElement psiElement) {
                if(!((YamlElementPatternHelper.getSingleLineScalarKey("class", "factory_class").accepts(psiElement) || YamlElementPatternHelper.getParameterClassPattern().accepts(psiElement)) && YamlElementPatternHelper.getInsideServiceKeyPattern().accepts(psiElement))) {
                    super.visitElement(psiElement);
                    return;
                }

                invoke(psiElement, holder);

                super.visitElement(psiElement);
            }
        };
    }

    private void invoke(@NotNull final PsiElement psiElement, @NotNull ProblemsHolder holder) {
        String className = PsiElementUtils.getText(psiElement);

        if(YamlHelper.isValidParameterName(className)) {
            String resolvedParameter = ContainerCollectionResolver.resolveParameter(psiElement.getProject(), className);
            if(resolvedParameter != null && PhpIndex.getInstance(psiElement.getProject()).getAnyByFQN(resolvedParameter).size() > 0) {
                return ;
            }
        }

        if(PhpElementsUtil.getClassInterface(psiElement.getProject(), className) == null) {
            holder.registerProblem(psiElement, "Missing class", ProblemHighlightType.GENERIC_ERROR_OR_WARNING);
        }
    }
}
