package fr.adrienbrault.idea.symfony2plugin.translation;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.formatter.FormatterUtil;
import com.jetbrains.php.lang.lexer.PhpTokenTypes;
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
        if (!(psiElement instanceof StringLiteralExpression)) {
            return;
        }

        PsiElement parameterList = psiElement.getContext();
        if (!(parameterList instanceof ParameterList)) {
            return;
        }

        PsiElement methodReferenceOrNewExpression = parameterList.getContext();
        if (!(methodReferenceOrNewExpression instanceof MethodReference) && !(methodReferenceOrNewExpression instanceof NewExpression)) {
            return;
        }

        ASTNode previousNonWhitespaceSibling = FormatterUtil.getPreviousNonWhitespaceSibling(psiElement.getNode());

        if (previousNonWhitespaceSibling != null && previousNonWhitespaceSibling.getElementType() == PhpTokenTypes.opCOLON) {
            ASTNode previousNonWhitespaceSibling1 = FormatterUtil.getPreviousNonWhitespaceSibling(previousNonWhitespaceSibling);
            if (previousNonWhitespaceSibling1 != null && previousNonWhitespaceSibling1.getElementType() == PhpTokenTypes.IDENTIFIER) {
                String text = previousNonWhitespaceSibling1.getText();
                boolean isSupportedAttributeInsideContext = "domain".equals(text) && (
                    (methodReferenceOrNewExpression instanceof MethodReference && PhpElementsUtil.isMethodReferenceInstanceOf((MethodReference) methodReferenceOrNewExpression, TranslationUtil.PHP_TRANSLATION_SIGNATURES))
                        ||  (methodReferenceOrNewExpression instanceof NewExpression && PhpElementsUtil.isNewExpressionPhpClassWithInstance((NewExpression) methodReferenceOrNewExpression, TranslationUtil.PHP_TRANSLATION_TRANSLATABLE_MESSAGE))
                );

                if (isSupportedAttributeInsideContext) {
                    annotateTranslationDomain((StringLiteralExpression) psiElement, holder);
                }
            }

            return;
        }

        int domainParameter = getDomainParameter(methodReferenceOrNewExpression);

        if (domainParameter >= 0) {
            ParameterBag currentIndex = PsiElementUtils.getCurrentParameterIndex(psiElement);
            if(currentIndex != null && currentIndex.getIndex() == domainParameter) {
                annotateTranslationDomain((StringLiteralExpression) psiElement, holder);
            }
        }
    }

    public static int getDomainParameter(@NotNull PsiElement methodReferenceOrNewExpression) {
        if (methodReferenceOrNewExpression instanceof MethodReference && PhpElementsUtil.isMethodReferenceInstanceOf((MethodReference) methodReferenceOrNewExpression, TranslationUtil.PHP_TRANSLATION_SIGNATURES)) {
            int domainParameter = 2;

            if("transChoice".equals(((MethodReference) methodReferenceOrNewExpression).getName())) {
                domainParameter = 3;
            }

            return domainParameter;
        } else if(methodReferenceOrNewExpression instanceof NewExpression && PhpElementsUtil.isNewExpressionPhpClassWithInstance((NewExpression) methodReferenceOrNewExpression, TranslationUtil.PHP_TRANSLATION_TRANSLATABLE_MESSAGE)) {
            return 2;
        }

        return -1;
    }

    private void annotateTranslationDomain(StringLiteralExpression psiElement, @NotNull ProblemsHolder holder) {
        String contents = psiElement.getContents();
        if(StringUtils.isBlank(contents) || TranslationUtil.hasDomain(psiElement.getProject(), contents)) {
            return;
        }

        holder.registerProblem(psiElement, MESSAGE, ProblemHighlightType.GENERIC_ERROR_OR_WARNING, new TranslationDomainGuessTypoQuickFix(contents));
    }
}
