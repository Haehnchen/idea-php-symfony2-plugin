package fr.adrienbrault.idea.symfony2plugin.mcp.collector

import com.intellij.openapi.project.Project
import fr.adrienbrault.idea.symfony2plugin.mcp.McpCsvUtil
import fr.adrienbrault.idea.symfony2plugin.mcp.McpPathUtil
import fr.adrienbrault.idea.symfony2plugin.form.util.FormUtil

class SymfonyFormTypeCollector(private val project: Project) {
    fun collect(): String {
        val collector = FormUtil.FormTypeCollector(project).collect()
        val formTypes = collector.formTypesMap

        val csv = StringBuilder("name,className,filePath\\n")

        formTypes.forEach { (name, formTypeClass) ->
            val phpClass = formTypeClass.getPhpClass(project)
            val filePath = phpClass?.containingFile?.virtualFile
                ?.let { McpPathUtil.getRelativeProjectPath(project, it) }
                ?: ""

            csv.append("${McpCsvUtil.escape(name)},${McpCsvUtil.escape(formTypeClass.phpClassName)},${McpCsvUtil.escape(filePath)}\\n")
        }

        return csv.toString()
    }
}
