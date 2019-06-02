package fr.adrienbrault.idea.symfony2plugin.stubs;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.CachedValue;
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
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.ServicesDefinitionStubIndex;
import fr.adrienbrault.idea.symfony2plugin.util.dict.ServiceUtil;
import fr.adrienbrault.idea.symfony2plugin.util.service.ServiceXmlParserFactory;
import org.apache.commons.lang.StringUtils;
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

    @Nullable
    public static ContainerService getService(@NotNull Project project, @NotNull String serviceName) {
        Map<String, ContainerService> services = getServices(project);
        return services.getOrDefault(serviceName, null);
    }

    public static Map<String, ContainerService> getServices(@NotNull Project project) {
        return ServiceCollector.create(project).getServices();
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

    public static class ServiceCollector {

        @NotNull
        final private Project project;

        @Nullable
        private ParameterCollector parameterCollector;

        @Nullable
        private Map<String, ContainerService> services;

        public ServiceCollector(@NotNull Project project) {
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
            if(this.services != null) {
                return this.services;
            }

            this.services = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

            // file system
            for(ServiceInterface entry: ServiceXmlParserFactory.getInstance(project, XmlServiceParser.class).getServiceMap().getServices()) {
                // compiled container owns all class names in resolved state
                // api safe check
                if(entry.getClassName() != null) {
                    services.put(entry.getId(), new ContainerService(entry.getId(), entry.getClassName()));
                }
            }

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

            if(exps.size() > 0) {
                exps.forEach(service -> services.put(service.getId(), new ContainerService(service, null)));
            }

            for (Map.Entry<String, List<ServiceSerializable>> entry : FileIndexCaches.getSetDataCache(project, SERVICE_CONTAINER_INDEX, SERVICE_CONTAINER_INDEX_NAMES, ServicesDefinitionStubIndex.KEY, ServiceIndexUtil.getRestrictedFileTypesScope(project)).entrySet()) {

                // dont work twice on service;
                // @TODO: to need to optimize this to decorate as much service data as possible
                String serviceName = entry.getKey();

                // fake empty service, case which is not allowed by catch it
                List<ServiceSerializable> services = entry.getValue();
                if(services.size() == 0) {
                    this.services.put(serviceName, new ContainerService(serviceName, null, true));
                    continue;
                }

                for(ServiceInterface service: services) {
                    String classValue = service.getClassName();

                    // duplicate services
                    if(this.services.containsKey(serviceName)) {
                        if(classValue == null) {
                            continue;
                        }

                        String compiledClassName = this.services.get(serviceName).getClassName();
                        if(classValue.equalsIgnoreCase(compiledClassName)) {
                            continue;
                        }

                        String resolvedClassValue = getParameterCollector().resolve(classValue);
                        if(resolvedClassValue != null && !StringUtils.isBlank(classValue) && !resolvedClassValue.equalsIgnoreCase(compiledClassName)) {
                            this.services.get(serviceName).addClassName(resolvedClassValue);
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
                    this.services.put(serviceName, new ContainerService(service, classValue));
                }
            }

            // replace alias with main service
            if(aliases.size() > 0) {
                collectAliases(aliases);
            }

            if(decorated.size() > 0) {
                collectDecorated(decorated);
            }

            return this.services;
        }

        private void collectAliases(@NotNull Collection<ServiceInterface> aliases) {
            for (ServiceInterface service : aliases) {

                // double check alias name
                String alias = service.getAlias();
                if(alias == null || StringUtils.isBlank(alias) || !this.services.containsKey(alias)) {
                    continue;
                }

                this.services.put(service.getId(), this.services.get(alias));
            }
        }

        private void collectDecorated(@NotNull Collection<ServiceInterface> decorated) {
            for (ServiceInterface service : decorated) {
                String decorationInnerName = service.getDecorationInnerName();
                if(StringUtils.isBlank(decorationInnerName)) {
                    decorationInnerName = service.getId() + ".inner";
                }

                ContainerService origin = this.services.get(service.getDecorates());
                if(origin == null) {
                    continue;
                }

                // @TODO: migrate constructor to ServiceInterface and decorate
                ContainerService value = new ContainerService(decorationInnerName, origin.getClassName(), origin.isWeak(), true);
                origin.getClassNames().forEach(value::addClassName);

                this.services.put(decorationInnerName, value);
            }
        }

        public Set<String> convertClassNameToServices(@NotNull String fqnClassName) {

            Set<String> serviceNames = new HashSet<>();

            fqnClassName = StringUtils.stripStart(fqnClassName, "\\");

            for(Map.Entry<String, ContainerService> entry: this.getServices().entrySet()) {
                for (String className : entry.getValue().getClassNames()) {
                    String indexedClassName = this.getParameterCollector().resolve(className);
                    if(indexedClassName != null) {
                        if(StringUtils.stripStart(indexedClassName, "\\").equalsIgnoreCase(fqnClassName)) {
                            serviceNames.add(entry.getKey());
                        }
                    }
                }
            }

            return serviceNames;
        }

        private Set<String> getNames() {

            Set<String> serviceNames = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

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

            return serviceNames;
        }


        private ParameterCollector getParameterCollector() {
            return (this.parameterCollector != null) ? this.parameterCollector : (this.parameterCollector = ParameterCollector.create(this.project));
        }

        public static ServiceCollector create(@NotNull Project project) {
            return new ContainerCollectionResolver.ServiceCollector(project);
        }
    }

    public static class ParameterCollector {

        @NotNull
        private Project project;

        @Nullable
        private Map<String, ContainerParameter> containerParameterMap;

        public ParameterCollector(@NotNull Project project) {
            this.project = project;
        }

        public static ParameterCollector create(@NotNull Project project) {
            return new ParameterCollector(project);
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

            this.containerParameterMap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);


            // local filesystem
            for(Map.Entry<String, String> Entry: ServiceXmlParserFactory.getInstance(project, ParameterServiceParser.class).getParameterMap().entrySet()) {

                // user input here; secure nullable values
                String key = Entry.getKey();
                if(key != null) {
                    this.containerParameterMap.put(key, new ContainerParameter(key, Entry.getValue()));
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
                if(!this.containerParameterMap.containsKey(parameterName)) {
                    this.containerParameterMap.put(parameterName, new ContainerParameter(parameterName, entry.getValue(), true));
                }
            }

            // setParameter("foo") for ContainerBuilder
            for (ContainerBuilderCall call : FileBasedIndex.getInstance().getValues(ContainerBuilderStubIndex.KEY, "setParameter", GlobalSearchScope.allScope(project))) {
                Collection<String> parameters = call.getParameter();
                if(parameters == null || parameters.size() == 0) {
                    continue;
                }

                for (String parameter : parameters) {
                    if(this.containerParameterMap.containsKey(parameter)) {
                        continue;
                    }

                    this.containerParameterMap.put(parameter, new ContainerParameter(parameter, true));
                }
            }

            // Kernel::getKernelParameters
            for (String parameterName : ServiceUtil.getParameterParameters(project)) {
                if(this.containerParameterMap.containsKey(parameterName)) {
                    continue;
                }

                this.containerParameterMap.put(parameterName, new ContainerParameter(parameterName, true));
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
                this.containerParameterMap.put(extParameter.getName(), extParameter);
            }

            return this.containerParameterMap;
        }

        private Set<String> getNames() {

            // use overall map if already generated
            if(this.containerParameterMap != null) {
                return this.containerParameterMap.keySet();
            }

            Set<String> parameterNames = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

            // local filesystem
            parameterNames.addAll(ServiceXmlParserFactory.getInstance(project, ParameterServiceParser.class).getParameterMap().keySet());

            // index
            parameterNames.addAll(
                FileIndexCaches.getIndexKeysCache(project, SERVICE_PARAMETER_INDEX_NAMES, ContainerParameterStubIndex.KEY)
            );

            // setParameter("foo") for ContainerBuilder
            for (ContainerBuilderCall call : FileBasedIndex.getInstance().getValues(ContainerBuilderStubIndex.KEY, "setParameter", GlobalSearchScope.allScope(project))) {
                Collection<String> parameter = call.getParameter();
                if(parameter != null) {
                    parameterNames.addAll(parameter);
                }
            }


            return parameterNames;
        }

    }

}
