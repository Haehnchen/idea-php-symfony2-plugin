package fr.adrienbrault.idea.symfony2plugin.translation.inspection;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.jetbrains.twig.TwigTokenTypes;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.templating.TwigPattern;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import fr.adrienbrault.idea.symfony2plugin.translation.dict.TranslationUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TwigTranslationDomainInspection extends LocalInspectionTool {

    public static final String MESSAGE = "Symfony: Missing translation domain";

    @NotNull
    public PsiElementVisitor buildVisitor(final @NotNull ProblemsHolder holder, boolean isOnTheFly) {
        if(!Symfony2ProjectComponent.isEnabled(holder.getProject())) {
            return super.buildVisitor(holder, isOnTheFly);
        }

        return new MyTranslationDomainPsiElementVisitor(holder);
    }

    /**
     * 'foo'|trans({}, 'foobar')
     * 'foo'|transchoice({}, null, 'foobar')
     */
    private static class MyTranslationDomainPsiElementVisitor extends PsiElementVisitor {
        private final ProblemsHolder holder;

        MyTranslationDomainPsiElementVisitor(ProblemsHolder holder) {
            this.holder = holder;
        }

        @Override
        public void visitElement(@NotNull PsiElement psiElement) {
            if(!TwigPattern.getTransDomainPattern().accepts(psiElement)) {
                return;
            }

            // @TODO: move to pattern, dont allow nested filters: eg "'form.tab.profile'|trans|desc('Interchange')"
            final PsiElement[] psiElementTrans = new PsiElement[1];
            PsiElementUtils.getPrevSiblingOnCallback(psiElement, psiElement1 -> {
                if(psiElement1.getNode().getElementType() == TwigTokenTypes.FILTER) {
                    return false;
                } else {
                    if(PlatformPatterns.psiElement(TwigTokenTypes.IDENTIFIER).withText(PlatformPatterns.string().oneOf("trans", "transchoice")).accepts(psiElement1)) {
                        psiElementTrans[0] = psiElement1;
                    }
                }

                return true;
            });

            if(psiElementTrans[0] != null && TwigUtil.getTwigMethodString(psiElementTrans[0]) != null) {
                String text = psiElement.getText();
                if(StringUtils.isNotBlank(text) && !TranslationUtil.hasDomain(psiElement.getProject(), text)) {
                    holder.registerProblem(
                        psiElement,
                        MESSAGE,
                        ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                        new TranslationDomainGuessTypoQuickFix(text)
                    );
                }
            }

            super.visitElement(psiElement);
        }
    }
}
