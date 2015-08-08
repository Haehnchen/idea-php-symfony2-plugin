package fr.adrienbrault.idea.symfony2plugin.stubs;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.CachedValue;
import com.intellij.util.indexing.FileBasedIndexImpl;
import fr.adrienbrault.idea.symfony2plugin.config.component.parser.ParameterServiceParser;
import fr.adrienbrault.idea.symfony2plugin.dic.ContainerParameter;
import fr.adrienbrault.idea.symfony2plugin.dic.ContainerService;
import fr.adrienbrault.idea.symfony2plugin.dic.XmlServiceParser;
import fr.adrienbrault.idea.symfony2plugin.stubs.cache.FileIndexCaches;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.ContainerParameterStubIndex;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.ServicesDefinitionStubIndex;
import fr.adrienbrault.idea.symfony2plugin.util.service.ServiceXmlParserFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class ContainerCollectionResolver {

    private static final Key<CachedValue<Map<String, List<String[]>>>> SERVICE_CONTAINER_INDEX = new Key<CachedValue<Map<String, List<String[]>>>>("SYMFONY_SERVICE_CONTAINER_INDEX");
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
        private ServiceCollector collector;
        private Source[] sources  = {Source.COMPILER, Source.INDEX};

        public LazyServiceCollector(Project project) {
            this.project = project;
        }

        @NotNull
        public ServiceCollector getCollector() {

            if(this.collector == null) {
                this.collector = new ServiceCollector(project, sources);
            }

            return this.collector;
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
        return new ParameterCollector(project, Source.COMPILER, Source.INDEX).resolve(paramOrClassName);
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

            this.services = new HashMap<String, ContainerService>();

            if(this.sources.contains(Source.COMPILER)) {
                for(Map.Entry<String, String> entry: ServiceXmlParserFactory.getInstance(project, XmlServiceParser.class).getServiceMap().getMap().entrySet()) {
                    services.put(entry.getKey(), new ContainerService(entry.getKey(), entry.getValue()));
                }
            }

            if(this.sources.contains(Source.INDEX)) {
                for (Map.Entry<String, List<String[]>> entry : FileIndexCaches.getSetDataCache(project, SERVICE_CONTAINER_INDEX, SERVICE_CONTAINER_INDEX_NAMES, ServicesDefinitionStubIndex.KEY, ServiceIndexUtil.getRestrictedFileTypesScope(project)).entrySet()) {
                    String serviceName = entry.getKey();
                    if(this.services.containsKey(serviceName)) {
                        continue;
                    }
                    List<String[]> value = entry.getValue();
                    if(value.size() == 0) {
                        this.services.put(serviceName, new ContainerService(serviceName, null, true));
                    } else {
                        this.services.putAll(convertIndexToService(serviceName, value));
                    }
                }
            }


            return this.services;
        }

        private Map<String, ContainerService> convertIndexToService(String serviceName, List<String[]> serviceDefinitions) {

            Map<String, ContainerService> serviceMap = new HashMap<String, ContainerService>();

            for(String[] serviceDefinitionArray: serviceDefinitions) {

                // 0: class name
                // 1: private: (String) "true" if presented
                if(serviceDefinitionArray.length == 0) {
                    // just a fallback should not happen, but provide at least a service name
                    serviceMap.put(serviceName, new ContainerService(serviceName, null, true));
                } else {

                    // resolve class value, it can be null or a parameter
                    String classValue = serviceDefinitionArray[0];
                    if(classValue != null && !classValue.equals("")) {
                        classValue = getParameterCollector().resolve(serviceDefinitionArray[0]);
                    }

                    if(serviceDefinitionArray.length == 1) {
                        serviceMap.put(serviceName, new ContainerService(serviceName, classValue, true));
                    }

                    if(serviceDefinitionArray.length == 2) {
                        serviceMap.put(serviceName, new ContainerService(serviceName, classValue, true, "true".equals(serviceDefinitionArray[1])));
                    }

                }

            }

            return serviceMap;
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

            Set<String> serviceNames = new HashSet<String>();

            if(this.sources.contains(Source.COMPILER)) {
                serviceNames.addAll(ServiceXmlParserFactory.getInstance(project, XmlServiceParser.class).getServiceMap().getMap().keySet());
            }

            if(this.sources.contains(Source.INDEX)) {

                serviceNames.addAll(
                    FileIndexCaches.getIndexKeysCache(project, SERVICE_CONTAINER_INDEX_NAMES, ServicesDefinitionStubIndex.KEY)
                );

                SymfonyProcessors.CollectProjectUniqueKeysStrong projectUniqueKeysStrong = new SymfonyProcessors.CollectProjectUniqueKeysStrong(project, ServicesDefinitionStubIndex.KEY, serviceNames);
                FileBasedIndexImpl.getInstance().processAllKeys(ServicesDefinitionStubIndex.KEY, projectUniqueKeysStrong, project);
                serviceNames.addAll(projectUniqueKeysStrong.getResult());
            }

            return serviceNames;

        }


        private ParameterCollector getParameterCollector() {
            return (this.parameterCollector != null) ? this.parameterCollector : (this.parameterCollector = new ParameterCollector(this.project, this.sources.toArray(new Source[this.sources.size()])));
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

            this.containerParameterMap = new HashMap<String, ContainerParameter>();

            if(this.sources.contains(Source.COMPILER)) {
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

            Set<String> parameterNames = new HashSet<String>();

            if(this.sources.contains(Source.COMPILER)) {
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
