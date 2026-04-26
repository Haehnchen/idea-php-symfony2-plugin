package fr.adrienbrault.idea.symfony2plugin.translation;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.jetbrains.php.lang.psi.elements.*;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.translation.dict.TranslationUtil;
import fr.adrienbrault.idea.symfony2plugin.translation.inspection.TranslationKeyGuessTypoQuickFix;
import fr.adrienbrault.idea.symfony2plugin.translation.inspection.TwigTranslationKeyInspection;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class PhpTranslationKeyInspection extends LocalInspectionTool {

    public static final String MESSAGE = TwigTranslationKeyInspection.MESSAGE;

    @NotNull
    @Override
    public PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        if (!Symfony2ProjectComponent.isEnabled(holder.getProject())) {
            return super.buildVisitor(holder, isOnTheFly);
        }

        return new PsiElementVisitor() {
            @Override
            public void visitElement(@NotNull PsiElement element) {
                if (element instanceof StringLiteralExpression stringLiteralExpression) {
                    invoke(holder, stringLiteralExpression);
                }
                super.visitElement(element);
            }
        };
    }

    private void invoke(@NotNull ProblemsHolder holder, @NotNull StringLiteralExpression psiElement) {
        ParameterListOwner methodReferenceOrNewExpression = TranslationUtil.getTranslationFunctionContext(psiElement);
        if (methodReferenceOrNewExpression == null) {
            return;
        }

        if (!PsiElementUtils.isCurrentParameter(psiElement, "id", 0)) {
            return;
        }

        if (!TranslationUtil.isTranslationReference(methodReferenceOrNewExpression)) {
            return;
        }

        ParameterList parameterList = (ParameterList) psiElement.getContext();
        PsiElement domainElement = parameterList.getParameter("domain", PhpTranslationDomainInspection.getDomainParameter(methodReferenceOrNewExpression));
        if(domainElement == null) {
            // no domain found; fallback to default domain
            annotateTranslationKey(psiElement, "messages", holder);
        } else {
            // resolve string in parameter
            String domain = PhpElementsUtil.getStringValue(domainElement);
            if(domain != null) {
                annotateTranslationKey(psiElement, domain, holder);
            }
        }
    }

    private void annotateTranslationKey(@NotNull StringLiteralExpression psiElement, @NotNull String domainName, @NotNull ProblemsHolder holder) {
        String keyName = psiElement.getContents();

        // should not annotate "foo$bar"
        // @TODO: regular expression to only notice translation keys and not possible text values
        if(StringUtils.isBlank(keyName) || keyName.contains("$")) {
            return;
        }

        // dont annotate non goto available keys
        if(TranslationUtil.hasTranslationKey(psiElement.getProject(), keyName, domainName)) {
            return;
        }

        holder.registerProblem(
            psiElement,
            MESSAGE,
            new TranslationKeyIntentionAndQuickFixAction(keyName, domainName),
            new TranslationKeyGuessTypoQuickFix(keyName, domainName)
        );
    }
}
