package fr.adrienbrault.idea.symfony2plugin.mcp.collector

import com.intellij.openapi.project.Project
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import fr.adrienbrault.idea.symfony2plugin.mcp.McpCsvUtil
import fr.adrienbrault.idea.symfony2plugin.mcp.McpGlobMatcher
import fr.adrienbrault.idea.symfony2plugin.mcp.McpPathUtil
import fr.adrienbrault.idea.symfony2plugin.util.SymfonyCommandUtil

class SymfonyCommandCollector(private val project: Project) {
    fun collect(fileGlob: String? = null): String = buildString {
        appendLine("name,className,filePath,options,arguments")

        collectCommandRows(fileGlob).forEach { command ->
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

    private fun collectCommandRows(fileGlob: String?): List<CommandRow> {
        val normalizedFileGlob = fileGlob?.trim()?.takeIf { it.isNotBlank() }

        return SymfonyCommandUtil.getCommands(project).map { command ->
            val target = SymfonyCommandUtil.resolveCommandTarget(project, command)

            CommandRow(
                name = command.name,
                className = command.fqn,
                filePath = target
                    ?.containingFile
                    ?.virtualFile
                    ?.let { McpPathUtil.getRelativeProjectPath(project, it) }
                    ?: "",
                options = SymfonyCommandUtil.getCommandOptions(project, command),
                arguments = SymfonyCommandUtil.getCommandArguments(project, command),
            )
        }.filter { command ->
            normalizedFileGlob == null || McpGlobMatcher.matches(command.filePath, normalizedFileGlob)
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
