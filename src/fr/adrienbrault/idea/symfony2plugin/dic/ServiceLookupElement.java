package fr.adrienbrault.idea.symfony2plugin.dic;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementPresentation;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import org.jetbrains.annotations.NotNull;

/**
 * @author Adrien Brault <adrien.brault@gmail.com>
 */
public class ServiceLookupElement extends LookupElement {

    private String serviceId;
    private PhpClass serviceClass;

    public ServiceLookupElement(String serviceId, PhpClass serviceClass) {
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
        presentation.setTypeText(serviceClass.getPresentableFQN());
        presentation.setTypeGrayed(true);
        presentation.setIcon(Symfony2Icons.SYMFONY);
    }
}
