package fr.adrienbrault.idea.symfony2plugin.dic;

import com.intellij.openapi.project.Project;
import fr.adrienbrault.idea.symfony2plugin.config.component.parser.ParameterServiceParser;
import fr.adrienbrault.idea.symfony2plugin.util.service.ServiceIndexUtil;
import fr.adrienbrault.idea.symfony2plugin.util.service.ServiceXmlParserFactory;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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
     * Resolve service class name which can be a class name or parameter
     *
     * @param project project
     * @param parameterName any raw service name or parameter name
     * @return class name or unchanged item
     */
    @Nullable
    public static String resolveParameterClass(Project project, String parameterName) {

        // strip "%"
        if(parameterName.length() > 1 && parameterName.startsWith("%") && parameterName.endsWith("%")) {
            parameterName = parameterName.substring(1, parameterName.length() - 1);

            Map<String, String> parameterMap = ServiceXmlParserFactory.getInstance(project, ParameterServiceParser.class).getParameterMap();
            if(parameterMap.containsKey(parameterName)) {
                return parameterMap.get(parameterName);
            }

            return null;
        }

        return parameterName;
    }

}
