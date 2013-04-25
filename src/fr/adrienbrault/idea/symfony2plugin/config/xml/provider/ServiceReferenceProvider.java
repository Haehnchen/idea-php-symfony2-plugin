package fr.adrienbrault.idea.symfony2plugin.config.xml.provider;


import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceProvider;
import com.intellij.util.ProcessingContext;
import fr.adrienbrault.idea.symfony2plugin.dic.ServiceReference;
import org.jetbrains.annotations.NotNull;

public class ServiceReferenceProvider extends PsiReferenceProvider {

    @NotNull
    @Override
    public PsiReference[] getReferencesByElement(@NotNull PsiElement psiElement, @NotNull ProcessingContext processingContext) {
        // get the service name "service_container"
        String text = psiElement.getText();
        return new PsiReference[]{ new ServiceReference(psiElement, text.substring(1, text.length() - 1)) };
    }

}
