package fr.adrienbrault.idea.symfony2plugin.mcp.collector

import com.intellij.openapi.project.Project
import fr.adrienbrault.idea.symfony2plugin.mcp.McpCsvUtil
import fr.adrienbrault.idea.symfony2plugin.mcp.McpPathUtil
import fr.adrienbrault.idea.symfony2plugin.doctrine.EntityHelper

class DoctrineEntityCollector(private val project: Project) {
    fun collect(): String {
        val entities = EntityHelper.getModelClasses(project)

        val csv = StringBuilder("className,filePath\\n")

        entities.forEach { entity ->
            val phpClass = entity.phpClass
            val filePath = phpClass.containingFile?.virtualFile
                ?.let { McpPathUtil.getRelativeProjectPath(project, it) }
                ?: ""

            csv.append("${McpCsvUtil.escape(phpClass.fqn)},${McpCsvUtil.escape(filePath)}\\n")
        }

        return csv.toString()
    }
}
