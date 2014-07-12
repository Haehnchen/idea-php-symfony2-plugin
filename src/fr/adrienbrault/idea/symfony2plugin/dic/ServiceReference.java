package fr.adrienbrault.idea.symfony2plugin.dic;

import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import org.jetbrains.annotations.NotNull;


public class ServiceReference extends AbstractServiceReference {

    public ServiceReference(@NotNull StringLiteralExpression element) {
        super(element);
        this.serviceId = element.getContents();
    }

    public ServiceReference(@NotNull StringLiteralExpression element, boolean useIndexedServices) {
        this(element);
        this.useIndexedServices = useIndexedServices;
   }

    public ServiceReference(@NotNull StringLiteralExpression element, boolean useIndexedServices, boolean usePrivateServices) {
        this(element);
        this.useIndexedServices = useIndexedServices;
        this.usePrivateServices = usePrivateServices;
    }

}
