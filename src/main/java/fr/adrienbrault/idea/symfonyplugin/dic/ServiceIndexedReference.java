package fr.adrienbrault.idea.symfonyplugin.dic;

import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import org.jetbrains.annotations.NotNull;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ServiceIndexedReference extends ServiceReference {
    public ServiceIndexedReference(@NotNull StringLiteralExpression element) {
        super(element, true);
    }
}
