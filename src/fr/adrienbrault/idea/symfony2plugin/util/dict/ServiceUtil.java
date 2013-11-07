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

        if(className.length() > 1 && className.startsWith("%") && className.endsWith("%")) {
            className = className.substring(1, className.length() - 1);
            Map<String, String> serviceMap = ServiceXmlParserFactory.getInstance(project, ParameterServiceParser.class).getParameterMap();
            if(!serviceMap.containsKey(className)) {
                return null;
            }
            className = serviceMap.get(className);
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

}
