package fr.adrienbrault.idea.symfony2plugin.util.dict;

import com.intellij.openapi.project.Project;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.config.component.parser.ParameterServiceParser;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.service.ServiceXmlParserFactory;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class ServiceUtil {

    @Nullable
    public static PhpClass getResolvedClass(Project project, String className) {

        if(className.startsWith("%") && className.endsWith("%")) {
            className = className.substring(1, className.length() - 1);
            Map<String, String> serviceMap = ServiceXmlParserFactory.getInstance(project, ParameterServiceParser.class).getParameterMap();
            if(!serviceMap.containsKey(className)) {
                return null;
            }
            className = serviceMap.get(className);
        }

        PhpIndex phpIndex = PhpIndex.getInstance(project);
        PhpClass phpClass = PhpElementsUtil.getClass(phpIndex, className);
        if(phpClass == null) {
            return null;
        }

        return phpClass;
    }

}
