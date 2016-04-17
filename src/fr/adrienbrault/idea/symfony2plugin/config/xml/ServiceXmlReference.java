package fr.adrienbrault.idea.symfony2plugin.config.xml;

import com.intellij.psi.PsiElement;
import fr.adrienbrault.idea.symfony2plugin.dic.AbstractServiceReference;
import org.jetbrains.annotations.NotNull;

public class ServiceXmlReference extends AbstractServiceReference {

    public ServiceXmlReference(@NotNull PsiElement element, String ServiceId) {
        super(element);
        serviceId = ServiceId;
    }

    public ServiceXmlReference(@NotNull PsiElement element, String ServiceId, boolean useIndexedServices) {
        this(element, ServiceId);
        this.useIndexedServices = useIndexedServices;
    }

}
