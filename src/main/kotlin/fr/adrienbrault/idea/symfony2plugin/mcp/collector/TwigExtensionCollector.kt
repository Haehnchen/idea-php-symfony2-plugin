package fr.adrienbrault.idea.symfony2plugin.mcp.collector

import com.intellij.openapi.project.Project
import fr.adrienbrault.idea.symfony2plugin.mcp.McpCsvUtil
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
    ): String = buildString {
        val phpIndex = PhpIndex.getInstance(project)
        appendLine("extension_type,name,className,methodName,parameters")

        val searchLower = search?.lowercase()

        if (includeFilters) {
            val filters = TwigExtensionParser.getFilters(project)
            filters.forEach { (name, extension) ->
                if (searchLower == null || searchLower in name.lowercase()) {
                    val (className, methodName, parameters) = parseExtensionSignature(extension.signature, phpIndex)
                    val paramsStr = parameters?.joinToString(",") ?: ""
                    appendLine("filter,${McpCsvUtil.escape(name)},${McpCsvUtil.escape(className ?: "")},${McpCsvUtil.escape(methodName ?: "")},${McpCsvUtil.escape(paramsStr)}")
                }
            }
        }

        if (includeFunctions) {
            val functions = TwigExtensionParser.getFunctions(project)
            functions.forEach { (name, extension) ->
                if (searchLower == null || searchLower in name.lowercase()) {
                    val (className, methodName, parameters) = parseExtensionSignature(extension.signature, phpIndex)
                    val paramsStr = parameters?.joinToString(",") ?: ""
                    appendLine("function,${McpCsvUtil.escape(name)},${McpCsvUtil.escape(className ?: "")},${McpCsvUtil.escape(methodName ?: "")},${McpCsvUtil.escape(paramsStr)}")
                }
            }
        }

        if (includeTests) {
            val tests = TwigExtensionParser.getSimpleTest(project)
            tests.forEach { (name, extension) ->
                if (searchLower == null || searchLower in name.lowercase()) {
                    val (className, methodName, parameters) = parseExtensionSignature(extension.signature, phpIndex)
                    val paramsStr = parameters?.joinToString(",") ?: ""
                    appendLine("test,${McpCsvUtil.escape(name)},${McpCsvUtil.escape(className ?: "")},${McpCsvUtil.escape(methodName ?: "")},${McpCsvUtil.escape(paramsStr)}")
                }
            }
        }

        if (includeTags) {
            val tags = TwigUtil.getNamedTokenParserTags(project)
            tags.forEach { name ->
                if (searchLower == null || searchLower in name.lowercase()) {
                    appendLine("tag,${McpCsvUtil.escape(name)},,,")
                }
            }
        }
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
