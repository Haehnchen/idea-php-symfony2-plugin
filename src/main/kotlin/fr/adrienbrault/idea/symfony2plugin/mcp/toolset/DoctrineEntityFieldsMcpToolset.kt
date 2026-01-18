@file:Suppress("FunctionName", "unused")

package fr.adrienbrault.idea.symfony2plugin.mcp.toolset

import com.intellij.mcpserver.McpToolset
import com.intellij.mcpserver.annotations.McpDescription
import com.intellij.mcpserver.annotations.McpTool
import com.intellij.mcpserver.mcpFail
import com.intellij.mcpserver.project
import com.intellij.openapi.application.readAction
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent
import fr.adrienbrault.idea.symfony2plugin.doctrine.EntityHelper
import fr.adrienbrault.idea.symfony2plugin.mcp.McpUtil
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil
import kotlinx.coroutines.currentCoroutineContext
import org.apache.commons.lang3.StringUtils

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

        Returns CSV format with columns: name,column,type,relation,relationType
        - name: Field/property name
        - column: Database column name
        - type: Doctrine type (string, integer, text, etc.)
        - relation: Related entity class if this is a relation field
        - relationType: Type of relation (OneToOne, OneToMany, ManyToOne, ManyToMany)

        Example output:
        name,column,type,relation,relationType
        id,id,integer,,
        username,username,string,,
        email,email,string,,
        orders,orders,,App\Entity\Order,OneToMany
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
            val normalizedClassName = StringUtils.stripStart(className, "\\")

            // Resolve the class name to PhpClass
            val phpClass = PhpElementsUtil.getClassInterface(project, normalizedClassName)
                ?: mcpFail("Entity '$className' not found. Make sure the class exists.")

            // Get model fields using EntityHelper
            val fields = EntityHelper.getModelFields(phpClass)

            if (fields.isEmpty()) {
                mcpFail("Entity '$className' has no fields or is not a valid Doctrine entity with metadata.")
            }

            val csv = StringBuilder("name,column,type,relation,relationType\n")

            fields.forEach { field ->
                csv.append("${escapeCsv(field.name)},")
                csv.append("${escapeCsv(field.column ?: "")},")
                csv.append("${escapeCsv(field.typeName ?: "")},")
                csv.append("${escapeCsv(field.relation ?: "")},")
                csv.append("${escapeCsv(field.relationType ?: "")}\n")
            }

            csv.toString()
        }
    }

    private fun escapeCsv(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }
}
