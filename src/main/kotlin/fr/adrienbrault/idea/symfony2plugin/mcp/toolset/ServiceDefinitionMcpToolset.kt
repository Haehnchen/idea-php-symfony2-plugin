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
            serviceDefinitionGenerator.generate(project, className, outputType, useClassNameAsId)
                ?: "Error: Class not found: $className"
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
