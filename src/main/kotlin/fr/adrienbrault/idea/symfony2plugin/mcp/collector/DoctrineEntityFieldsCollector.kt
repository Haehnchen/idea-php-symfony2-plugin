package fr.adrienbrault.idea.symfony2plugin.mcp.collector

import com.intellij.mcpserver.mcpFail
import fr.adrienbrault.idea.symfony2plugin.mcp.McpCsvUtil
import fr.adrienbrault.idea.symfony2plugin.mcp.McpPathUtil
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

        val csv = StringBuilder("name,column,type,relation,relationType,enumType,propertyType\\n")

        fields.forEach { field ->
            csv.append("${McpCsvUtil.escape(field.name)},")
            csv.append("${McpCsvUtil.escape(field.column ?: "")},")
            csv.append("${McpCsvUtil.escape(field.typeName ?: "")},")
            csv.append("${McpCsvUtil.escape(field.relation ?: "")},")
            csv.append("${McpCsvUtil.escape(field.relationType ?: "")},")
            csv.append("${McpCsvUtil.escape(field.enumType ?: "")},")
            csv.append("${McpCsvUtil.escape(field.propertyTypes.joinToString("|"))}\\n")
        }

        return csv.toString()
    }
}
