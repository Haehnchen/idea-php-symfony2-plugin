package fr.adrienbrault.idea.symfony2plugin.dic;

import fr.adrienbrault.idea.symfony2plugin.extension.ServiceCollector;
import fr.adrienbrault.idea.symfony2plugin.extension.ServiceCollectorParameter;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class DefaultServiceCollector implements ServiceCollector {

    private static Map<String, String> DEFAULTS = new HashMap<String, String>() {{
        put("request", "Symfony\\Component\\HttpFoundation\\Request"); // drop Symfony < 3.0 ?
        put("service_container", "Symfony\\Component\\DependencyInjection\\ContainerInterface");
        put("kernel", "Symfony\\Component\\HttpKernel\\KernelInterface");
        put("controller_resolver", "Symfony\\Component\\HttpKernel\\Controller\\ControllerResolverInterface");
    }};

    @Override
    public void collectServices(@NotNull ServiceCollectorParameter.Service parameter) {
        DEFAULTS.forEach(parameter::add);
    }

    @Override
    public void collectIds(@NotNull ServiceCollectorParameter.Id parameter) {
        parameter.addAll(DEFAULTS.keySet());
    }
}
