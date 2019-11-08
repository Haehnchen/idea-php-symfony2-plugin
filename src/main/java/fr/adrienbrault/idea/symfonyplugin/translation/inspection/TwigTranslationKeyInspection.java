package fr.adrienbrault.idea.symfonyplugin.translation.inspection;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import fr.adrienbrault.idea.symfonyplugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfonyplugin.templating.TwigPattern;
import fr.adrienbrault.idea.symfonyplugin.templating.util.TwigUtil;
import fr.adrienbrault.idea.symfonyplugin.translation.TranslationKeyIntentionAndQuickFixAction;
import fr.adrienbrault.idea.symfonyplugin.translation.dict.TranslationUtil;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TwigTranslationKeyInspection extends LocalInspectionTool {

    public static final String MESSAGE = "Missing translation key";

    @NotNull
    public PsiElementVisitor buildVisitor(final @NotNull ProblemsHolder holder, boolean isOnTheFly) {
        if(!Symfony2ProjectComponent.isEnabled(holder.getProject())) {
            return super.buildVisitor(holder, isOnTheFly);
        }

        return new MyTranslationKeyPsiElementVisitor(holder);
    }

    private static class MyTranslationKeyPsiElementVisitor extends PsiElementVisitor {
        private final ProblemsHolder holder;

        MyTranslationKeyPsiElementVisitor(ProblemsHolder holder) {
            this.holder = holder;
        }

        @Override
        public void visitElement(PsiElement psiElement) {
            if(!TwigPattern.getTranslationKeyPattern("trans", "transchoice").accepts(psiElement)) {
                super.visitElement(psiElement);
                return;
            }

            String text = psiElement.getText();
            if(StringUtils.isBlank(text) || fr.adrienbrault.idea.symfonyplugin.util.StringUtils.isInterpolatedString(text)) {
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
                new TranslationKeyIntentionAndQuickFixAction(text, domainName)
            );

            super.visitElement(psiElement);
        }
    }
}
