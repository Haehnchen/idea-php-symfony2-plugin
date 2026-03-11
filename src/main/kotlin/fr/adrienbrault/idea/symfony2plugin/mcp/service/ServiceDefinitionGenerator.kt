package fr.adrienbrault.idea.symfony2plugin.mcp.service

import com.intellij.openapi.project.Project
import com.jetbrains.php.lang.psi.elements.ClassReference
import com.jetbrains.php.lang.psi.elements.Method
import com.jetbrains.php.lang.psi.elements.Parameter
import com.jetbrains.php.lang.psi.elements.PhpClass
import fr.adrienbrault.idea.symfony2plugin.action.ServiceActionUtil
import fr.adrienbrault.idea.symfony2plugin.action.ui.MethodParameter
import fr.adrienbrault.idea.symfony2plugin.action.ui.ServiceBuilder
import fr.adrienbrault.idea.symfony2plugin.dic.ContainerService
import fr.adrienbrault.idea.symfony2plugin.stubs.ContainerCollectionResolver
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil
import fr.adrienbrault.idea.symfony2plugin.util.dict.ServiceUtil
import org.apache.commons.lang3.StringUtils

/**
 * Generates Symfony service definitions in YAML or XML format based on a class name.
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
class ServiceDefinitionGenerator(private val project: Project) {

    /**
     * Generates a service definition for the given class.
     * Validation should be handled by the caller (e.g., MCP tool).
     *
     * @param className The fully qualified class name (FQN) - should be validated by caller
     * @param outputType The output format (YAML or XML)
     * @param useClassNameAsId If true, use class name as service ID; if false, use generated snake_case service name
     * @return The generated service definition as a string, or null if generation fails
     */
    @JvmOverloads
    fun generate(className: String, outputType: OutputType, useClassNameAsId: Boolean = true): String? {
        // Normalize class name (strip leading backslash)
        val normalizedClassName = StringUtils.stripStart(className, "\\")

        val phpClass: PhpClass = PhpElementsUtil.getClass(project, normalizedClassName) ?: return null

        // Get default service name (snake_case generated from class name)
        val serviceName = ServiceUtil.getServiceNameForClass(project, normalizedClassName)

        // Collect all services for type resolution
        val serviceClass: Map<String, ContainerService> = ContainerCollectionResolver.getServices(project)
        val serviceSetComplete = sortedSetOf("").also { it.addAll(serviceClass.keys) }

        // Build method parameters
        val modelParameters = collectMethodParameters(phpClass, serviceClass, serviceSetComplete)

        // Sort parameters
        modelParameters.sortWith(
            compareBy<MethodParameter.MethodModelParameter> { it.name }
                .thenComparingInt { it.index }
        )

        // Build the service definition
        val builderOutputType = if (outputType == OutputType.YAML) ServiceBuilder.OutputType.Yaml else ServiceBuilder.OutputType.XML

        val builder = ServiceBuilder(modelParameters, project, useClassNameAsId)
        return builder.build(builderOutputType, normalizedClassName, serviceName)
    }

    /**
     * Collects method parameters from the given class that are potential service dependencies.
     */
    private fun collectMethodParameters(
        phpClass: PhpClass,
        serviceClass: Map<String, ContainerService>,
        serviceSetComplete: Set<String>
    ): MutableList<MethodParameter.MethodModelParameter> {
        val modelParameters = mutableListOf<MethodParameter.MethodModelParameter>()

        for (method: Method in phpClass.methods) {
            if (method.modifier.isPublic) {
                val parameters: Array<Parameter> = method.parameters
                for (i in parameters.indices) {
                    val possibleServices = getPossibleServices(parameters[i], serviceClass)
                    if (possibleServices.isNotEmpty()) {
                        val serviceName = getServiceName(possibleServices)
                        modelParameters.add(MethodParameter.MethodModelParameter(method, parameters[i], i, possibleServices, serviceName))
                    } else {
                        modelParameters.add(MethodParameter.MethodModelParameter(method, parameters[i], i, serviceSetComplete))
                    }
                }
            }
        }

        return modelParameters
    }

    /**
     * Gets possible services for a given parameter type.
     */
    private fun getPossibleServices(parameter: Parameter, serviceClass: Map<String, ContainerService>): Set<String> {
        val phpPsiElement = parameter.firstPsiChild
        if (phpPsiElement !is ClassReference) {
            return emptySet()
        }

        val type = phpPsiElement.fqn ?: return emptySet()

        return ServiceActionUtil.getPossibleServices(project, type, serviceClass)
    }

    /**
     * Gets the best service name from a set of possible services.
     * Returns the first one (services are sorted by priority).
     */
    private fun getServiceName(services: Set<String>): String? {
        return if (services.isEmpty()) null else services.iterator().next()
    }

    /**
     * Output format for service definitions.
     */
    enum class OutputType {
        YAML,
        XML
    }
}
