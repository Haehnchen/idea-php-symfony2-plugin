package fr.adrienbrault.idea.symfonyplugin.dic;

import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import org.jetbrains.annotations.NotNull;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ServiceReference extends AbstractServiceReference {

    public ServiceReference(@NotNull StringLiteralExpression element) {
        super(element);
        this.serviceId = element.getContents();
    }

    public ServiceReference(@NotNull StringLiteralExpression element, boolean usePrivateServices) {
        this(element);
        this.usePrivateServices = usePrivateServices;
    }

}
