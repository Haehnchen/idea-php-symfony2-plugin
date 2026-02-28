package fr.adrienbrault.idea.symfony2plugin.mcp.collector

import com.intellij.openapi.project.Project
import fr.adrienbrault.idea.symfony2plugin.mcp.McpCsvUtil
import fr.adrienbrault.idea.symfony2plugin.mcp.McpPathUtil
import com.jetbrains.php.PhpIndex
import fr.adrienbrault.idea.symfony2plugin.util.SymfonyCommandUtil

class SymfonyCommandCollector(private val project: Project) {
    fun collect(): String {
        val commands = SymfonyCommandUtil.getCommands(project)
        val phpIndex = PhpIndex.getInstance(project)

        val csv = StringBuilder("name,className,filePath\\n")

        commands.forEach { command ->
            val filePath = phpIndex.getClassesByFQN(command.fqn).firstOrNull()
                ?.containingFile
                ?.virtualFile
                ?.let { McpPathUtil.getRelativeProjectPath(project, it) }
                ?: ""

            csv.append("${McpCsvUtil.escape(command.name)},${McpCsvUtil.escape(command.fqn)},${McpCsvUtil.escape(filePath)}\\n")
        }

        return csv.toString()
    }
}
