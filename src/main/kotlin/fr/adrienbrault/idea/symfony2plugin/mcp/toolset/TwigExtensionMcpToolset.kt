@file:Suppress("FunctionName", "unused")

package fr.adrienbrault.idea.symfony2plugin.mcp.toolset

import com.intellij.mcpserver.McpToolset
import com.intellij.mcpserver.annotations.McpDescription
import com.intellij.mcpserver.annotations.McpTool
import com.intellij.mcpserver.project
import com.intellij.openapi.application.readAction
import com.jetbrains.php.PhpIndex
import com.jetbrains.php.lang.psi.elements.Function
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent
import fr.adrienbrault.idea.symfony2plugin.mcp.McpUtil
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigExtensionParser
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil
import kotlinx.coroutines.currentCoroutineContext

/**
 * MCP toolset for Twig extensions.
 * Provides unified access to all Twig extensions including filters, functions, tests, and tags.
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
class TwigExtensionMcpToolset : McpToolset {

    @McpTool
    @McpDescription("""
        Lists available Twig template extensions for code generation and template assistance.

        Use this to discover:
        - Filters for value transformation ({{ value|filter }})
        - Functions for template logic ({{ function() }})
        - Tests for conditionals ({% if var is test %})
        - Tags for control structures ({% tag %})

        Supports filtering by name (partial match) and type. Use to generate accurate Twig code,
        validate template syntax, or suggest available extensions to developers.

        Returns CSV: extension_type,name,className,methodName,parameters
        Example: filter,upper,\Twig\Extension\CoreExtension,upper,"value,encoding"
    """)
    suspend fun list_twig_extensions(
        @McpDescription("Partial name search (case-insensitive). Examples: 'date', 'url', 'format'")
        search: String? = null,

        @McpDescription("Include filters ({{ value|filter }}). Default: true")
        includeFilters: Boolean = true,

        @McpDescription("Include functions ({{ func() }}). Default: true")
        includeFunctions: Boolean = true,

        @McpDescription("Include tests ({% if value is test %}). Default: true")
        includeTests: Boolean = true,

        @McpDescription("Include tags ({% tag %}). Default: true")
        includeTags: Boolean = true
    ): String {
        val project = currentCoroutineContext().project

        if (!Symfony2ProjectComponent.isEnabled(project)) {
            throw IllegalStateException("Symfony plugin is not enabled for this project.")
        }

        McpUtil.checkToolEnabled(project, "list_twig_extensions")

        return readAction {
            val phpIndex = PhpIndex.getInstance(project)
            val csv = StringBuilder("extension_type,name,className,methodName,parameters\n")

            val searchLower = search?.lowercase()

            // Collect filters
            if (includeFilters) {
                val filters = TwigExtensionParser.getFilters(project)
                filters.forEach { (name, extension) ->
                    if (searchLower == null || name.lowercase().contains(searchLower)) {
                        val (className, methodName, parameters) = parseExtensionSignature(extension.signature, phpIndex)
                        val paramsStr = parameters?.joinToString(",") ?: ""
                        csv.append("filter,${escapeCsv(name)},${escapeCsv(className ?: "")},${escapeCsv(methodName ?: "")},${escapeCsv(paramsStr)}\n")
                    }
                }
            }

            // Collect functions
            if (includeFunctions) {
                val functions = TwigExtensionParser.getFunctions(project)
                functions.forEach { (name, extension) ->
                    if (searchLower == null || name.lowercase().contains(searchLower)) {
                        val (className, methodName, parameters) = parseExtensionSignature(extension.signature, phpIndex)
                        val paramsStr = parameters?.joinToString(",") ?: ""
                        csv.append("function,${escapeCsv(name)},${escapeCsv(className ?: "")},${escapeCsv(methodName ?: "")},${escapeCsv(paramsStr)}\n")
                    }
                }
            }

            // Collect tests
            if (includeTests) {
                val tests = TwigExtensionParser.getSimpleTest(project)
                tests.forEach { (name, extension) ->
                    if (searchLower == null || name.lowercase().contains(searchLower)) {
                        val (className, methodName, parameters) = parseExtensionSignature(extension.signature, phpIndex)
                        val paramsStr = parameters?.joinToString(",") ?: ""
                        csv.append("test,${escapeCsv(name)},${escapeCsv(className ?: "")},${escapeCsv(methodName ?: "")},${escapeCsv(paramsStr)}\n")
                    }
                }
            }

            // Collect tags
            if (includeTags) {
                val tags = TwigUtil.getNamedTokenParserTags(project)
                tags.forEach { name ->
                    if (searchLower == null || name.lowercase().contains(searchLower)) {
                        csv.append("tag,${escapeCsv(name)},,,\n")
                    }
                }
            }

            csv.toString()
        }
    }

    private fun parseExtensionSignature(
        signature: String?,
        phpIndex: PhpIndex
    ): Triple<String?, String?, List<String>?> {
        if (signature == null) {
            return Triple(null, null, null)
        }

        val classAndMethod = signature.removePrefix("#M#C").removePrefix("#M#M")
        val parts = classAndMethod.split(".")

        val className = if (parts.size >= 2) parts[0] else null
        val methodName = if (parts.size >= 2) parts[1] else null

        val parameters = try {
            val phpNamedElements = phpIndex.getBySignature(signature)
            val function = phpNamedElements.firstOrNull() as? Function
            function?.parameters
                ?.filter { !it.name.startsWith("_") }
                ?.map { it.name }
                ?.takeIf { it.isNotEmpty() }
        } catch (e: Exception) {
            null
        }

        return Triple(className, methodName, parameters)
    }

    private fun escapeCsv(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }
}
