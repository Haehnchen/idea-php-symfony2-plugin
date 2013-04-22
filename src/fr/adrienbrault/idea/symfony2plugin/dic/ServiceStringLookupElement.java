package fr.adrienbrault.idea.symfony2plugin.dic;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementPresentation;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import org.jetbrains.annotations.NotNull;

public class ServiceStringLookupElement extends LookupElement {

    private String serviceId;
    private String serviceClass;

    public ServiceStringLookupElement(String serviceId, String serviceClass) {
        this.serviceId = serviceId;
        this.serviceClass = serviceClass;
    }

    @NotNull
    @Override
    public String getLookupString() {
        return serviceId;
    }

    public void renderElement(LookupElementPresentation presentation) {
        presentation.setItemText(getLookupString());
        presentation.setTypeText(serviceClass);
        presentation.setTypeGrayed(true);
        presentation.setIcon(Symfony2Icons.SYMFONY);
    }
}
