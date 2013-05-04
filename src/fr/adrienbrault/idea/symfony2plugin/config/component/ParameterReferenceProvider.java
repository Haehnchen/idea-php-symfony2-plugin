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

    private boolean trimQuote = false;
    private boolean trimPercent = false;

    public ParameterReferenceProvider setTrimPercent(boolean trimPercent) {
        this.trimPercent = trimPercent;
        return this;
    }

    public ParameterReferenceProvider setTrimQuote(boolean trimQuote) {
        this.trimQuote = trimQuote;
        return this;
    }

    @NotNull
    @Override
    public PsiReference[] getReferencesByElement(@NotNull PsiElement psiElement, @NotNull ProcessingContext processingContext) {

        String text = psiElement.getText();

        // xml attributes are wrapped with quote
        if(this.trimQuote) {
            text = this.trimChar(text, "\"");
        }

        // xml need wrapped %, php and other dont
        if(this.trimPercent) {
            text = this.trimChar(text, "%");
        }

        return new PsiReference[]{ new ParameterReference(psiElement, text).wrapVariantsWithPercent(true) };
    }

    protected String trimChar(String text, String charString) {
        if(text.length() >= 2) {
            if(text.startsWith(charString)) {
                text = text.substring(1, text.length() - 1);
            }

            if(text.endsWith(charString)) {
                text = text.substring(1, text.length() - 1);
            }
        }

        return text;
    }

}
