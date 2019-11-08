package fr.adrienbrault.idea.symfonyplugin.config.xml.provider;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceProvider;
import com.intellij.util.ProcessingContext;
import fr.adrienbrault.idea.symfonyplugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfonyplugin.config.xml.ServiceXmlReference;
import fr.adrienbrault.idea.symfonyplugin.util.PsiElementUtils;
import org.jetbrains.annotations.NotNull;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ServiceReferenceProvider extends PsiReferenceProvider {

    @NotNull
    @Override
    public PsiReference[] getReferencesByElement(@NotNull PsiElement psiElement, @NotNull ProcessingContext processingContext) {

        if(!Symfony2ProjectComponent.isEnabled(psiElement)) {
            return new PsiReference[0];
        }

        // get the service name "service_container"
        String text = PsiElementUtils.trimQuote(psiElement.getText());

        return new PsiReference[]{ new ServiceXmlReference(psiElement, text) };
    }

}
