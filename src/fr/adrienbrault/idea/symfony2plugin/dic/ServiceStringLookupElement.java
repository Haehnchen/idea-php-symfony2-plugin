package fr.adrienbrault.idea.symfony2plugin.dic;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementPresentation;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import org.jetbrains.annotations.NotNull;

public class ServiceStringLookupElement extends LookupElement {

    private String serviceId;
    private String serviceClass;

    public ServiceStringLookupElement(String serviceId) {
        this.serviceId = serviceId;
    }

    public ServiceStringLookupElement(String serviceId, String serviceClass) {
        this(serviceId);
        this.serviceClass = serviceClass;
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
        if(serviceClass == null) {
            presentation.setIcon(Symfony2Icons.SERVICE_OPACITY);
            return;
        }

        // classnames have "\", more it more readable
        if(serviceClass.startsWith("\\")) {
            serviceClass = serviceClass.substring(1);
        }

        presentation.setTypeText(serviceClass);
        presentation.setIcon(Symfony2Icons.SERVICE);



    }

}
