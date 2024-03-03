package fr.adrienbrault.idea.symfony2plugin.dic;

import fr.adrienbrault.idea.symfony2plugin.extension.ServiceCollector;
import fr.adrienbrault.idea.symfony2plugin.extension.ServiceCollectorParameter;
import fr.adrienbrault.idea.symfony2plugin.util.SymfonyUtil;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class DefaultServiceCollector implements ServiceCollector {

    private static final Map<String, String> DEFAULTS = new HashMap<>() {{
        put("service_container", "Symfony\\Component\\DependencyInjection\\ContainerInterface");
        put("kernel", "Symfony\\Component\\HttpKernel\\KernelInterface");
        put("controller_resolver", "Symfony\\Component\\HttpKernel\\Controller\\ControllerResolverInterface");
    }};

    @Override
    public void collectServices(@NotNull ServiceCollectorParameter.Service parameter) {
        DEFAULTS.forEach(parameter::add);

        if(SymfonyUtil.isVersionLessThen(parameter.getProject(), "3.0")) {
            parameter.add("request", "Symfony\\Component\\HttpFoundation\\Request");
        }
    }

    @Override
    public void collectIds(@NotNull ServiceCollectorParameter.Id parameter) {
        parameter.addAll(DEFAULTS.keySet());

        if(SymfonyUtil.isVersionLessThen(parameter.getProject(), "3.0")) {
            parameter.add("request");
        }
    }
}
