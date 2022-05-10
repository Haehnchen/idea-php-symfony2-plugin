package fr.adrienbrault.idea.symfony2plugin.config.yaml;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import fr.adrienbrault.idea.symfony2plugin.dic.AbstractServiceReference;
import org.jetbrains.annotations.NotNull;

public class ServiceYamlReference extends AbstractServiceReference {

    public ServiceYamlReference(@NotNull PsiElement psiElement, @NotNull String serviceId) {
        super(psiElement);

        this.serviceId = serviceId;
    }

    public ServiceYamlReference(@NotNull PsiElement psiElement, @NotNull TextRange range, @NotNull String serviceId) {
        super(psiElement, range);

        this.serviceId = serviceId;
    }

    @Override
    public @NotNull Object[] getVariants() {
        return new Object[0];
    }
}
