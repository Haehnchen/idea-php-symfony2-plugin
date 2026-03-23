package fr.adrienbrault.idea.symfony2plugin.mcp.collector

import com.intellij.mcpserver.mcpFail
import fr.adrienbrault.idea.symfony2plugin.mcp.McpCsvUtil
import com.intellij.openapi.project.Project
import fr.adrienbrault.idea.symfony2plugin.doctrine.EntityHelper
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil
import org.apache.commons.lang3.StringUtils

class DoctrineEntityFieldsCollector(private val project: Project) {
    fun collect(className: String): String {
        val normalizedClassName = StringUtils.stripStart(className, "\\")

        val phpClass = PhpElementsUtil.getClassInterface(project, normalizedClassName)
            ?: mcpFail("Entity '$className' not found. Make sure the class exists.")

        val fields = EntityHelper.getModelFields(phpClass)

        if (fields.isEmpty()) {
            mcpFail("Entity '$className' has no fields or is not a valid Doctrine entity with metadata.")
        }

        return buildString {
            appendLine("name,column,type,relation,relationType,enumType,propertyType")
            fields.forEach { field ->
                appendLine(
                    "${McpCsvUtil.escape(field.name)}," +
                        "${McpCsvUtil.escape(field.column ?: "")}," +
                        "${McpCsvUtil.escape(field.typeName ?: "")}," +
                        "${McpCsvUtil.escape(field.relation ?: "")}," +
                        "${McpCsvUtil.escape(field.relationType ?: "")}," +
                        "${McpCsvUtil.escape(field.enumType ?: "")}," +
                        McpCsvUtil.escape(field.propertyTypes.joinToString("|"))
                )
            }
        }
    }
}
