package fr.adrienbrault.idea.symfony2plugin.mcp.collector

import com.intellij.openapi.project.Project
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.jetbrains.php.PhpIndex
import fr.adrienbrault.idea.symfony2plugin.mcp.McpPathUtil
import fr.adrienbrault.idea.symfony2plugin.mcp.McpCsvUtil
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil
import fr.adrienbrault.idea.symfony2plugin.util.SymfonyCommandUtil

class SymfonyCommandCollector(private val project: Project) {
    fun collect(): String = buildString {
        appendLine("name,className,filePath,options,arguments")

        collectCommandRows().forEach { command ->
            appendLine(
                listOf(
                    command.name,
                    command.className,
                    command.filePath,
                    serializeOptions(command),
                    serializeArguments(command),
                ).joinToString(",") { McpCsvUtil.escape(it) }
            )
        }
    }

    private fun collectCommandRows(): List<CommandRow> {
        val phpIndex = PhpIndex.getInstance(project)

        return SymfonyCommandUtil.getCommands(project).map { command ->
            val phpClass = PhpElementsUtil.getClassInterface(project, command.fqn)

            CommandRow(
                name = command.name,
                className = command.fqn,
                filePath = phpIndex.getClassesByFQN(command.fqn).firstOrNull()
                    ?.containingFile
                    ?.virtualFile
                    ?.let { McpPathUtil.getRelativeProjectPath(project, it) }
                    ?: "",
                options = if (phpClass != null) SymfonyCommandUtil.getCommandOptions(phpClass) else emptyMap(),
                arguments = if (phpClass != null) SymfonyCommandUtil.getCommandArguments(phpClass) else emptyMap(),
            )
        }.sortedBy { it.name }
    }

    private fun serializeOptions(command: CommandRow): String =
        command.options.values
            .sortedBy { it.name() }
            .map { option ->
                JsonObject().apply {
                    addOptionalProperty("name", option.name())
                    addOptionalProperty("shortcut", option.shortcut())
                    addOptionalProperty("description", option.description())
                    addOptionalProperty("defaultValue", option.defaultValue())
                }
            }
            .takeIf { it.isNotEmpty() }
            ?.let { options ->
                JsonArray().apply {
                    options.forEach { add(it) }
                }.toString()
            }
            ?: ""

    private fun serializeArguments(command: CommandRow): String =
        command.arguments.values
            .sortedBy { it.name() }
            .map { argument ->
                JsonObject().apply {
                    addOptionalProperty("name", argument.name())
                    addOptionalProperty("description", argument.description())
                    addOptionalProperty("defaultValue", argument.defaultValue())
                }
            }
            .takeIf { it.isNotEmpty() }
            ?.let { arguments ->
                JsonArray().apply {
                    arguments.forEach { add(it) }
                }.toString()
            }
            ?: ""

    private fun JsonObject.addOptionalProperty(name: String, value: String?) {
        if (value != null) {
            addProperty(name, value)
        }
    }

    private data class CommandRow(
        val name: String,
        val className: String,
        val filePath: String,
        val options: Map<String, SymfonyCommandUtil.CommandOption>,
        val arguments: Map<String, SymfonyCommandUtil.CommandArgument>,
    )
}
