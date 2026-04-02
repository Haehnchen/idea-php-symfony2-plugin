package fr.adrienbrault.idea.symfony2plugin.mcp.service

import com.intellij.openapi.project.Project
import fr.adrienbrault.idea.symfony2plugin.action.ui.ServiceBuilder
import org.apache.commons.lang3.StringUtils

/**
 * Generates Symfony service definitions for class name.
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
class ServiceDefinitionGenerator {

    data class ServiceDefinitionModel(
        val serviceName: String,
        val modelParameters: MutableList<fr.adrienbrault.idea.symfony2plugin.action.ui.MethodParameter.MethodModelParameter>
    )

    /**
     * Generates a service definition for the given class.
     * Validation should be handled by the caller (e.g., MCP tool).
     *
     * @param className The fully qualified class name (FQN) - should be validated by caller
     */
    fun generate(project: Project, className: String, outputType: ServiceBuilder.OutputType, useClassNameAsId: Boolean = true): String? {
        val model = createModel(project, className) ?: return null

        val normalizedClassName = StringUtils.stripStart(className, "\\")

        val builder = ServiceBuilder(model.modelParameters, project, useClassNameAsId)
        return builder.build(outputType, normalizedClassName, model.serviceName)
    }

    fun createModel(project: Project, className: String): ServiceDefinitionModel? =
        ServiceBuilder.createModel(project, className)?.toModel()
}

private fun ServiceBuilder.ServiceDefinitionModel.toModel(): ServiceDefinitionGenerator.ServiceDefinitionModel {
    return ServiceDefinitionGenerator.ServiceDefinitionModel(serviceName, modelParameters.toMutableList())
}
