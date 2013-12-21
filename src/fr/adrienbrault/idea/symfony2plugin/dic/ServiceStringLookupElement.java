package fr.adrienbrault.idea.symfony2plugin.dic;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementPresentation;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import org.jetbrains.annotations.NotNull;

public class ServiceStringLookupElement extends LookupElement {

    private String serviceId;
    private String serviceClass;
    private boolean isWeakIndexService = false;
    private ContainerService containerService;

    public ServiceStringLookupElement(ContainerService containerService) {
        this(containerService.getName(), containerService.getClassName(), containerService.isWeak());
        this.containerService = containerService;
    }

    public ServiceStringLookupElement(String serviceId) {
        this.serviceId = serviceId;
    }

    public ServiceStringLookupElement(String serviceId, String serviceClass) {
        this(serviceId);
        this.serviceClass = serviceClass;
    }

    public ServiceStringLookupElement(String serviceId, String serviceClass, boolean isWeakIndexService) {
        this(serviceId, serviceClass);
        this.isWeakIndexService = isWeakIndexService;
    }

    @NotNull
    @Override
    public String getLookupString() {
        return serviceId;
    }

    public void renderElement(LookupElementPresentation presentation) {
        presentation.setItemText(getLookupString());
        presentation.setTypeGrayed(true);

        // private or non container services
        if(this.serviceClass == null || this.isWeakIndexService) {
            presentation.setIcon(Symfony2Icons.SERVICE_OPACITY);
        } else {
            presentation.setIcon(Symfony2Icons.SERVICE);
        }

        if(this.containerService != null && this.containerService.isPrivate()) {
            presentation.setIcon(Symfony2Icons.SERVICE_PRIVATE_OPACITY);
        }

        if(serviceClass != null) {
            // classnames have "\", make it more readable
            if(serviceClass.startsWith("\\")) {
                serviceClass = serviceClass.substring(1);
            }
            presentation.setTypeText(serviceClass);
        }

    }

}
