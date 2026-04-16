package fr.adrienbrault.idea.symfony2plugin.routing.dic;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiModificationTracker;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.routing.Route;
import fr.adrienbrault.idea.symfony2plugin.routing.RouteHelper;
import fr.adrienbrault.idea.symfony2plugin.stubs.ContainerCollectionResolver;
import fr.adrienbrault.idea.symfony2plugin.util.dict.ServiceUtil;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ServiceRouteContainer  {

    private static final Key<CachedValue<ServiceRouteContainer>> SERVICE_ROUTE_CONTAINER_CACHE = new Key<>("SYMFONY_SERVICE_ROUTE_CONTAINER_CACHE");

    private final Collection<Route> routes;
    // Lazily built index: method name -> matching route entries. Null until first use.
    private Map<String, List<RouteEntry>> routesByMethodName;
    private ContainerCollectionResolver.LazyServiceCollector lazyServiceCollector;

    private final Map<String, PhpClass> serviceCache = new HashMap<>();

    // Holds the pre-split controller parts alongside the Route to avoid re-splitting on every lookup.
    private record RouteEntry(String serviceId, String methodName, Route route) {}

    private ServiceRouteContainer(@NotNull Collection<Route> routes) {
        this.routes = routes;
    }

    /**
     * Builds the method-name index on the first call and caches it for subsequent calls.
     * Splitting is done here once with indexOf instead of split() to avoid repeated array allocations.
     */
    private Map<String, List<RouteEntry>> getRoutesByMethodName() {
        if (routesByMethodName != null) {
            return routesByMethodName;
        }

        Map<String, List<RouteEntry>> index = new HashMap<>();
        for (Route route : routes) {
            String controller = route.getController();
            if (controller == null) continue;
            // controller format: "service_id:methodName" or "service_id::methodName"
            String normalizedController = controller.replace("::", ":");
            int colon = normalizedController.indexOf(':');
            if (colon > 0 && colon < normalizedController.length() - 1) {
                String serviceId = normalizedController.substring(0, colon);
                String methodName = normalizedController.substring(colon + 1);
                index.computeIfAbsent(methodName, k -> new ArrayList<>()).add(new RouteEntry(serviceId, methodName, route));
            }
        }

        return routesByMethodName  = index;
    }

    public Set<String> getServiceNames() {
        Set<String> services = new HashSet<>();
        for (List<RouteEntry> entries : getRoutesByMethodName().values()) {
            for (RouteEntry entry : entries) {
                services.add(entry.serviceId());
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

        // Fast path: no routes registered for this method name at all
        List<RouteEntry> candidates = getRoutesByMethodName().get(method.getName());
        if (candidates == null) {
            return Collections.emptyList();
        }

        String classFqn = StringUtils.stripStart(originClass.getFQN(), "\\");

        Collection<Route> routes = new ArrayList<>();
        for (RouteEntry entry : candidates) {
            // cache PhpClass resolve
            if(!serviceCache.containsKey(entry.serviceId())) {
                serviceCache.put(entry.serviceId(), ServiceUtil.getResolvedClassDefinition(method.getProject(), entry.serviceId(), getLazyServiceCollector(method.getProject())));
            }

            PhpClass phpClass = serviceCache.get(entry.serviceId());
            if(phpClass != null && classFqn.equals(phpClass.getPresentableFQN())) {
                if(phpClass.findMethodByName(entry.methodName()) != null) {
                    routes.add(entry.route());
                }
            }
        }

        return routes;
    }

    private ContainerCollectionResolver.LazyServiceCollector getLazyServiceCollector(Project project) {
        return this.lazyServiceCollector == null ? this.lazyServiceCollector = new ContainerCollectionResolver.LazyServiceCollector(project) : this.lazyServiceCollector;
    }

    @NotNull
    public static ServiceRouteContainer build(@NotNull Project project) {
        return CachedValuesManager.getManager(project).getCachedValue(
            project,
            SERVICE_ROUTE_CONTAINER_CACHE,
            () -> {
                ServiceRouteContainer container = buildUncached(project, RouteHelper.getAllRoutes(project));
                return CachedValueProvider.Result.create(
                    container,
                    PsiModificationTracker.MODIFICATION_COUNT
                );
            },
            false
        );
    }

    /**
     * Build container which stores all service routes
     *
     * @param routes Unfiltered routes
     */
    private static ServiceRouteContainer buildUncached(@NotNull Project project, @NotNull Map<String, Route> routes) {
        Collection<Route> serviceRoutes = new ArrayList<>();

        ContainerCollectionResolver.LazyServiceCollector lazyServiceCollector = null;

        for (Route route : routes.values()) {
            String controller = route.getController();
            if(controller == null || !RouteHelper.isServiceController(controller)) {
                continue;
            }

            String[] split = controller.replace("::", ":").split(":");

            if (lazyServiceCollector == null) {
                lazyServiceCollector = new ContainerCollectionResolver.LazyServiceCollector(project);
            }

            if(split.length > 1 && ContainerCollectionResolver.hasServiceName(lazyServiceCollector, split[0])) {
                serviceRoutes.add(route);
            }
        }

        return new ServiceRouteContainer(serviceRoutes);
    }

}
