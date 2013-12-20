package fr.adrienbrault.idea.symfony2plugin.stubs;

import com.intellij.ide.highlighter.XmlFileType;
import com.intellij.openapi.project.Project;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.indexing.FileBasedIndexImpl;
import fr.adrienbrault.idea.symfony2plugin.config.component.parser.ParameterServiceParser;
import fr.adrienbrault.idea.symfony2plugin.dic.ContainerParameter;
import fr.adrienbrault.idea.symfony2plugin.dic.ContainerService;
import fr.adrienbrault.idea.symfony2plugin.dic.XmlServiceParser;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.ContainerParameterStubIndex;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.ServicesDefinitionStubIndex;
import fr.adrienbrault.idea.symfony2plugin.util.service.ServiceXmlParserFactory;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.yaml.YAMLFileType;

import java.util.*;

public class ContainerCollectionResolver {

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

    public static Collection<ContainerService> getServices(Project project) {
        return getServices(project, ContainerCollectionResolver.Source.COMPILER, ContainerCollectionResolver.Source.INDEX);
    }

    public static Collection<ContainerService> getServices(Project project, ContainerCollectionResolver.Source... collectorSources) {
        return new ServiceCollector(project, collectorSources).getServices().values();
    }

    @Nullable
    public static String resolveService(Project project, String serviceName) {
        return resolveService(project, serviceName, ContainerCollectionResolver.Source.COMPILER, ContainerCollectionResolver.Source.INDEX);
    }

    @Nullable
    public static String resolveService(Project project, String serviceName, ContainerCollectionResolver.Source... collectorSources) {
        return new ServiceCollector(project, collectorSources).resolve(serviceName);
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
                for(String serviceName: FileBasedIndexImpl.getInstance().getAllKeys(ServicesDefinitionStubIndex.KEY, project)) {

                    // we have higher priority on compiler, which already has safe value
                    if(!this.services.containsKey(serviceName)) {

                        List<Set<String>> serviceDefinitions = FileBasedIndexImpl.getInstance().getValues(ServicesDefinitionStubIndex.KEY, serviceName, GlobalSearchScope.getScopeRestrictedByFileTypes(GlobalSearchScope.allScope(project), XmlFileType.INSTANCE, YAMLFileType.YML));

                        if(serviceDefinitions.size() == 0) {
                            this.services.put(serviceName, new ContainerService(serviceName, null, true));
                        } else {
                            for(Set<String> parameterValues: serviceDefinitions) {

                                // 0: class name
                                // 1: private: (String) "true" if presented
                                String[] serviceDefinitionArray = parameterValues.toArray(new String[parameterValues.size()]);

                                if(serviceDefinitionArray.length == 0) {
                                    this.services.put(serviceName, new ContainerService(serviceName, null, true));
                                }

                                if(serviceDefinitionArray.length == 1) {
                                    this.services.put(serviceName, new ContainerService(serviceName, getParameterCollector().resolve(serviceDefinitionArray[0]), true));
                                }

                                if(serviceDefinitionArray.length == 2) {
                                    this.services.put(serviceName, new ContainerService(serviceName, getParameterCollector().resolve(serviceDefinitionArray[0]), true, "true".equals(serviceDefinitionArray[1])));
                                }

                            }
                        }


                    }

                }
            }


            return this.services;
        }

        public Set<String> convertClassNameToServices(String fqnClassName) {

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
                serviceNames.addAll(FileBasedIndexImpl.getInstance().getAllKeys(ServicesDefinitionStubIndex.KEY, project));
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
        private String resolve(String paramOrClassName) {

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
                    this.containerParameterMap.put(Entry.getKey(), new ContainerParameter(Entry.getKey(), Entry.getValue()));
                }
            }

            if(this.sources.contains(Source.INDEX)) {

                for(String parameterName: FileBasedIndexImpl.getInstance().getAllKeys(ContainerParameterStubIndex.KEY, project)) {

                    // indexes is weak stuff, dont overwrite compiled ones
                    if(!this.containerParameterMap.containsKey(parameterName)) {

                        String value = null;

                        // one parameter definition can be in multiple files, use first match for now
                        // @TODO: at least we should skip null
                        List<String> parameterValues = FileBasedIndexImpl.getInstance().getValues(ContainerParameterStubIndex.KEY, parameterName, GlobalSearchScope.getScopeRestrictedByFileTypes(GlobalSearchScope.allScope(project), XmlFileType.INSTANCE, YAMLFileType.YML));
                        if(parameterValues.size() > 0) {
                            value = parameterValues.get(0);
                        }

                        this.containerParameterMap.put(parameterName, new ContainerParameter(parameterName, value, true));

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
                parameterNames.addAll(FileBasedIndexImpl.getInstance().getAllKeys(ContainerParameterStubIndex.KEY, project));
            }

            return parameterNames;


        }

    }

}
