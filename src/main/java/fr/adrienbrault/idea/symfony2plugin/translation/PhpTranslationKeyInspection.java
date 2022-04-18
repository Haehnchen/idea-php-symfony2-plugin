package fr.adrienbrault.idea.symfony2plugin.translation;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.jetbrains.php.lang.psi.elements.MethodReference;
import com.jetbrains.php.lang.psi.elements.ParameterList;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.translation.dict.TranslationUtil;
import fr.adrienbrault.idea.symfony2plugin.translation.inspection.TranslationKeyGuessTypoQuickFix;
import fr.adrienbrault.idea.symfony2plugin.translation.inspection.TwigTranslationKeyInspection;
import fr.adrienbrault.idea.symfony2plugin.util.ParameterBag;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class PhpTranslationKeyInspection extends LocalInspectionTool {

    public static final String MESSAGE = TwigTranslationKeyInspection.MESSAGE;

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
        if (!(psiElement instanceof StringLiteralExpression) || !(psiElement.getContext() instanceof ParameterList)) {
            return;
        }

        ParameterList parameterList = (ParameterList) psiElement.getContext();
        PsiElement methodReference = parameterList.getContext();
        if (!(methodReference instanceof MethodReference)) {
            return;
        }

        if (!PhpElementsUtil.isMethodReferenceInstanceOf((MethodReference) methodReference, TranslationUtil.PHP_TRANSLATION_SIGNATURES)) {
            return;
        }

        ParameterBag currentIndex = PsiElementUtils.getCurrentParameterIndex(psiElement);
        if(currentIndex == null || currentIndex.getIndex() != 0) {
            return;
        }

        int domainParameter = 2;
        if("transChoice".equals(((MethodReference) methodReference).getName())) {
            domainParameter = 3;
        }

        PsiElement domainElement = PsiElementUtils.getMethodParameterPsiElementAt(parameterList, domainParameter);

        if(domainElement == null) {
            // no domain found; fallback to default domain
            annotateTranslationKey((StringLiteralExpression) psiElement, "messages", holder);
        } else {
            // resolve string in parameter
            PsiElement[] parameters = parameterList.getParameters();
            if(parameters.length >= domainParameter) {
                String domain = PhpElementsUtil.getStringValue(parameters[domainParameter]);
                if(domain != null) {
                    annotateTranslationKey((StringLiteralExpression) psiElement, domain, holder);
                }
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
