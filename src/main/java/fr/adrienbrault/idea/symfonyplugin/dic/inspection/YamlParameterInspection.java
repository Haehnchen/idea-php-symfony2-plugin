package fr.adrienbrault.idea.symfonyplugin.dic.inspection;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import fr.adrienbrault.idea.symfonyplugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfonyplugin.config.yaml.YamlElementPatternHelper;
import fr.adrienbrault.idea.symfonyplugin.stubs.ContainerCollectionResolver;
import fr.adrienbrault.idea.symfonyplugin.util.PsiElementUtils;
import fr.adrienbrault.idea.symfonyplugin.util.yaml.YamlHelper;
import org.jetbrains.annotations.NotNull;

/**
 * Check if service parameter exists
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class YamlParameterInspection extends LocalInspectionTool {
    @NotNull
    public PsiElementVisitor buildVisitor(final @NotNull ProblemsHolder holder, boolean isOnTheFly) {
        if (!Symfony2ProjectComponent.isEnabled(holder.getProject())) {
            return super.buildVisitor(holder, isOnTheFly);
        }

        return new PsiElementVisitor() {
            @Override
            public void visitElement(PsiElement psiElement) {
                if(YamlElementPatternHelper.getServiceParameterDefinition().accepts(psiElement) && YamlElementPatternHelper.getInsideServiceKeyPattern().accepts(psiElement)) {
                    invoke(psiElement, holder);
                }

                super.visitElement(psiElement);
            }
        };
    }

    private void invoke(@NotNull final PsiElement psiElement, @NotNull ProblemsHolder holder) {
        // at least %a%
        // and not this one: %kernel.root_dir%/../web/
        // %kernel.root_dir%/../web/%webpath_modelmasks%
        String parameterName = PsiElementUtils.getText(psiElement);
        if(!YamlHelper.isValidParameterName(parameterName)) {
            return;
        }

        // strip "%"
        parameterName = parameterName.substring(1, parameterName.length() - 1);

        // parameter a always lowercase see #179
        parameterName = parameterName.toLowerCase();
        if (!ContainerCollectionResolver.getParameterNames(psiElement.getProject()).contains(parameterName)) {
            holder.registerProblem(psiElement, "Missing Parameter", ProblemHighlightType.GENERIC_ERROR_OR_WARNING);
        }
    }
}
