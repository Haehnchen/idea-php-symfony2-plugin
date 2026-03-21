package fr.adrienbrault.idea.symfony2plugin.translation.inspection;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.patterns.ElementPattern;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.templating.TwigPattern;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import fr.adrienbrault.idea.symfony2plugin.translation.TranslationKeyIntentionAndQuickFixAction;
import fr.adrienbrault.idea.symfony2plugin.translation.dict.TranslationUtil;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import static fr.adrienbrault.idea.symfony2plugin.util.StringUtils.isInterpolatedString;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TwigTranslationKeyInspection extends LocalInspectionTool {

    public static final String MESSAGE = "Symfony: Missing translation key";

    @NotNull
    public PsiElementVisitor buildVisitor(final @NotNull ProblemsHolder holder, boolean isOnTheFly) {
        if(!Symfony2ProjectComponent.isEnabled(holder.getProject())) {
            return super.buildVisitor(holder, isOnTheFly);
        }

        return new MyTranslationKeyPsiElementVisitor(holder);
    }

    private static class MyTranslationKeyPsiElementVisitor extends PsiElementVisitor {
        private final ProblemsHolder holder;

        private ElementPattern<PsiElement> translationKeyPattern;

        MyTranslationKeyPsiElementVisitor(ProblemsHolder holder) {
            this.holder = holder;
        }

        @Override
        public void visitElement(PsiElement psiElement) {
            if(!getTranslationKeyPattern().accepts(psiElement)) {
                super.visitElement(psiElement);
                return;
            }

            String text = psiElement.getText();
            if(StringUtils.isBlank(text) || isInterpolatedString(text)) {
                super.visitElement(psiElement);
                return;
            }

            // get domain on file scope or method parameter
            String domainName = TwigUtil.getPsiElementTranslationDomain(psiElement);

            if(TranslationUtil.hasTranslationKey(psiElement.getProject(), text, domainName)) {
                super.visitElement(psiElement);
                return;
            }

            holder.registerProblem(
                psiElement,
                MESSAGE,
                new TranslationKeyIntentionAndQuickFixAction(text, domainName),
                new TranslationKeyGuessTypoQuickFix(text, domainName)
            );

            super.visitElement(psiElement);
        }

        private ElementPattern<PsiElement> getTranslationKeyPattern() {
            return translationKeyPattern != null ? translationKeyPattern : (translationKeyPattern = TwigPattern.getTranslationKeyPattern("trans", "transchoice"));
        }
    }
}
