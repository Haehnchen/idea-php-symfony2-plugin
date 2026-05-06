package fr.adrienbrault.idea.symfony2plugin.routing.dic;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.jetbrains.php.lang.psi.stubs.indexes.PhpClassFqnIndex;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.routing.Route;
import fr.adrienbrault.idea.symfony2plugin.routing.RouteHelper;
import fr.adrienbrault.idea.symfony2plugin.stubs.ContainerCollectionResolver;
import fr.adrienbrault.idea.symfony2plugin.stubs.cache.FileIndexCaches;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.PhpAttributeIndex;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.RoutesStubIndex;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.ServicesDefinitionStubIndex;
import fr.adrienbrault.idea.symfony2plugin.util.SymfonyVarDirectoryWatcher;
import fr.adrienbrault.idea.symfony2plugin.util.SymfonyVarDirectoryWatcherKt;
import fr.adrienbrault.idea.symfony2plugin.util.dict.ServiceUtil;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ServiceRouteContainer  {

    private static final Key<CachedValue<ServiceRouteData>> SERVICE_ROUTE_DATA_CACHE = new Key<>("SYMFONY_SERVICE_ROUTE_DATA_CACHE");

    // Holds the pre-split controller parts alongside the Route to avoid re-splitting on every lookup.
    private record RouteEntry(String serviceId, String methodName, Route route) {}

    private record ServiceRouteData(@NotNull Map<String, List<RouteEntry>> routesByMethodName, @NotNull Set<String> serviceNames) {}

    /**
     * Returns service ids referenced by service-style route controllers.
     */
    @NotNull
    public static Set<String> getServiceNames(@NotNull Project project) {
        return getServiceRouteData(project).serviceNames();
    }

    @NotNull
    public static Collection<Route> getMethodMatchesForRouteController(@NotNull Method method) {
        PhpClass originClass = method.getContainingClass();
        if(originClass == null) {
            return Collections.emptyList();
        }

        // Fast path: no routes registered for this method name at all
        List<RouteEntry> candidates = getServiceRouteData(method.getProject()).routesByMethodName().get(method.getName());
        if (candidates == null) {
            return Collections.emptyList();
        }

        String classFqn = StringUtils.stripStart(originClass.getFQN(), "\\");

        Collection<Route> routes = new ArrayList<>();
        Map<String, PhpClass> serviceCache = new HashMap<>();
        ContainerCollectionResolver.LazyServiceCollector lazyServiceCollector = new ContainerCollectionResolver.LazyServiceCollector(method.getProject());
        for (RouteEntry entry : candidates) {
            // cache PhpClass resolve
            if(!serviceCache.containsKey(entry.serviceId())) {
                serviceCache.put(entry.serviceId(), ServiceUtil.getResolvedClassDefinition(method.getProject(), entry.serviceId(), lazyServiceCollector));
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

    @NotNull
    private static ServiceRouteData getServiceRouteData(@NotNull Project project) {
        return CachedValuesManager.getManager(project).getCachedValue(
            project,
            SERVICE_ROUTE_DATA_CACHE,
            () -> CachedValueProvider.Result.create(
                buildUncached(project, RouteHelper.getAllRoutes(project)),
                getCacheDependencies(project)
            ),
            false
        );
    }

    private static Object @NotNull [] getCacheDependencies(@NotNull Project project) {
        return new Object[] {
            FileIndexCaches.getModificationTrackerForIndexId(project, RoutesStubIndex.KEY),
            SymfonyVarDirectoryWatcherKt.getSymfonyVarDirectoryWatcher(project).getModificationTracker(SymfonyVarDirectoryWatcher.Scope.ROUTES),
            FileIndexCaches.getModificationTrackerForIndexId(project, ServicesDefinitionStubIndex.KEY),
            FileIndexCaches.getModificationTrackerForIndexId(project, PhpAttributeIndex.KEY),
            FileIndexCaches.getModificationTrackerForIndexId(project, PhpClassFqnIndex.KEY),
            SymfonyVarDirectoryWatcherKt.getSymfonyVarDirectoryWatcher(project).getModificationTracker(SymfonyVarDirectoryWatcher.Scope.CONTAINER),
            VirtualFileManager.VFS_STRUCTURE_MODIFICATIONS
        };
    }

    /**
     * Build route lookup data for service-style route controllers.
     *
     * @param routes Unfiltered routes
     */
    private static ServiceRouteData buildUncached(@NotNull Project project, @NotNull Map<String, Route> routes) {
        Map<String, List<RouteEntry>> routesByMethodName = new HashMap<>();
        Set<String> serviceNames = new HashSet<>();

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
                String serviceId = split[0];
                String methodName = split[1];
                routesByMethodName.computeIfAbsent(methodName, key -> new ArrayList<>()).add(new RouteEntry(serviceId, methodName, route));
                serviceNames.add(serviceId);
            }
        }

        Map<String, List<RouteEntry>> unmodifiableRoutesByMethodName = new HashMap<>();
        for (Map.Entry<String, List<RouteEntry>> entry : routesByMethodName.entrySet()) {
            unmodifiableRoutesByMethodName.put(entry.getKey(), Collections.unmodifiableList(entry.getValue()));
        }

        return new ServiceRouteData(
            Collections.unmodifiableMap(unmodifiableRoutesByMethodName),
            Collections.unmodifiableSet(serviceNames)
        );
    }
}
