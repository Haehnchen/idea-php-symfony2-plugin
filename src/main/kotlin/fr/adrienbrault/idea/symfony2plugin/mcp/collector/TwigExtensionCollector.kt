package fr.adrienbrault.idea.symfony2plugin.mcp.collector

import com.intellij.openapi.project.Project
import fr.adrienbrault.idea.symfony2plugin.mcp.McpCsvUtil
import fr.adrienbrault.idea.symfony2plugin.mcp.McpPathUtil
import com.jetbrains.php.PhpIndex
import com.jetbrains.php.lang.psi.elements.Function
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigExtensionParser
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil

class TwigExtensionCollector(private val project: Project) {
    fun collect(
        search: String? = null,
        includeFilters: Boolean = true,
        includeFunctions: Boolean = true,
        includeTests: Boolean = true,
        includeTags: Boolean = true
    ): String {
        val phpIndex = PhpIndex.getInstance(project)
        val csv = StringBuilder("extension_type,name,className,methodName,parameters\\n")

        val searchLower = search?.lowercase()

        if (includeFilters) {
            val filters = TwigExtensionParser.getFilters(project)
            filters.forEach { (name, extension) ->
                if (searchLower == null || name.lowercase().contains(searchLower)) {
                    val (className, methodName, parameters) = parseExtensionSignature(extension.signature, phpIndex)
                    val paramsStr = parameters?.joinToString(",") ?: ""
                    csv.append("filter,${McpCsvUtil.escape(name)},${McpCsvUtil.escape(className ?: "")},${McpCsvUtil.escape(methodName ?: "")},${McpCsvUtil.escape(paramsStr)}\\n")
                }
            }
        }

        if (includeFunctions) {
            val functions = TwigExtensionParser.getFunctions(project)
            functions.forEach { (name, extension) ->
                if (searchLower == null || name.lowercase().contains(searchLower)) {
                    val (className, methodName, parameters) = parseExtensionSignature(extension.signature, phpIndex)
                    val paramsStr = parameters?.joinToString(",") ?: ""
                    csv.append("function,${McpCsvUtil.escape(name)},${McpCsvUtil.escape(className ?: "")},${McpCsvUtil.escape(methodName ?: "")},${McpCsvUtil.escape(paramsStr)}\\n")
                }
            }
        }

        if (includeTests) {
            val tests = TwigExtensionParser.getSimpleTest(project)
            tests.forEach { (name, extension) ->
                if (searchLower == null || name.lowercase().contains(searchLower)) {
                    val (className, methodName, parameters) = parseExtensionSignature(extension.signature, phpIndex)
                    val paramsStr = parameters?.joinToString(",") ?: ""
                    csv.append("test,${McpCsvUtil.escape(name)},${McpCsvUtil.escape(className ?: "")},${McpCsvUtil.escape(methodName ?: "")},${McpCsvUtil.escape(paramsStr)}\\n")
                }
            }
        }

        if (includeTags) {
            val tags = TwigUtil.getNamedTokenParserTags(project)
            tags.forEach { name ->
                if (searchLower == null || name.lowercase().contains(searchLower)) {
                    csv.append("tag,${McpCsvUtil.escape(name)},,,\\n")
                }
            }
        }

        return csv.toString()
    }

    private fun parseExtensionSignature(signature: String?, phpIndex: PhpIndex): Triple<String?, String?, List<String>?> {
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
        } catch (_: Exception) {
            null
        }

        return Triple(className, methodName, parameters)
    }
}
