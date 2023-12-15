package fr.adrienbrault.idea.symfony2plugin.templating;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.jetbrains.php.lang.psi.elements.MethodReference;
import com.jetbrains.php.lang.psi.elements.ParameterList;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.templating.inspection.TemplateCreateByNameLocalQuickFix;
import fr.adrienbrault.idea.symfony2plugin.templating.inspection.TemplateGuessTypoQuickFix;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import fr.adrienbrault.idea.symfony2plugin.util.ParameterBag;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class PhpTemplateMissingInspection extends LocalInspectionTool {
    @NotNull
    @Override
    public PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        if(!Symfony2ProjectComponent.isEnabled(holder.getProject())) {
            return super.buildVisitor(holder, isOnTheFly);
        }

        return new PsiElementVisitor() {
            @Override
            public void visitElement(@NotNull PsiElement element) {
                invoke(holder, element);
                super.visitElement(element);
            }
        };
    }

    private void invoke(@NotNull ProblemsHolder holder, @NotNull PsiElement psiElement) {
        if (!(psiElement instanceof StringLiteralExpression)) {
            return;
        }

        String templateNameIfMissing = getTemplateNameIfMissing((StringLiteralExpression) psiElement);
        if(templateNameIfMissing == null) {
            return;
        }

        LocalQuickFix[] templateCreateByNameLocalQuickFix = new LocalQuickFix[]{
            new TemplateCreateByNameLocalQuickFix(templateNameIfMissing),
            new TemplateGuessTypoQuickFix(templateNameIfMissing)
        };

        holder.registerProblem(
            psiElement,
            "Twig: Missing Template",
            ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
            templateCreateByNameLocalQuickFix
        );
    }

    @Nullable
    private String getTemplateNameIfMissing(@NotNull StringLiteralExpression psiElement) {
        ParameterBag parameterBag = PsiElementUtils.getCurrentParameterIndex(psiElement);
        if(parameterBag == null || parameterBag.getIndex() != 0) {
            return null;
        }

        PsiElement parameterList = psiElement.getParent();
        if (!(parameterList instanceof ParameterList)) {
            return null;
        }

        PsiElement methodReference = parameterList.getParent();
        if (!(methodReference instanceof MethodReference)) {
            return null;
        }

        if(Arrays.stream(psiElement.getReferences()).noneMatch(ref -> ref instanceof TemplateReference)) {
            return null;
        }

        String templateName = PhpElementsUtil.getFirstArgumentStringValue((MethodReference) methodReference);
        if(templateName == null || StringUtils.isBlank(templateName)) {
            return null;
        }

        if(TwigUtil.getTemplateFiles(psiElement.getProject(), templateName).size() > 0) {
            return null;
        }

        return templateName;
    }
}
