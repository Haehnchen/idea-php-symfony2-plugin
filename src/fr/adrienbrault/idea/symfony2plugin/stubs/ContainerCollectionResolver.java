package fr.adrienbrault.idea.symfony2plugin.stubs;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.CachedValue;
import fr.adrienbrault.idea.symfony2plugin.config.component.parser.ParameterServiceParser;
import fr.adrienbrault.idea.symfony2plugin.dic.ContainerParameter;
import fr.adrienbrault.idea.symfony2plugin.dic.ContainerService;
import fr.adrienbrault.idea.symfony2plugin.dic.XmlServiceParser;
import fr.adrienbrault.idea.symfony2plugin.dic.container.ServiceInterface;
import fr.adrienbrault.idea.symfony2plugin.dic.webDeployment.ServiceContainerRemoteFileStorage;
import fr.adrienbrault.idea.symfony2plugin.routing.webDeployment.RoutingRemoteFileStorage;
import fr.adrienbrault.idea.symfony2plugin.stubs.cache.FileIndexCaches;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.ContainerParameterStubIndex;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.ServicesDefinitionStubIndex;
import fr.adrienbrault.idea.symfony2plugin.util.service.ServiceXmlParserFactory;
import fr.adrienbrault.idea.symfony2plugin.webDeployment.utils.RemoteWebServerUtil;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class ContainerCollectionResolver {

    private static final Key<CachedValue<Map<String, List<ServiceInterface>>>> SERVICE_CONTAINER_INDEX = new Key<CachedValue<Map<String, List<ServiceInterface>>>>("SYMFONY_SERVICE_CONTAINER_INDEX");
    private static final Key<CachedValue<Map<String, List<String>>>> SERVICE_PARAMETER_INDEX = new Key<CachedValue<Map<String, List<String>>>>("SERVICE_PARAMETER_INDEX");

    private static final Key<CachedValue<Set<String>>> SERVICE_CONTAINER_INDEX_NAMES = new Key<CachedValue<Set<String>>>("SYMFONY_SERVICE_CONTAINER_INDEX_NAMES");
    private static final Key<CachedValue<Set<String>>> SERVICE_PARAMETER_INDEX_NAMES = new Key<CachedValue<Set<String>>>("SERVICE_PARAMETER_INDEX_NAMES");

    public static enum Source {
        INDEX, COMPILER
    }

    public static Collection<String> getServiceNames(Project project) {
        return new ServiceCollector(project, ContainerCollectionResolver.Source.COMPILER, ContainerCollectionResolver.Source.INDEX).getNames();
    }

    public static boolean hasServiceNames(Project project, String serviceName) {
        // @TODO: we dont need a collection here; stop on first match
        return new ServiceCollector(project, ContainerCollectionResolver.Source.COMPILER, ContainerCollectionResolver.Source.INDEX).getNames().contains(serviceName);
    }

    @Nullable
    public static ContainerService getService(Project project, String serviceName) {
        Map<String, ContainerService> services = getServices(project, Source.COMPILER, Source.INDEX);
        return services.containsKey(serviceName) ? services.get(serviceName) : null;
    }

    public static Map<String, ContainerService> getServices(Project project) {
        return getServices(project, ContainerCollectionResolver.Source.COMPILER, ContainerCollectionResolver.Source.INDEX);
    }

    public static Map<String, ContainerService> getServices(Project project, Source... collectorSources) {
        return new ServiceCollector(project, collectorSources).getServices();
    }

    @Nullable
    public static String resolveService(Project project, String serviceName) {
        return resolveService(project, serviceName, ContainerCollectionResolver.Source.COMPILER, ContainerCollectionResolver.Source.INDEX);
    }

    @Nullable
    public static String resolveService(Project project, String serviceName, ContainerCollectionResolver.Source... collectorSources) {
        return new ServiceCollector(project, collectorSources).resolve(serviceName);
    }


    private static GlobalSearchScope getSearchScope(Project project) {
        return GlobalSearchScope.allScope(project);
    }

    public static class LazyServiceCollector {

        private final Project project;
        private ServiceCollector serviceCollector;
        private Source[] sources  = {Source.COMPILER, Source.INDEX};
        private ParameterCollector parameterCollector;

        public LazyServiceCollector(Project project) {
            this.project = project;
        }

        @NotNull
        public ServiceCollector getCollector() {

            if(this.serviceCollector == null) {
                this.serviceCollector = new ServiceCollector(project, sources);
            }

            return this.serviceCollector;
        }
        @NotNull
        public ParameterCollector getParameterCollector() {

            if(this.parameterCollector == null) {
                this.parameterCollector = new ParameterCollector(project, sources);
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
    public static String resolveParameter(Project project, String paramOrClassName) {
        return resolveParameter(new ParameterCollector(project, Source.COMPILER, Source.INDEX), paramOrClassName);
    }

    @Nullable
    public static String resolveParameter(@NotNull ParameterCollector parameterCollector, @NotNull String paramOrClassName) {
        return parameterCollector.resolve(paramOrClassName);
    }

    public static Map<String, ContainerParameter> getParameters(Project project) {
        return getParameters(project, ContainerCollectionResolver.Source.COMPILER, ContainerCollectionResolver.Source.INDEX);
    }

    public static Map<String, ContainerParameter> getParameters(Project project, ContainerCollectionResolver.Source... collectorSources) {
        return new ParameterCollector(project, collectorSources).getParameters();
    }

    public static Set<String> getParameterNames(Project project) {
        return getParameterNames(project, ContainerCollectionResolver.Source.COMPILER, ContainerCollectionResolver.Source.INDEX);
    }

    public static Set<String> getParameterNames(Project project, ContainerCollectionResolver.Source... collectorSources) {
        return new ParameterCollector(project, collectorSources).getNames();
    }

    public static class ServiceCollector {

        private Set<Source> sources = new HashSet<Source>();
        private Project project;
        private ParameterCollector parameterCollector;
        private Map<String, ContainerService> services;

        public ServiceCollector(Project project, Source... sources) {
            this(project);
            this.sources = new HashSet<Source>(Arrays.asList(sources));
        }

        public ServiceCollector(Project project) {
            this.project = project;
        }

        public void addCollectorSource(Source source) {
            this.sources.add(source);
        }

        public Collection<ContainerService> collect() {
            return this.getServices().values();
        }

        @Nullable
        public String resolve(String serviceName) {

            if(this.getServices().containsKey(serviceName)) {

                // service can be a parameter, resolve if necessary
                String className = this.getServices().get(serviceName).getClassName();
                if(className != null && className.startsWith("%") && className.endsWith("%")) {
                    return getParameterCollector().resolve(className);
                } else {
                    return className;
                }
            }

            return null;
        }

        public Map<String, ContainerService> getServices() {

            if(this.services != null) {
                return this.services;
            }

            this.services = new TreeMap<String, ContainerService>(String.CASE_INSENSITIVE_ORDER);

            if(this.sources.contains(Source.COMPILER)) {

                // @TODO: extension point;
                // add remote first; local filesystem wins on duplicate key
                ServiceContainerRemoteFileStorage extensionInstance = RemoteWebServerUtil.getExtensionInstance(project, ServiceContainerRemoteFileStorage.class);
                if(extensionInstance != null) {
                    for (Map.Entry<String, String> entry : extensionInstance.getState().getServiceMap().entrySet()) {
                        services.put(entry.getKey(), new ContainerService(entry.getKey(), entry.getValue()));
                    }
                }

                for(Map.Entry<String, String> entry: ServiceXmlParserFactory.getInstance(project, XmlServiceParser.class).getServiceMap().getMap().entrySet()) {
                    services.put(entry.getKey(), new ContainerService(entry.getKey(), entry.getValue()));
                }
            }

            if(this.sources.contains(Source.INDEX)) {

                Collection<ServiceInterface> aliases = new ArrayList<ServiceInterface>();

                for (Map.Entry<String, List<ServiceInterface>> entry : FileIndexCaches.getSetDataCache(project, SERVICE_CONTAINER_INDEX, SERVICE_CONTAINER_INDEX_NAMES, ServicesDefinitionStubIndex.KEY, ServiceIndexUtil.getRestrictedFileTypesScope(project)).entrySet()) {

                    // dont work twice on service;
                    // @TODO: to need to optimize this to decorate as much service data as possible
                    String serviceName = entry.getKey();
                    if(this.services.containsKey(serviceName)) {
                        continue;
                    }

                    // fake empty service, case which is not allowed by catch it
                    List<ServiceInterface> services = entry.getValue();
                    if(services.size() == 0) {
                        this.services.put(serviceName, new ContainerService(serviceName, null, true));
                        continue;
                    }

                    for(ServiceInterface service: services) {

                        // reuse iteration for alias mapping
                        if(service.getAlias() != null) {
                            aliases.add(service);
                        }

                        // resolve class value, it can be null or a parameter
                        String classValue = service.getClassName();
                        if(!StringUtils.isBlank(classValue)) {
                            classValue = getParameterCollector().resolve(classValue);
                        }

                        // @TODO: legacy bridge; replace this with ServiceInterface
                        this.services.put(serviceName, new ContainerService(service, classValue));
                    }
                }

                // replace alias with main service
                if(aliases.size() > 0) {
                    for (ServiceInterface service : aliases) {

                        // double check alias name
                        String alias = service.getAlias();
                        if(alias == null || StringUtils.isBlank(alias) || !this.services.containsKey(alias)) {
                            continue;
                        }

                        this.services.put(service.getId(), this.services.get(alias));
                    }
                }
            }

            return this.services;
        }

        public Set<String> convertClassNameToServices(@NotNull String fqnClassName) {

            Set<String> serviceNames = new HashSet<String>();

            // normalize class name; prepend "\"
            if(!fqnClassName.startsWith("\\")) {
                fqnClassName = "\\" + fqnClassName;
            }

            for(Map.Entry<String, ContainerService> entry: this.getServices().entrySet()) {
                if(entry.getValue().getClassName() != null) {
                    String indexedClassName = this.getParameterCollector().resolve(entry.getValue().getClassName());
                    if(indexedClassName != null) {

                        // also normalize user input string inside container
                        if(!indexedClassName.startsWith("\\")) {
                            indexedClassName = "\\" + indexedClassName;
                        }

                        if(indexedClassName.equals(fqnClassName)) {
                            serviceNames.add(entry.getKey());
                        }
                    }

                }

            }

            return serviceNames;
        }

        private Set<String> getNames() {

            Set<String> serviceNames = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);

            if(this.sources.contains(Source.COMPILER)) {

                // @TODO: extension point;
                // add remote first; local filesystem wins on duplicate key
                ServiceContainerRemoteFileStorage extensionInstance = RemoteWebServerUtil.getExtensionInstance(project, ServiceContainerRemoteFileStorage.class);
                if(extensionInstance != null) {
                    serviceNames.addAll(extensionInstance.getState().getServiceMap().keySet());
                }

                // local filesystem
                serviceNames.addAll(ServiceXmlParserFactory.getInstance(project, XmlServiceParser.class).getServiceMap().getMap().keySet());
            }

            if(this.sources.contains(Source.INDEX)) {

                serviceNames.addAll(
                    FileIndexCaches.getIndexKeysCache(project, SERVICE_CONTAINER_INDEX_NAMES, ServicesDefinitionStubIndex.KEY)
                );
            }

            return serviceNames;

        }


        private ParameterCollector getParameterCollector() {
            return (this.parameterCollector != null) ? this.parameterCollector : (this.parameterCollector = new ParameterCollector(this.project, this.sources.toArray(new Source[this.sources.size()])));
        }

        public static ServiceCollector create(@NotNull Project project) {
            return new ContainerCollectionResolver.ServiceCollector(
                project,
                ContainerCollectionResolver.Source.COMPILER,
                ContainerCollectionResolver.Source.INDEX
            );
        }

    }

    public static class ParameterCollector {

        private Set<Source> sources = new HashSet<Source>();
        private Project project;
        private Map<String, ContainerParameter> containerParameterMap;

        public ParameterCollector(Project project) {
            this.project = project;
        }

        public ParameterCollector(Project project, Source... sources) {
            this(project);
            this.sources = new HashSet<Source>(Arrays.asList(sources));
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


        private Map<String, ContainerParameter> getParameters() {

            if(this.containerParameterMap != null) {
                return this.containerParameterMap;
            }

            this.containerParameterMap = new TreeMap<String, ContainerParameter>(String.CASE_INSENSITIVE_ORDER);

            if(this.sources.contains(Source.COMPILER)) {

                // remote files
                ServiceContainerRemoteFileStorage extensionInstance = RemoteWebServerUtil.getExtensionInstance(project, ServiceContainerRemoteFileStorage.class);
                if(extensionInstance != null) {
                    for (Map.Entry<String, String> entry : extensionInstance.getState().getParameterMap().entrySet()) {
                        String key = entry.getKey();
                        if(key == null) {
                            continue;
                        }

                        this.containerParameterMap.put(key, new ContainerParameter(entry.getKey(), entry.getValue()));
                    }
                }

                // local filesystem
                for(Map.Entry<String, String> Entry: ServiceXmlParserFactory.getInstance(project, ParameterServiceParser.class).getParameterMap().entrySet()) {

                    // user input here; secure nullable values
                    String key = Entry.getKey();
                    if(key != null) {
                        this.containerParameterMap.put(key, new ContainerParameter(key, Entry.getValue()));
                    }

                }
            }

            if(this.sources.contains(Source.INDEX)) {
                for (Map.Entry<String, List<String>> entry : FileIndexCaches.getStringDataCache(project, SERVICE_PARAMETER_INDEX, SERVICE_PARAMETER_INDEX_NAMES, ContainerParameterStubIndex.KEY, ServiceIndexUtil.getRestrictedFileTypesScope(project)).entrySet()) {
                    String parameterName = entry.getKey();
                    // just for secure
                    if(parameterName == null) {
                        continue;
                    }

                    // indexes is weak stuff, dont overwrite compiled ones
                    if(!this.containerParameterMap.containsKey(parameterName)) {
                        this.containerParameterMap.put(parameterName, new ContainerParameter(parameterName, entry.getValue(), true));
                    }
                }

            }

            return this.containerParameterMap;

        }

        private Set<String> getNames() {

            // use overall map if already generated
            if(this.containerParameterMap != null) {
                return this.containerParameterMap.keySet();
            }

            Set<String> parameterNames = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);

            if(this.sources.contains(Source.COMPILER)) {

                // remote files
                ServiceContainerRemoteFileStorage extensionInstance = RemoteWebServerUtil.getExtensionInstance(project, ServiceContainerRemoteFileStorage.class);
                if(extensionInstance != null) {
                    parameterNames.addAll(extensionInstance.getState().getParameterMap().keySet());
                }

                // local filesystem
                parameterNames.addAll(ServiceXmlParserFactory.getInstance(project, ParameterServiceParser.class).getParameterMap().keySet());
            }

            if(this.sources.contains(Source.INDEX)) {
                parameterNames.addAll(
                    FileIndexCaches.getIndexKeysCache(project, SERVICE_PARAMETER_INDEX_NAMES, ContainerParameterStubIndex.KEY)
                );
            }

            return parameterNames;


        }

    }

}
