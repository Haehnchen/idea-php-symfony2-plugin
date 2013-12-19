package fr.adrienbrault.idea.symfony2plugin.util.dict;

import com.intellij.openapi.project.Project;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.config.component.parser.ParameterServiceParser;
import fr.adrienbrault.idea.symfony2plugin.stubs.ContainerCollectionResolver;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.service.ServiceXmlParserFactory;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class ServiceUtil {

    @Nullable
    public static PhpClass getResolvedClass(Project project, String className) {

        // possible parameter class %class_name%
        if(className.length() > 1 && className.startsWith("%") && className.endsWith("%")) {
            String resolvedParameter = getResolvedParameter(project, className);
            if(resolvedParameter == null) {
                return null;
            }

            className = resolvedParameter;
        }

        PhpClass phpClass = PhpElementsUtil.getClass(project, className);
        if(phpClass == null) {
            return null;
        }

        return phpClass;
    }

    /**
     * %test%, service, \Class\Name to PhpClass
     */
    @Nullable
    public static PhpClass getResolvedClassDefinition(Project project, String classParameterName) {

        String serviceClass = ContainerCollectionResolver.getClassNameFromService(project, classParameterName);
        if(serviceClass != null) {
            return PhpElementsUtil.getClassInterface(project, serviceClass);
        }

        serviceClass = ContainerCollectionResolver.resolveParameterClass(project, classParameterName);
        if(serviceClass == null) {
            return null;
        }

        return PhpElementsUtil.getClassInterface(project, serviceClass);
    }

    /**
     * Get value
     *
     * @param parameterName parameter with or without "%"
     * @return resolved parameter can be eg a classname
     */
    @Nullable
    public static String getResolvedParameter(Project project, String parameterName) {

        // strip "%"
        if(parameterName.length() > 1 && parameterName.startsWith("%") && parameterName.endsWith("%")) {
            parameterName = parameterName.substring(1, parameterName.length() - 1);
        }

        Map<String, String> serviceMap = ServiceXmlParserFactory.getInstance(project, ParameterServiceParser.class).getParameterMap();
        if(serviceMap.containsKey(parameterName)) {
            return serviceMap.get(parameterName);
        }

        // lowercase also valid
        if(serviceMap.containsKey(parameterName.toLowerCase())) {
            return serviceMap.get(parameterName.toLowerCase());
        }

        return null;
    }

}
