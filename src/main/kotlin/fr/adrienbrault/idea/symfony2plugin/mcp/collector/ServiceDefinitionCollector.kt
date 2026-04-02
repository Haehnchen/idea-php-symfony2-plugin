package fr.adrienbrault.idea.symfony2plugin.mcp.collector

import com.intellij.mcpserver.mcpFail
import com.intellij.openapi.project.Project
import fr.adrienbrault.idea.symfony2plugin.action.ui.MethodParameter
import fr.adrienbrault.idea.symfony2plugin.action.ui.ServiceBuilder
import fr.adrienbrault.idea.symfony2plugin.mcp.service.ServiceDefinitionGenerator
import org.apache.commons.lang3.StringUtils

/**
 * Collects MCP output for generated Symfony service definitions.
 *
 * This collector accepts one or more class names, delegates the actual definition generation
 * to [ServiceDefinitionGenerator], and then enriches the generated YAML/XML/PHP output with
 * additional comment lines for constructor arguments that have multiple matching service ids.
 *
 * The extra comments are intended for MCP consumers only, so the tool can return both the
 * generated definition and relevant service-id alternatives in a single response.
 */
class ServiceDefinitionCollector(private val project: Project) {
    private val serviceDefinitionGenerator = ServiceDefinitionGenerator()
    private val maxServicesPerParameterSuggestion = 15

    private data class CommentSyntax(
        val lineFormat: String,
    )

    fun collect(
        classNames: String,
        outputType: ServiceBuilder.OutputType,
        useClassNameAsId: Boolean,
    ): String {
        val classes = classNames
            .split(',')
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        if (classes.isEmpty()) {
            mcpFail("className parameter is required.")
        }

        return classes.joinToString("\n---\n") { className ->
            val model = serviceDefinitionGenerator.createModel(project, className)
                ?: return@joinToString "Error: Class not found: $className"

            val definition = serviceDefinitionGenerator.generate(project, className, outputType, useClassNameAsId)
                ?: return@joinToString "Error: Class not found: $className"

            appendServiceSuggestions(definition, model.modelParameters, getCommentSyntax(outputType))
        }
    }

    private fun getCommentSyntax(outputType: ServiceBuilder.OutputType): CommentSyntax {
        return when (outputType) {
            ServiceBuilder.OutputType.Yaml -> CommentSyntax("# %s")
            ServiceBuilder.OutputType.XML -> CommentSyntax("<!-- %s -->")
            ServiceBuilder.OutputType.Fluent, ServiceBuilder.OutputType.PhpArray -> CommentSyntax("// %s")
        }
    }

    private fun appendServiceSuggestions(
        content: String,
        modelParameters: List<MethodParameter.MethodModelParameter>,
        commentSyntax: CommentSyntax,
    ): String {
        val commentLines = formatServiceSuggestions(modelParameters, commentSyntax)
        if (commentLines.isEmpty()) {
            return content
        }

        return content + "\n\n" + commentLines.joinToString("\n")
    }

    private fun formatServiceSuggestions(
        modelParameters: List<MethodParameter.MethodModelParameter>,
        commentSyntax: CommentSyntax,
    ): List<String> {
        if (modelParameters.isEmpty() || modelParameters.none { it.isPossibleService && it.possibleServices.size > 1 }) {
            return emptyList()
        }

        return buildList {
            add(commentSyntax.lineFormat.format("Possible services per parameter:"))
            modelParameters.forEach { add(commentSyntax.lineFormat.format(formatServiceSuggestion(it))) }
        }
    }

    private fun formatServiceSuggestion(modelParameter: MethodParameter.MethodModelParameter): String {
        val parameterName = "$${modelParameter.parameter.name}"
        val declaredType = StringUtils.stripStart(modelParameter.parameter.declaredType.toString(), "\\")
        val displayType = declaredType.takeIf { it.isNotBlank() } ?: "mixed"
        val possibleServices = modelParameter.possibleServices
            .take(maxServicesPerParameterSuggestion)
            .joinToString(", ") { service -> if (service.contains(',')) "\"$service\"" else service }

        return if (modelParameter.isPossibleService && modelParameter.possibleServices.size > 1) {
            "$parameterName [$displayType] => $possibleServices"
        } else {
            "$parameterName [$displayType]"
        }
    }
}