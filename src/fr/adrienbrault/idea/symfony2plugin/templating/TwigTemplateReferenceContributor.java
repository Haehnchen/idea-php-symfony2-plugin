package fr.adrienbrault.idea.symfony2plugin.templating;

import com.intellij.psi.*;
import com.intellij.util.ProcessingContext;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import org.jetbrains.annotations.NotNull;

/**
 * @author Adrien Brault <adrien.brault@gmail.com>
 *
 * TODO This does not seem to be used by phpstorm ... using the completion contributor as a fallback for now
 */
public class TwigTemplateReferenceContributor extends PsiReferenceContributor {

    @Override
    public void registerReferenceProviders(PsiReferenceRegistrar psiReferenceRegistrar) {
        psiReferenceRegistrar.registerReferenceProvider(
            TemplateHelper.getAutocompletableTemplatePattern(),
            new PsiReferenceProvider() {
                @NotNull
                @Override
                public PsiReference[] getReferencesByElement(@NotNull PsiElement psiElement, @NotNull ProcessingContext processingContext) {
                    return new PsiReference[]{ new TemplateReference((StringLiteralExpression) psiElement) };
                }
            }
        );
    }

}
