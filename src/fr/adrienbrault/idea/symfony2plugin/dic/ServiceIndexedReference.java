package fr.adrienbrault.idea.symfony2plugin.dic;

import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import org.jetbrains.annotations.NotNull;

public class ServiceIndexedReference extends ServiceReference {
    public ServiceIndexedReference(@NotNull StringLiteralExpression element) {
        super(element, true);
    }
}
