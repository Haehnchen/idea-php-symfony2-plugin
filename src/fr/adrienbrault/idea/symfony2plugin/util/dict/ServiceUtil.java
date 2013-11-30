package fr.adrienbrault.idea.symfony2plugin.util.dict;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.config.component.parser.ParameterServiceParser;
import fr.adrienbrault.idea.symfony2plugin.dic.XmlServiceParser;
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
    public static PhpClass getResolvedClassDefinition(Project project, String className) {

        String serviceClass = ServiceXmlParserFactory.getInstance(project, XmlServiceParser.class).getServiceMap().getMap().get(className);
        if (null != serviceClass) {
            PsiElement[] psiElements = PhpElementsUtil.getClassInterfacePsiElements(project, serviceClass);
            for(PsiElement psiElement: psiElements) {
                if(psiElement instanceof PhpClass) {
                    return (PhpClass) psiElement;
                }
            }
        }

        // parameter and direct class name
        PhpClass phpClass = getResolvedClass(project, className);
        if(phpClass != null) {
            return phpClass;
        }

        return null;
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
