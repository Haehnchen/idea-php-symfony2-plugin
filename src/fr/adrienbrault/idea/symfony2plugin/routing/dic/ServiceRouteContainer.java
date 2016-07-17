package fr.adrienbrault.idea.symfony2plugin.routing.dic;

import com.intellij.openapi.project.Project;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.routing.Route;
import fr.adrienbrault.idea.symfony2plugin.routing.RouteHelper;
import fr.adrienbrault.idea.symfony2plugin.stubs.ContainerCollectionResolver;
import fr.adrienbrault.idea.symfony2plugin.util.dict.ServiceUtil;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ServiceRouteContainer  {

    private final Collection<Route> routes;
    private ContainerCollectionResolver.LazyServiceCollector lazyServiceCollector;

    private Map<String, PhpClass> serviceCache = new HashMap<>();

    private ServiceRouteContainer(@NotNull Collection<Route> routes) {
        this.routes = routes;
    }

    public Set<String> getServiceNames() {

        Set<String> services = new HashSet<>();

        for (Route route : this.routes) {

            String controller = route.getController();
            if(controller == null) {
                continue;
            }

            String[] split = controller.split(":");
            if(split.length > 1) {
                services.add(split[0]);
            }

        }

        return services;
    }

    @NotNull
    public Collection<Route> getMethodMatches(@NotNull Method method) {
        PhpClass originClass = method.getContainingClass();
        if(originClass == null) {
            return Collections.emptyList();
        }

        String classFqn = StringUtils.stripStart(originClass.getFQN(), "\\");

        Collection<Route> routes = new ArrayList<>();
        for (Route route : this.routes) {

            String serviceRoute = route.getController();
            if(serviceRoute == null) {
                continue;
            }

            // if controller matches:
            // service_id:methodName
            String[] split = serviceRoute.split(":");
            if(split.length != 2 || !split[1].equals(method.getName())) {
                continue;
            }

            // cache PhpClass resolve
            if(!serviceCache.containsKey(split[0])) {
                serviceCache.put(split[0], ServiceUtil.getResolvedClassDefinition(method.getProject(), split[0], getLazyServiceCollector(method.getProject())));
            }

            PhpClass phpClass = serviceCache.get(split[0]);
            if(phpClass != null && classFqn.equals(phpClass.getPresentableFQN())) {
                Method targetMethod = phpClass.findMethodByName(split[1]);
                if(targetMethod != null) {
                    routes.add(route);
                }
            }
        }

        return routes;
    }

    private ContainerCollectionResolver.LazyServiceCollector getLazyServiceCollector(Project project) {
        return this.lazyServiceCollector == null ? this.lazyServiceCollector = new ContainerCollectionResolver.LazyServiceCollector(project) : this.lazyServiceCollector;
    }

    /**
     * Build container which stores all service routes
     *
     * @param routes Unfiltered routes
     */
    @NotNull
    public static ServiceRouteContainer build(@NotNull Map<String, Route> routes) {

        Collection<Route> serviceRoutes = new ArrayList<>();

        for (Route route : routes.values()) {

            String controller = route.getController();
            if(controller == null || !RouteHelper.isServiceController(controller)) {
                continue;
            }

            String[] split = controller.split(":");
            if(split.length > 1) {
                serviceRoutes.add(route);
            }

        }

        return new ServiceRouteContainer(serviceRoutes);
    }

}
