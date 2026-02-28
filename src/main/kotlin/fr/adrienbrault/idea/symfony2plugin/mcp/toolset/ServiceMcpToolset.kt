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
import fr.adrienbrault.idea.symfony2plugin.mcp.collector.SymfonyServiceLocatorCollector
import kotlinx.coroutines.currentCoroutineContext

/**
 * MCP toolset for Symfony service container.
 * Provides access to service definitions configured in the Symfony project.
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
class ServiceMcpToolset : McpToolset {

    @McpTool
    @McpDescription("""
        Locate in which lineNumber a Symfony service is defined in configuration files by service name or class name.

        Returns CSV format with columns: serviceName,className,filePath,lineNumber
        - serviceName: The service ID/name (from service definition)
        - className: FQN of the service class (if available)
        - filePath: Relative path from project root
        - lineNumber: Line number where the service definition starts (1-indexed)

        IMPORTANT: The lineNumber indicates only the START of the service definition.
        Service definitions are multi-line YAML/XML/PHP blocks. You MUST read a range
        of lines (around the lineNumber, typically 10-20 lines depending on complexity)
        to capture the complete service definition.

        Note: Autowired services (automatically registered by Symfony based on class names)
        do not have explicit definitions in config files.

        Example output:
        serviceName,className,filePath,lineNumber
        app.service.my_service,\App\Service\MyService,config/services.yaml,15
        app.my_service_alias,\App\Service\MyService,config/services.yaml,25
    """)
    suspend fun locate_symfony_service(identifier: String): String {
        val project = currentCoroutineContext().project

        if (!Symfony2ProjectComponent.isEnabled(project)) {
            mcpFail("Symfony plugin is not enabled for this project.")
        }

        McpUtil.checkToolEnabled(project, "locate_symfony_service")

        if (identifier.isBlank()) {
            mcpFail("identifier parameter is required.")
        }

        return readAction {
            SymfonyServiceLocatorCollector(project).collect(identifier)
        }
    }
}
