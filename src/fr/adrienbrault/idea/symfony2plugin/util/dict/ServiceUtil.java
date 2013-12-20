package fr.adrienbrault.idea.symfony2plugin.util.dict;

import com.intellij.openapi.project.Project;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.stubs.ContainerCollectionResolver;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import org.jetbrains.annotations.Nullable;

public class ServiceUtil {

    /**
     * %test%, service, \Class\Name to PhpClass
     */
    @Nullable
    public static PhpClass getResolvedClassDefinition(Project project, String serviceClassParameterName) {

        // match parameter
        if(serviceClassParameterName.startsWith("%") && serviceClassParameterName.endsWith("%")) {
            String serviceClass = ContainerCollectionResolver.resolveParameter(project, serviceClassParameterName);

            if(serviceClass != null) {
                return PhpElementsUtil.getClassInterface(project, serviceClass);
            }

            return null;
        }

        // service names dont have namespaces
        if(!serviceClassParameterName.contains("\\")) {
            String serviceClass = ContainerCollectionResolver.resolveService(project, serviceClassParameterName);
            if(serviceClass != null) {
                return PhpElementsUtil.getClassInterface(project, serviceClass);
            }
        }

        // fallback to class name with and without namespaces
        return PhpElementsUtil.getClassInterface(project, serviceClassParameterName);
    }

}
