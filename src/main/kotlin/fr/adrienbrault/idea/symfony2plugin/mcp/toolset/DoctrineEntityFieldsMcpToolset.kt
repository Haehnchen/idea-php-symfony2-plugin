@file:Suppress("FunctionName", "unused")

package fr.adrienbrault.idea.symfony2plugin.mcp.toolset

import com.intellij.mcpserver.McpToolset
import com.intellij.mcpserver.annotations.McpDescription
import com.intellij.mcpserver.annotations.McpTool
import com.intellij.mcpserver.mcpFail
import com.intellij.mcpserver.project
import com.intellij.openapi.application.readAction
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent
import fr.adrienbrault.idea.symfony2plugin.mcp.collector.DoctrineEntityFieldsCollector
import fr.adrienbrault.idea.symfony2plugin.mcp.McpUtil
import kotlinx.coroutines.currentCoroutineContext

/**
 * MCP toolset for Doctrine entity field information.
 * Provides detailed field information for a specific entity class.
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
class DoctrineEntityFieldsMcpToolset : McpToolset {

    @McpTool
    @McpDescription("""
        Lists all fields of a Doctrine entity as CSV.

        Parameters:
        - className: FQN of the entity class (e.g., "App\Entity\User" or "\App\Entity\User")

        Returns CSV format with columns: name,column,type,relation,relationType,enumType,propertyType
        - name: Field/property name
        - column: Database column name
        - type: Doctrine type (string, integer, text, etc.)
        - relation: Related entity class if this is a relation field
        - relationType: Type of relation (OneToOne, OneToMany, ManyToOne, ManyToMany)
        - enumType: FQN of the enum class for enum fields (PHP 8.1+)
        - propertyType: Pipe-separated PHP property types (e.g. "string", "\App\Status|null")

        Example output:
        name,column,type,relation,relationType,enumType,propertyType
        id,id,integer,,,,int
        username,username,string,,,string
        status,status,string,,,\App\Enum\Status,\App\Enum\Status
        orders,orders,,App\Entity\Order,OneToMany,,
    """)
    suspend fun list_doctrine_entity_fields(className: String): String {
        val project = currentCoroutineContext().project

        if (!Symfony2ProjectComponent.isEnabled(project)) {
            mcpFail("Symfony plugin is not enabled for this project.")
        }

        McpUtil.checkToolEnabled(project, "list_doctrine_entity_fields")

        if (className.isBlank()) {
            mcpFail("className parameter is required.")
        }

        return readAction {
            DoctrineEntityFieldsCollector(project).collect(className)
        }
    }
}
