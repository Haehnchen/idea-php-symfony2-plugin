package fr.adrienbrault.idea.symfony2plugin.stubs;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiModificationTracker;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.util.indexing.FileBasedIndex;
import fr.adrienbrault.idea.symfony2plugin.config.component.parser.ParameterServiceParser;
import fr.adrienbrault.idea.symfony2plugin.dic.ContainerParameter;
import fr.adrienbrault.idea.symfony2plugin.dic.ContainerService;
import fr.adrienbrault.idea.symfony2plugin.dic.XmlServiceParser;
import fr.adrienbrault.idea.symfony2plugin.dic.container.ServiceInterface;
import fr.adrienbrault.idea.symfony2plugin.dic.container.ServiceSerializable;
import fr.adrienbrault.idea.symfony2plugin.dic.container.dict.ContainerBuilderCall;
import fr.adrienbrault.idea.symfony2plugin.extension.ServiceCollectorParameter;
import fr.adrienbrault.idea.symfony2plugin.extension.ServiceParameterCollector;
import fr.adrienbrault.idea.symfony2plugin.extension.ServiceParameterCollectorParameter;
import fr.adrienbrault.idea.symfony2plugin.stubs.cache.FileIndexCaches;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.ContainerBuilderStubIndex;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.ContainerParameterStubIndex;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.PhpAttributeIndex;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.ServicesDefinitionStubIndex;
import fr.adrienbrault.idea.symfony2plugin.dic.container.util.ServiceContainerUtil;
import fr.adrienbrault.idea.symfony2plugin.util.dict.ServiceUtil;
import fr.adrienbrault.idea.symfony2plugin.util.service.ServiceXmlParserFactory;
import com.intellij.openapi.vfs.VirtualFile;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ContainerCollectionResolver {

    private static final Key<CachedValue<Map<String, List<ServiceSerializable>>>> SERVICE_CONTAINER_INDEX = new Key<>("SYMFONY_SERVICE_CONTAINER_INDEX");
    private static final Key<CachedValue<Map<String, List<String>>>> SERVICE_PARAMETER_INDEX = new Key<>("SERVICE_PARAMETER_INDEX");

    private static final Key<CachedValue<Set<String>>> SERVICE_CONTAINER_INDEX_NAMES = new Key<>("SYMFONY_SERVICE_CONTAINER_INDEX_NAMES");
    private static final Key<CachedValue<Set<String>>> SERVICE_PARAMETER_INDEX_NAMES = new Key<>("SERVICE_PARAMETER_INDEX_NAMES");

    private static final Key<CachedValue<ServiceCollector>> SYMFONY_SERVICE_COLLECTOR_CACHE = new Key<>("SYMFONY_SERVICE_COLLECTOR_CACHE");
    private static final Key<CachedValue<Map<String, ContainerService>>> RESOURCE_BASED_SERVICES_CACHE = new Key<>("SYMFONY_RESOURCE_BASED_SERVICES_CACHE");
    private static final Key<CachedValue<ParameterCollector>> SYMFONY_PARAMETER_COLLECTOR_CACHE = new Key<>("SYMFONY_PARAMETER_COLLECTOR_CACHE");
    private static final Key<CachedValue<Set<String>>> EXCLUDED_CLASSES_CACHE = new Key<>("SYMFONY_EXCLUDED_CLASSES_CACHE");

    private static final ExtensionPointName<fr.adrienbrault.idea.symfony2plugin.extension.ServiceCollector> EXTENSIONS = new ExtensionPointName<>(
        "fr.adrienbrault.idea.symfony2plugin.extension.ServiceCollector"
    );

    private static final ExtensionPointName<fr.adrienbrault.idea.symfony2plugin.extension.ServiceParameterCollector> EXTENSIONS_PARAMETER = new ExtensionPointName<>(
        "fr.adrienbrault.idea.symfony2plugin.extension.ServiceParameterCollector"
    );

    public static Collection<String> getServiceNames(@NotNull Project project) {
        return ServiceCollector.create(project).getNames();
    }

    public static boolean hasServiceNames(@NotNull Project project, @NotNull String serviceName) {
        // @TODO: we dont need a collection here; stop on first match
        return ServiceCollector.create(project).getNames().contains(serviceName);
    }

    public static boolean hasServiceName(@NotNull LazyServiceCollector lazyServiceCollector, @NotNull String serviceName) {
        // @TODO: we dont need a collection here; stop on first match
        return lazyServiceCollector.getCollector().getNames().contains(serviceName);
    }

    @Nullable
    public static ContainerService getService(@NotNull Project project, @NotNull String serviceName) {
        Map<String, ContainerService> services = getServices(project);
        return services.getOrDefault(serviceName, null);
    }

    public static Map<String, ContainerService> getServices(@NotNull Project project) {
        return ServiceCollector.create(project).getServices();
    }

    /**
     * Collect services defined via resource patterns (e.g., "App\: resource: '../src/'").
     * These are auto-loaded services that should be available for autocompletion and type providers.
     */
    @NotNull
    private static Map<String, ContainerService> getResourceBasedServices(@NotNull Project project) {
        return CachedValuesManager.getManager(project).getCachedValue(
            project,
            RESOURCE_BASED_SERVICES_CACHE,
            () -> CachedValueProvider.Result.create(
                getResourceBasedServicesInner(project),
                FileIndexCaches.getModificationTrackerForIndexId(project, ServicesDefinitionStubIndex.KEY),
                VirtualFileManager.VFS_STRUCTURE_MODIFICATIONS
            ),
            false
        );
    }

    @NotNull
    private static Map<String, ContainerService> getResourceBasedServicesInner(@NotNull Project project) {
        Map<String, ContainerService> services = new HashMap<>();

        // Collect keys first to avoid nested index access inside processAllKeys
        List<String> resourceServiceIds = new ArrayList<>();
        FileBasedIndex.getInstance().processAllKeys(ServicesDefinitionStubIndex.KEY, serviceId -> {
            if (serviceId.endsWith("\\")) {
                resourceServiceIds.add(serviceId);
            }
            return true;
        }, project);

        GlobalSearchScope scope = ServiceIndexUtil.getRestrictedFileTypesScope(project);
        for (String serviceId : resourceServiceIds) {
            List<ServiceSerializable> serviceValues = FileBasedIndex.getInstance()
                .getValues(ServicesDefinitionStubIndex.KEY, serviceId, scope);

            for (ServiceSerializable service : serviceValues) {
                if (service.getResource().isEmpty()) {
                    continue;
                }

                VirtualFile[] definitionFiles = ServiceIndexUtil.findServiceDefinitionFiles(project, serviceId);
                if (definitionFiles.length == 0) {
                    continue;
                }

                VirtualFile containerFile = definitionFiles[0];

                Collection<String> phpClasses = ServiceContainerUtil.getPhpClassFromResources(
                    project,
                    service.getId(),
                    containerFile,
                    service.getResource(),
                    service.getExclude()
                );

                for (String phpClass : phpClasses) {
                    String fqn = StringUtils.stripStart(phpClass, "\\");
                    services.put(fqn, new ContainerService(fqn, fqn, true));
                }
            }
        }

        return services;
    }

    @Nullable
    public static String resolveService(@NotNull Project project, @NotNull String serviceName) {
        return ServiceCollector.create(project).resolve(serviceName);
    }

    public static class LazyServiceCollector {

        private final Project project;
        private ServiceCollector serviceCollector;
        private ParameterCollector parameterCollector;

        public LazyServiceCollector(Project project) {
            this.project = project;
        }

        @NotNull
        public ServiceCollector getCollector() {

            if(this.serviceCollector == null) {
                this.serviceCollector = ServiceCollector.create(project);
            }

            return this.serviceCollector;
        }
        @NotNull
        public ParameterCollector getParameterCollector() {

            if(this.parameterCollector == null) {
                this.parameterCollector = ParameterCollector.create(project);
            }

            return this.parameterCollector;
        }
    }

    /**
     *
     * Resolve service class name which can be a class name or parameter, unknown parameter returns null
     *
     * @param project project
     * @param paramOrClassName any raw class name or parameter name
     * @return class name or unchanged item
     */
    @Nullable
    public static String resolveParameter(@NotNull Project project, @NotNull String paramOrClassName) {
        return resolveParameter(ParameterCollector.create(project), paramOrClassName);
    }

    @NotNull
    public static Map<String, ContainerParameter> getParameters(@NotNull Project project) {
        return ParameterCollector.create(project).getParameters();
    }

    @Nullable
    public static String resolveParameter(@NotNull ParameterCollector parameterCollector, @NotNull String paramOrClassName) {
        return parameterCollector.resolve(paramOrClassName);
    }

    public static Set<String> getParameterNames(@NotNull Project project) {
        return ParameterCollector.create(project).getNames();
    }

    public static boolean hasParameterName(@NotNull LazyServiceCollector lazyServiceCollector, @NotNull String parameterName) {
        return lazyServiceCollector.getParameterCollector().getNames().contains(parameterName);
    }


    /**
     * Get all class FQNs that have the #[Exclude] attribute (without leading backslash).
     * These classes should be excluded from service discovery.
     */
    @NotNull
    private static Set<String> getExcludedClasses(@NotNull Project project) {
        return CachedValuesManager.getManager(project).getCachedValue(
            project,
            EXCLUDED_CLASSES_CACHE,
            () -> {
                Set<String> excludedClasses = new HashSet<>();

                FileBasedIndex.getInstance().processValues(
                    PhpAttributeIndex.KEY,
                    PhpAttributeIndex.PhpAttributeIndexer.EXCLUDE_ATTRIBUTE,
                    null,
                    (file, value) -> {
                        if (!value.isEmpty()) {
                            excludedClasses.add(value.getFirst());
                        }
                        return true;
                    },
                    GlobalSearchScope.allScope(project)
                );

                return CachedValueProvider.Result.create(
                    excludedClasses,
                    FileIndexCaches.getModificationTrackerForIndexId(project, PhpAttributeIndex.KEY)
                );
            },
            false
        );
    }

    public static class ServiceCollector {

        @NotNull
        final private Project project;

        @Nullable
        private ParameterCollector parameterCollector;

        @Nullable
        private Map<String, ContainerService> servicesCache;

        @Nullable
        private Set<String> serviceNamesCache;
        private Map<String, Set<String>> serviceClassNameCache;

        private ServiceCollector(@NotNull Project project) {
            this.project = project;
        }

        public Collection<ContainerService> collect() {
            return this.getServices().values();
        }

        @Nullable
        public String resolve(String serviceName) {

            if(this.getServices().containsKey(serviceName)) {

                // service can be a parameter, resolve if necessary
                ContainerService service = this.getServices().get(serviceName);
                String className = service.getClassName();
                if(className != null && className.startsWith("%") && className.endsWith("%")) {
                    return getParameterCollector().resolve(className);
                } else {
                    return className;
                }
            }

            return null;
        }

        @NotNull
        public Map<String, ContainerService> getServices() {
            if(this.servicesCache != null) {
                return this.servicesCache;
            }

            TreeMap<String, ContainerService> services = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

            // file system
            for(ServiceInterface entry: ServiceXmlParserFactory.getInstance(project, XmlServiceParser.class).getServiceMap().getServices()) {
                // compiled container owns all class names in resolved state
                // api safe check
                if(entry.getClassName() != null) {
                    services.put(entry.getId(), new ContainerService(entry.getId(), entry.getClassName()));
                }
            }

            // Add resource-based services (auto-loaded from namespace patterns)
            services.putAll(getResourceBasedServices(project));

            Collection<ServiceInterface> aliases = new ArrayList<>();
            Collection<ServiceInterface> decorated = new ArrayList<>();

            // Extension points
            ServiceCollectorParameter.Service parameter = null;
            Collection<ServiceInterface> exps = new ArrayList<>();
            for (fr.adrienbrault.idea.symfony2plugin.extension.ServiceCollector collectorEx : EXTENSIONS.getExtensions()) {
                if(parameter == null) {
                    parameter = new ServiceCollectorParameter.Service(project, exps);
                }

                collectorEx.collectServices(parameter);
            }

            if(!exps.isEmpty()) {
                exps.forEach(service -> services.put(service.getId(), new ContainerService(service, null)));
            }

            for (Map.Entry<String, List<ServiceSerializable>> entry : FileIndexCaches.getSetDataCache(project, SERVICE_CONTAINER_INDEX, SERVICE_CONTAINER_INDEX_NAMES, ServicesDefinitionStubIndex.KEY, ServiceIndexUtil.getRestrictedFileTypesScope(project)).entrySet()) {

                // dont work twice on service;
                // @TODO: to need to optimize this to decorate as much service data as possible
                String serviceName = entry.getKey();

                // fake empty service, case which is not allowed by catch it
                List<ServiceSerializable> servicesValues = entry.getValue();
                if(services.isEmpty()) {
                    services.put(serviceName, new ContainerService(serviceName, null, true));
                    continue;
                }

                for(ServiceInterface service: servicesValues) {
                    String classValue = service.getClassName();

                    // duplicate services
                    ContainerService containerService = services.get(serviceName);
                    if (containerService != null) {
                        if(classValue == null) {
                            continue;
                        }

                        String classValueResolve = classValue;
                        String compiledClassName = containerService.getClassName();
                        if(!classValue.equalsIgnoreCase(compiledClassName)) {
                            String resolvedClassValue = getParameterCollector().resolve(classValue);
                            if(resolvedClassValue != null && !StringUtils.isBlank(classValue) && !resolvedClassValue.equalsIgnoreCase(compiledClassName)) {
                                containerService.addClassName(resolvedClassValue);
                                classValueResolve = resolvedClassValue;
                            }
                        }

                        // compiled container done have a value
                        if (containerService.getService() == null) {
                            services.put(serviceName, new ContainerService(service, classValueResolve));
                        }

                        continue;
                    }

                    if(service.getAlias() != null) {
                        aliases.add(service);
                    }

                    // reuse iteration for alias mapping
                    if(service.getDecorates() != null) {
                        decorated.add(service);
                    }

                    // resolve class value, it can be null or a parameter
                    if(!StringUtils.isBlank(classValue)) {
                        classValue = getParameterCollector().resolve(classValue);
                    }

                    // @TODO: legacy bridge; replace this with ServiceInterface
                    services.put(serviceName, new ContainerService(service, classValue));
                }
            }

            // replace alias with main service
            if(!aliases.isEmpty()) {
                services.putAll(collectAliases(aliases, services));
            }

            if(!decorated.isEmpty()) {
                services.putAll(collectDecorated(decorated, services));
            }

            // Filter out services whose class has the #[Exclude] attribute
            Set<String> excludedClasses = getExcludedClasses(project);
            if (!excludedClasses.isEmpty()) {
                services.entrySet().removeIf(entry -> {
                    for (String className : entry.getValue().getClassNames()) {
                        if (excludedClasses.contains(StringUtils.stripStart(className, "\\"))) {
                            return true;
                        }
                    }
                    return false;
                });
            }

            return this.servicesCache = services;
        }

        @NotNull
        private Map<String, ContainerService> collectAliases(@NotNull Collection<ServiceInterface> aliases, @NotNull Map<String, ContainerService> currentServices) {
            Map<String, ContainerService> items = new HashMap<>();

            for (ServiceInterface service : aliases) {
                // double check alias name
                String alias = service.getAlias();
                if(alias == null || StringUtils.isBlank(alias) || !currentServices.containsKey(alias)) {
                    continue;
                }

                items.put(service.getId(), currentServices.get(alias));
            }

            return items;
        }

        @NotNull
        private Map<String, ContainerService> collectDecorated(@NotNull Collection<ServiceInterface> decorated, @NotNull Map<String, ContainerService> currentServices) {
            Map<String, ContainerService> items = new HashMap<>();

            for (ServiceInterface service : decorated) {
                String decorationInnerName = service.getDecorationInnerName();
                if(StringUtils.isBlank(decorationInnerName)) {
                    decorationInnerName = service.getId() + ".inner";
                }

                ContainerService origin = currentServices.get(service.getDecorates());
                if(origin == null) {
                    continue;
                }

                // @TODO: migrate constructor to ServiceInterface and decorate
                ContainerService value = new ContainerService(decorationInnerName, origin.getClassName(), origin.isWeak(), true);
                origin.getClassNames().forEach(value::addClassName);

                items.put(decorationInnerName, value);
            }

            return items;
        }

        public Set<String> convertClassNameToServices(@NotNull String fqnClassName) {
            if (this.serviceClassNameCache == null) {
                Map<String, Set<String>> result = new HashMap<>();
                for (Map.Entry<String, ContainerService> entry: this.getServices().entrySet()) {
                    for (String className : entry.getValue().getClassNames()) {
                        String indexedClassName = this.getParameterCollector().resolve(className);
                        if (indexedClassName != null) {
                            indexedClassName = "\\" + StringUtils.stripStart(indexedClassName, "\\");

                            result.putIfAbsent(indexedClassName, new HashSet<>());
                            result.get(indexedClassName).add(entry.getKey());
                        }
                    }
                }

                this.serviceClassNameCache = result;
            }

            fqnClassName = "\\" + StringUtils.stripStart(fqnClassName, "\\");
            return this.serviceClassNameCache.getOrDefault(fqnClassName, Collections.emptySet());
        }

        private Set<String> getNames() {
            if (this.serviceNamesCache != null) {
                return this.serviceNamesCache;
            }

            Set<String> serviceNames = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
            serviceNames.addAll(getResourceBasedServices(project).keySet());

            // local filesystem
            serviceNames.addAll(
                ServiceXmlParserFactory.getInstance(project, XmlServiceParser.class).getServiceMap().getIds()
            );

            // Extension points
            ServiceCollectorParameter.Id parameter = null;
            for (fr.adrienbrault.idea.symfony2plugin.extension.ServiceCollector collectorEx : EXTENSIONS.getExtensions()) {
                if(parameter == null) {
                    parameter = new ServiceCollectorParameter.Id(project, serviceNames);
                }

                collectorEx.collectIds(parameter);
            }

            // index
            serviceNames.addAll(
                FileIndexCaches.getIndexKeysCache(project, SERVICE_CONTAINER_INDEX_NAMES, ServicesDefinitionStubIndex.KEY)
            );

            // Filter out service names whose class has the #[Exclude] attribute
            Set<String> excludedClasses = getExcludedClasses(project);
            if (!excludedClasses.isEmpty()) {
                serviceNames.removeAll(excludedClasses);
            }

            return this.serviceNamesCache = serviceNames;
        }


        private ParameterCollector getParameterCollector() {
            return (this.parameterCollector != null) ? this.parameterCollector : (this.parameterCollector = ParameterCollector.create(this.project));
        }

        public static ServiceCollector create(@NotNull Project project) {
            return CachedValuesManager.getManager(project).getCachedValue(
                project,
                SYMFONY_SERVICE_COLLECTOR_CACHE,
                () -> CachedValueProvider.Result.create(new ServiceCollector(project), PsiModificationTracker.MODIFICATION_COUNT),
                false
            );
        }
    }

    public static class ParameterCollector {
        @NotNull
        private final Project project;

        @Nullable
        private Map<String, ContainerParameter> containerParameterMap;

        private ParameterCollector(@NotNull Project project) {
            this.project = project;
        }

        public static ParameterCollector create(@NotNull Project project) {
            return CachedValuesManager.getManager(project).getCachedValue(
                project,
                SYMFONY_PARAMETER_COLLECTOR_CACHE,
                () -> CachedValueProvider.Result.create(new ParameterCollector(project), PsiModificationTracker.MODIFICATION_COUNT),
                false
            );
        }

        /**
         *
         * Resolve service class name which can be a class name or parameter, unknown parameter returns null
         *
         */
        @Nullable
        private String resolve(@Nullable String paramOrClassName) {

            if(paramOrClassName == null) {
                return null;
            }

            // strip "%" to get the parameter name
            if(paramOrClassName.length() > 1 && paramOrClassName.startsWith("%") && paramOrClassName.endsWith("%")) {

                paramOrClassName = paramOrClassName.substring(1, paramOrClassName.length() - 1);

                // parameter is always lower see #179
                paramOrClassName = paramOrClassName.toLowerCase();

                if(this.getParameters().containsKey(paramOrClassName)) {
                    return getParameters().get(paramOrClassName).getValue();
                }

                return null;
            }

            return paramOrClassName;
        }

        @NotNull
        private Map<String, ContainerParameter> getParameters() {
            if(this.containerParameterMap != null) {
                return this.containerParameterMap;
            }

            TreeMap<String, ContainerParameter> parametersMap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);


            // local filesystem
            for(Map.Entry<String, String> Entry: ServiceXmlParserFactory.getInstance(project, ParameterServiceParser.class).getParameterMap().entrySet()) {

                // user input here; secure nullable values
                String key = Entry.getKey();
                if(key != null) {
                    parametersMap.put(key, new ContainerParameter(key, Entry.getValue(), false));
                }
            }

            // index
            for (Map.Entry<String, List<String>> entry : FileIndexCaches.getStringDataCache(project, SERVICE_PARAMETER_INDEX, SERVICE_PARAMETER_INDEX_NAMES, ContainerParameterStubIndex.KEY, ServiceIndexUtil.getRestrictedFileTypesScope(project)).entrySet()) {
                String parameterName = entry.getKey();
                // just for secure
                if(parameterName == null) {
                    continue;
                }

                // indexes is weak stuff, dont overwrite compiled ones
                if(!parametersMap.containsKey(parameterName)) {
                    parametersMap.put(parameterName, new ContainerParameter(parameterName, entry.getValue(), true));
                }
            }

            // setParameter("foo") for ContainerBuilder
            for (ContainerBuilderCall call : FileBasedIndex.getInstance().getValues(ContainerBuilderStubIndex.KEY, "setParameter", GlobalSearchScope.allScope(project))) {
                Collection<String> parameters = call.getParameter();
                if(parameters == null || parameters.isEmpty()) {
                    continue;
                }

                for (String parameter : parameters) {
                    if(parametersMap.containsKey(parameter)) {
                        continue;
                    }

                    parametersMap.put(parameter, new ContainerParameter(parameter, true));
                }
            }

            // Kernel::getKernelParameters
            for (String parameterName : ServiceUtil.getParameterParameters(project)) {
                if(parametersMap.containsKey(parameterName)) {
                    continue;
                }

                parametersMap.put(parameterName, new ContainerParameter(parameterName, true));
            }

            // Extension points
            ServiceParameterCollectorParameter.Id parameter = null;
            Collection<ContainerParameter> exps = new ArrayList<>();
            for (ServiceParameterCollector parameterCollector : EXTENSIONS_PARAMETER.getExtensions()) {
                if(parameter == null) {
                    parameter = new ServiceParameterCollectorParameter.Id(project, exps);
                }

                parameterCollector.collectIds(parameter);
            }

            for (ContainerParameter extParameter: exps) {
                parametersMap.put(extParameter.getName(), extParameter);
            }

            return this.containerParameterMap = parametersMap;
        }

        @NotNull
        private Set<String> getNames() {
            return getParameters().keySet();
        }
    }
}
