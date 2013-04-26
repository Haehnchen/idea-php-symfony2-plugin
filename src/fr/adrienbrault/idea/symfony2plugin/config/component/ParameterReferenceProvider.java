package fr.adrienbrault.idea.symfony2plugin.config.component;


import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceProvider;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ParameterReferenceProvider extends PsiReferenceProvider {


    private boolean trimQuote = true;

    public ParameterReferenceProvider() {
    }

    public ParameterReferenceProvider(boolean trimQuoteChar) {
        trimQuote = trimQuoteChar;
    }

    @NotNull
    @Override
    public PsiReference[] getReferencesByElement(@NotNull PsiElement psiElement, @NotNull ProcessingContext processingContext) {

        String text = psiElement.getText();

        // xml are wrapped with quote
        if(trimQuote && text.length() >= 2) {
            if(text.startsWith("\"")) {
                text = text.substring(1, text.length() - 1);
            }

            if(text.endsWith("\"")) {
                text = text.substring(1, text.length() - 1);
            }
        }

        return new PsiReference[]{ new ParameterReference(psiElement, text) };
    }

}
