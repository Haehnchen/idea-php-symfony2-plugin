package fr.adrienbrault.idea.symfony2plugin.translation;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.jetbrains.php.lang.psi.elements.*;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.translation.dict.TranslationUtil;
import fr.adrienbrault.idea.symfony2plugin.translation.inspection.TranslationDomainGuessTypoQuickFix;
import fr.adrienbrault.idea.symfony2plugin.translation.inspection.TwigTranslationDomainInspection;
import fr.adrienbrault.idea.symfony2plugin.util.ParameterBag;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class PhpTranslationDomainInspection extends LocalInspectionTool {

    public static final String MESSAGE = TwigTranslationDomainInspection.MESSAGE;

    @NotNull
    @Override
    public PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        if(!Symfony2ProjectComponent.isEnabled(holder.getProject())) {
            return super.buildVisitor(holder, isOnTheFly);
        }

        return new PsiElementVisitor() {
            @Override
            public void visitElement(PsiElement element) {
                invoke(holder, element);
                super.visitElement(element);
            }
        };
    }

    private void invoke(@NotNull ProblemsHolder holder, @NotNull PsiElement psiElement) {
        if (!(psiElement instanceof StringLiteralExpression) || !(psiElement.getContext() instanceof ParameterList)) {
            return;
        }

        ParameterList parameterList = (ParameterList) psiElement.getContext();

        int domainParameter = -1;
        PsiElement methodReference = parameterList.getContext();
        if (methodReference instanceof MethodReference && PhpElementsUtil.isMethodReferenceInstanceOf((MethodReference) methodReference, TranslationUtil.PHP_TRANSLATION_SIGNATURES)) {
            domainParameter = 2;
            if("transChoice".equals(((MethodReference) methodReference).getName())) {
                domainParameter = 3;
            }
        } else if(methodReference instanceof NewExpression && PhpElementsUtil.isNewExpressionPhpClassWithInstance((NewExpression) methodReference, TranslationUtil.PHP_TRANSLATION_TRANSLATABLE_MESSAGE)) {
            domainParameter = 2;
        }

        if (domainParameter >= 0) {
            ParameterBag currentIndex = PsiElementUtils.getCurrentParameterIndex(psiElement);
            if(currentIndex != null && currentIndex.getIndex() == domainParameter) {
                annotateTranslationDomain((StringLiteralExpression) psiElement, holder);
            }
        }
    }

    private void annotateTranslationDomain(StringLiteralExpression psiElement, @NotNull ProblemsHolder holder) {
        String contents = psiElement.getContents();
        if(StringUtils.isBlank(contents) || TranslationUtil.hasDomain(psiElement.getProject(), contents)) {
            return;
        }

        holder.registerProblem(psiElement, MESSAGE, ProblemHighlightType.GENERIC_ERROR_OR_WARNING, new TranslationDomainGuessTypoQuickFix(contents));
    }
}
