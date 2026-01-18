@file:Suppress("FunctionName", "unused")

package fr.adrienbrault.idea.symfony2plugin.mcp.toolset

import com.intellij.mcpserver.McpToolset
import com.intellij.mcpserver.annotations.McpDescription
import com.intellij.mcpserver.annotations.McpTool
import com.intellij.mcpserver.mcpFail
import com.intellij.mcpserver.project
import com.intellij.openapi.application.readAction
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent
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

    @McpTool
    @McpDescription("""
        Generate Symfony service definition in YAML or XML format for a given class.

        This tool analyzes a PHP class to generate an appropriate service container definition.
        It inspects the class constructor to identify dependencies and creates service arguments
        for explicit wiring.

        Simple Symfony YAML example:
        ```yaml
        App\Service\EmailService:
            arguments: ['@mailer', '@logger']
        ```

        Simple Symfony XML example:
        ```xml
        <service id="App\Service\EmailService">
            <argument type="service" id="mailer"/>
        </service>
        ```
    """)

    suspend fun generate_symfony_service_definition(
        @McpDescription("Fully qualified class name for the service (e.g., 'App\\Service\\EmailService')")
        className: String,
        @McpDescription("Output format: 'yaml' (default) or 'xml'")
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
            "xml" -> ServiceDefinitionGenerator.OutputType.XML
            "yaml", "" -> ServiceDefinitionGenerator.OutputType.YAML
            else -> mcpFail("Invalid format: '$format'. Valid values are: 'yaml' or 'xml'")
        }

        return readAction {
            val generator = ServiceDefinitionGenerator(project)
            val definition = generator.generate(className, outputType, useClassNameAsId)
                ?: mcpFail("Class not found: $className")

            definition
        }
    }
}
