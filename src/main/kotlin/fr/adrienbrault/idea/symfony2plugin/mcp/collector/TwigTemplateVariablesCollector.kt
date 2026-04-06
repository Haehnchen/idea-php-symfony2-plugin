package fr.adrienbrault.idea.symfony2plugin.mcp.collector

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiManager
import com.jetbrains.php.lang.psi.elements.PhpClass
import com.jetbrains.php.lang.psi.resolve.types.PhpType
import fr.adrienbrault.idea.symfony2plugin.mcp.McpCsvUtil
import fr.adrienbrault.idea.symfony2plugin.mcp.McpGlobMatcher
import fr.adrienbrault.idea.symfony2plugin.mcp.McpPathUtil
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigTypeResolveUtil
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil

/**
 * Collects all Twig variables available in a template with their PHP types and
 * first-level Twig-accessible properties (get/is/has shortcut methods + public fields).
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
class TwigTemplateVariablesCollector(private val project: Project) {

    /**
     * Resolves the given logical template name and returns a CSV string:
     *   variable,type,properties
     */
    fun collect(templateInput: String, fileGlob: String? = null): String {
        val psiManager = PsiManager.getInstance(project)
        val psiFiles = linkedSetOf<com.intellij.psi.PsiFile>()
        val normalizedFileGlob = fileGlob?.trim()?.takeIf { it.isNotBlank() }

        TwigUtil.getTemplateFiles(project, templateInput)
            .filter { virtualFile ->
                normalizedFileGlob == null || McpGlobMatcher.matches(
                    McpPathUtil.getRelativeProjectPath(project, virtualFile),
                    normalizedFileGlob
                )
            }
            .mapNotNull { psiManager.findFile(it) }
            .forEach { psiFiles.add(it) }

        val psiFile = psiFiles.firstOrNull()
            ?: return "variable,type,properties\n"

        val variables = TwigTypeResolveUtil.collectScopeVariables(psiFile)

        val csv = StringBuilder("variable,type,properties\n")

        for ((varName, psiVariable) in variables.entries.sortedBy { it.key }) {
            val types = psiVariable.types

            val typeStr = types.joinToString("|")

            val properties = collectProperties(types)
            val propertiesStr = properties.joinToString(",")

            csv.append("${McpCsvUtil.escape(varName)},")
                .append("${McpCsvUtil.escape(typeStr)},")
                .append("${McpCsvUtil.escape(propertiesStr)}\n")
        }

        return csv.toString()
    }

    /**
     * Collects the first-level Twig-accessible property names from all PHP types.
     *
     * Delegates to [PhpTwigMethodLookupElement.getLookupString] so the output
     * exactly matches what IDE completion shows for `{{ variable.X }}`:
     * - get/is/has shortcut methods → shortcut name (e.g. getName → name)
     * - other public non-set, non-magic methods → method name as-is
     * - public non-static fields → field name
     *
     * Types ending with `[]` have the suffix stripped to resolve the base class.
     */
    private fun collectProperties(types: Set<String>): List<String> {
        val result = sortedSetOf<String>()

        for (type in types) {
            val baseType = type.trimEnd('[', ']')
            if (PhpType.isPrimitiveType(baseType)) continue
            val phpClass = PhpElementsUtil.getClassInterface(project, baseType) ?: continue
            collectFromClass(phpClass, result)
        }

        return result.toList()
    }

    private fun collectFromClass(phpClass: PhpClass, result: MutableSet<String>) {
        // Use central Twig method accessibility check
        for (method in phpClass.methods) {
            if (!TwigTypeResolveUtil.isTwigAccessibleMethod(method)) continue
            result.add(TwigTypeResolveUtil.getPropertyShortcutMethodName(method.name))
        }

        for (field in phpClass.fields) {
            if (field.modifier.isPublic && !field.modifier.isStatic) {
                result.add(field.name)
            }
        }
    }
}
