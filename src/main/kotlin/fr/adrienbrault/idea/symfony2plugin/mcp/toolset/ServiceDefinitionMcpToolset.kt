@file:Suppress("FunctionName", "unused")

package fr.adrienbrault.idea.symfony2plugin.mcp.toolset

import com.intellij.mcpserver.McpToolset
import com.intellij.mcpserver.annotations.McpDescription
import com.intellij.mcpserver.annotations.McpTool
import com.intellij.mcpserver.mcpFail
import com.intellij.mcpserver.project
import com.intellij.openapi.application.readAction
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent
import fr.adrienbrault.idea.symfony2plugin.action.ui.ServiceBuilder
import fr.adrienbrault.idea.symfony2plugin.mcp.McpUtil
import fr.adrienbrault.idea.symfony2plugin.mcp.service.ServiceDefinitionGenerator
import kotlinx.coroutines.currentCoroutineContext
import org.apache.commons.lang3.StringUtils

/**
 * MCP toolset for generating Symfony service definitions.
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
class ServiceDefinitionMcpToolset : McpToolset {
    private val serviceDefinitionGenerator = ServiceDefinitionGenerator()
    private val maxServicesPerParameterSuggestion = 15

    private data class CommentSyntax(
        val lineFormat: String,
    )

    internal fun generateDefinitions(
        project: com.intellij.openapi.project.Project,
        classNames: String,
        outputType: ServiceBuilder.OutputType,
        useClassNameAsId: Boolean
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
        modelParameters: List<fr.adrienbrault.idea.symfony2plugin.action.ui.MethodParameter.MethodModelParameter>,
        commentSyntax: CommentSyntax,
    ): String {
        val commentLines = formatServiceSuggestions(modelParameters, commentSyntax)
        if (commentLines.isEmpty()) {
            return content
        }

        return content + "\n\n" + commentLines.joinToString("\n")
    }

    private fun formatServiceSuggestions(
        modelParameters: List<fr.adrienbrault.idea.symfony2plugin.action.ui.MethodParameter.MethodModelParameter>,
        commentSyntax: CommentSyntax,
    ): List<String> {
        if (modelParameters.isEmpty() || modelParameters.none { it.isPossibleService && it.possibleServices.size > 1 }) {
            return emptyList()
        }

        return buildList {
            add(commentSyntax.lineFormat.format("Possible services per parameter:"))
            modelParameters
                .forEach { add(commentSyntax.lineFormat.format(formatServiceSuggestion(it))) }
        }
    }

    private fun formatServiceSuggestion(modelParameter: fr.adrienbrault.idea.symfony2plugin.action.ui.MethodParameter.MethodModelParameter): String {
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

    @McpTool
    @McpDescription(
        $$"""
        Generate Symfony service definitions in YAML, XML, Fluent PHP or PHP array format for one or more classes.
        Inspects each class constructor to guess service dependencies from parameter types for explicit wiring.

        Fluent PHP example:
        ```php
        $services->set(\App\EmailService::class)
            ->args([
                service('mailer')
            ]);
        ```

        PHP array example (eg in `App::config()` or `return`):
        ```php
        [
            \App\EmailService::class => [
                'arguments' => [
                    service('mailer')
                ],
            ],
        ];
        ```
    """
    )

    suspend fun generate_symfony_service_definition(
        @McpDescription("Fully qualified class name for the service, or a comma-separated list of class names (e.g., '\\App\\Service\\EmailService')")
        className: String,
        @McpDescription("Output format: 'yaml' (default), 'xml', 'fluent' or 'phparray'")
        format: String = "yaml",
        @McpDescription("If true, uses class name as service ID (default); if false, generates a short ID")
        useClassNameAsId: Boolean = true
    ): String {
        val project = currentCoroutineContext().project

        if (!Symfony2ProjectComponent.isEnabled(project)) {
            mcpFail("Symfony plugin is not enabled for this project.")
        }

        McpUtil.checkToolEnabled(project, "generate_symfony_service_definition")

        if (StringUtils.isBlank(className)) {
            mcpFail("className parameter is required.")
        }

        val outputType = when (format.lowercase()) {
            "xml" -> ServiceBuilder.OutputType.XML
            "yaml", "" -> ServiceBuilder.OutputType.Yaml
            "fluent" -> ServiceBuilder.OutputType.Fluent
            "phparray" -> ServiceBuilder.OutputType.PhpArray
            else -> mcpFail("Invalid format: '$format'. Valid values are: 'yaml', 'xml', 'fluent' or 'phparray'")
        }

        return readAction {
            generateDefinitions(project, className, outputType, useClassNameAsId)
        }
    }
}
