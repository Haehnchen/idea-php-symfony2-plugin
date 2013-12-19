package fr.adrienbrault.idea.symfony2plugin.stubs;

import com.intellij.openapi.project.Project;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.indexing.FileBasedIndexImpl;
import com.jetbrains.php.PhpIndex;
import fr.adrienbrault.idea.symfony2plugin.config.component.parser.ParameterServiceParser;
import fr.adrienbrault.idea.symfony2plugin.dic.ContainerParameter;
import fr.adrienbrault.idea.symfony2plugin.dic.XmlServiceParser;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.ContainerParameterStubIndex;
import fr.adrienbrault.idea.symfony2plugin.util.service.ServiceXmlParserFactory;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class ContainerCollectionResolver {

    public static enum Source {
        INDEX, COMPILER
    }

    @Nullable
    public static String getClassNameFromService(Project project, String serviceName) {
        return getClassNameFromService(project, serviceName, ContainerCollectionResolver.Source.COMPILER, ContainerCollectionResolver.Source.INDEX);
    }

    @Nullable
    public static String getClassNameFromService(Project project, String serviceName, ContainerCollectionResolver.Source... collectorSources) {

        Set<Source> collectors = new HashSet<Source>(Arrays.asList(collectorSources));

        String serviceClass = null;

        if(collectors.contains(ContainerCollectionResolver.Source.COMPILER)) {
            serviceClass = ServiceXmlParserFactory.getInstance(project, XmlServiceParser.class).getServiceMap().getMap().get(serviceName);
        }

        if(serviceClass == null && collectors.contains(ContainerCollectionResolver.Source.INDEX)) {
            serviceClass = ServiceIndexUtil.getServiceClassOnIndex(project, serviceName);
        }

        return serviceClass;

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
    public static String resolveParameterClass(Project project, String paramOrClassName) {

        // strip "%" to get the parameter name
        if(paramOrClassName.length() > 1 && paramOrClassName.startsWith("%") && paramOrClassName.endsWith("%")) {
            paramOrClassName = paramOrClassName.substring(1, paramOrClassName.length() - 1);

            // parameter is always lower see #179
            paramOrClassName = paramOrClassName.toLowerCase();

            // search on compiled file
            Map<String, String> parameterMap = ServiceXmlParserFactory.getInstance(project, ParameterServiceParser.class).getParameterMap();
            if(parameterMap.containsKey(paramOrClassName)) {
                return parameterMap.get(paramOrClassName);
            }

            // search on indexes
            ContainerParameter containerParameter = getParameter(project, paramOrClassName);
            if(containerParameter != null) {
                return containerParameter.getValue();
            }

            return null;
        }

        return paramOrClassName;
    }

    @Nullable
    public static ContainerParameter getParameter(Project project, String parameterName) {
        Map<String, ContainerParameter> parameters = getParameters(project, Source.COMPILER, Source.INDEX);
        if(parameters.containsKey(parameterName)) {
            return parameters.get(parameterName);
        }

        return null;
    }

    public static Map<String, ContainerParameter> getParameters(Project project) {
        return getParameters(project, ContainerCollectionResolver.Source.COMPILER, ContainerCollectionResolver.Source.INDEX);
    }

    public static Map<String, ContainerParameter> getParameters(Project project, ContainerCollectionResolver.Source... collectorSources) {
        Set<Source> collectors = new HashSet<Source>(Arrays.asList(collectorSources));

        Map<String, ContainerParameter> parameterMap = new HashMap<String, ContainerParameter>();


        if(collectors.contains(Source.COMPILER)) {
            for(Map.Entry<String, String> Entry: ServiceXmlParserFactory.getInstance(project, ParameterServiceParser.class).getParameterMap().entrySet()) {
                parameterMap.put(Entry.getKey(), new ContainerParameter(Entry.getKey(), Entry.getValue()));
            }
        }

        if(collectors.contains(Source.INDEX)) {

            for(String parameterName: FileBasedIndexImpl.getInstance().getAllKeys(ContainerParameterStubIndex.KEY, project)) {

                // indexes is weak stuff, dont overwrite compiled ones
                if(!parameterMap.containsKey(parameterName)) {

                    String value = null;

                    // one parameter definition can be in multiple files, use first match for now
                    // @TODO: at least we should skip null
                    List<String> parameterValues = FileBasedIndexImpl.getInstance().getValues(ContainerParameterStubIndex.KEY, parameterName, GlobalSearchScope.projectScope(project));
                    if(parameterValues.size() > 0) {
                        value = parameterValues.get(0);
                    }

                    parameterMap.put(parameterName, new ContainerParameter(parameterName, value, true));

                }
            }

        }


        return parameterMap;
    }

    public static Set<String> getParameterNames(Project project) {
        return getParameterNames(project, ContainerCollectionResolver.Source.COMPILER, ContainerCollectionResolver.Source.INDEX);
    }

    public static Set<String> getParameterNames(Project project, ContainerCollectionResolver.Source... collectorSources) {
        Set<Source> collectors = new HashSet<Source>(Arrays.asList(collectorSources));

        Set<String> parameterNames = new HashSet<String>();

        if(collectors.contains(Source.COMPILER)) {
            parameterNames.addAll(ServiceXmlParserFactory.getInstance(project, ParameterServiceParser.class).getParameterMap().values());
        }

        if(collectors.contains(Source.INDEX)) {
            parameterNames.addAll(FileBasedIndexImpl.getInstance().getAllKeys(ContainerParameterStubIndex.KEY, project));
        }

        return parameterNames;

    }


}
