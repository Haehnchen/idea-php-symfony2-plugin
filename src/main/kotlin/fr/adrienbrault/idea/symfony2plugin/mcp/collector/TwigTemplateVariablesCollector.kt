package fr.adrienbrault.idea.symfony2plugin.mcp.collector

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiFile
import com.jetbrains.php.lang.psi.elements.PhpClass
import com.jetbrains.php.lang.psi.resolve.types.PhpType
import fr.adrienbrault.idea.symfony2plugin.mcp.McpCsvUtil
import fr.adrienbrault.idea.symfony2plugin.mcp.McpGlobMatcher
import fr.adrienbrault.idea.symfony2plugin.mcp.McpPathUtil
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigTypeResolveUtil
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil
import fr.adrienbrault.idea.symfony2plugin.util.ProjectUtil
import fr.adrienbrault.idea.symfony2plugin.util.VfsExUtil

/**
 * Collects all Twig variables available in a template with their PHP types and
 * first-level Twig-accessible properties (get/is/has shortcut methods + public fields).
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
class TwigTemplateVariablesCollector(private val project: Project) {

    fun collect(templateInput: String? = null, fileGlob: String? = null): String {
        val psiManager = PsiManager.getInstance(project)
        val resolvedTemplates = linkedMapOf<String, PsiFile>()
        val normalizedFileGlob = fileGlob?.trim()?.takeIf { it.isNotBlank() }

        templateInput
            ?.split(",")
            ?.map { it.trim() }
            ?.filter { it.isNotBlank() }
            ?.forEach { templateName ->
                TwigUtil.getTemplateFiles(project, templateName)
                    .mapNotNull { psiManager.findFile(it) }
                    .firstOrNull()
                    ?.let { resolvedTemplates.putIfAbsent(templateName, it) }

                ProjectUtil.getProjectDir(project)
                    ?.findFileByRelativePath(templateName)
                    ?.let { virtualFile ->
                        val psiFile = psiManager.findFile(virtualFile) ?: return@let
                        TwigUtil.getTemplateNamesForFile(project, virtualFile).forEach { logicalTemplateName ->
                            resolvedTemplates.putIfAbsent(logicalTemplateName, psiFile)
                        }
                    }
            }

        if (normalizedFileGlob != null) {
            TwigUtil.getTemplateMap(project)
                .toSortedMap()
                .forEach { (templateName, virtualFiles) ->
                    val matchingFile = virtualFiles.firstOrNull { virtualFile ->
                        val relativePath = getDisplayPath(virtualFile)
                        relativePath.isNotBlank() && McpGlobMatcher.matches(relativePath, normalizedFileGlob)
                    } ?: return@forEach

                    psiManager.findFile(matchingFile)?.let { resolvedTemplates.putIfAbsent(templateName, it) }
                }
        }

        if (resolvedTemplates.isEmpty()) {
            return emptyCsv()
        }

        if (resolvedTemplates.size == 1) {
            return renderCsv(resolvedTemplates.values.first())
        }

        return buildString {
            resolvedTemplates.entries.forEachIndexed { index, (templateName, psiFile) ->
                val filePath = psiFile.virtualFile?.let { getDisplayPath(it) }.orEmpty()
                append("template: ").append(templateName)
                if (filePath.isNotBlank()) {
                    append(" => (file: ").append(filePath).append(")")
                }
                append('\n')
                append(renderCsv(psiFile))
                if (index < resolvedTemplates.size - 1) {
                    append('\n')
                }
            }
        }
    }

    private fun renderCsv(psiFile: PsiFile): String {
        val variables = TwigTypeResolveUtil.collectScopeVariables(psiFile)
        val csv = StringBuilder(emptyCsv())

        for ((varName, psiVariable) in variables.entries.sortedBy { it.key }) {
            val typeStr = psiVariable.types.joinToString("|")
            val propertiesStr = collectProperties(psiVariable.types).joinToString(",")

            csv.append("${McpCsvUtil.escape(varName)},")
                .append("${McpCsvUtil.escape(typeStr)},")
                .append("${McpCsvUtil.escape(propertiesStr)}\n")
        }

        return csv.toString()
    }

    private fun emptyCsv(): String {
        return "variable,type,properties\n"
    }

    private fun getDisplayPath(virtualFile: com.intellij.openapi.vfs.VirtualFile): String {
        return (
            VfsExUtil.getRelativeProjectPathStrict(project, virtualFile)
                ?: McpPathUtil.getRelativeProjectPath(project, virtualFile)
            ).replace('\\', '/')
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
