package fr.adrienbrault.idea.symfony2plugin.mcp

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.indexing.FileBasedIndex
import com.intellij.psi.PsiManager
import com.jetbrains.twig.TwigFile
import com.jetbrains.twig.TwigFileType
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.PhpTwigTemplateUsageStubIndex
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.TwigBlockIndexExtension
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.TwigComponentUsageStubIndex
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.TwigExtendsStubIndex
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.TwigIncludeStubIndex
import fr.adrienbrault.idea.symfony2plugin.templating.dict.TemplateInclude.TYPE as IncludeType
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil
import fr.adrienbrault.idea.symfony2plugin.util.ProjectUtil
import fr.adrienbrault.idea.symfony2plugin.util.UxUtil
import fr.adrienbrault.idea.symfony2plugin.util.VfsExUtil

/**
 * Collects Twig template usages across a project: which PHP controllers render a template,
 * and which other Twig templates include/extend/embed/import/use/form_theme it.
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
class TwigTemplateUsageCollector(private val project: Project) {

    /**
     * Collects usages of all templates matching the given input (case-insensitive).
     *
     * Two resolution strategies run in parallel and results are merged:
     * 1. Partial template-name match — all index keys that contain the input string.
     * 2. File-path resolution — the input is tried as a path relative to the project root;
     *    if a file is found, its logical template names (via TwigUtil.getTemplateNamesForFile)
     *    are added as exact matches.  This handles inputs like "templates/home/index.html.twig"
     *    whose template name is just "home/index.html.twig".
     *
     * Returns a CSV string with columns: template,controller,twig_include,twig_embed,twig_extends,twig_import,twig_use,twig_form_theme
     */
    fun collect(templateFilter: String): String {
        val index = FileBasedIndex.getInstance()
        val scope = GlobalSearchScope.allScope(project)
        val filterLower = templateFilter.lowercase()

        // Collect all matching template names using the internal template map
        val matchingTemplates = sortedSetOf<String>()

        // Strategy 1: partial template-name match using internal template map
        TwigUtil.getTemplateMap(project).keys.filterTo(matchingTemplates) { it.lowercase().contains(filterLower) }

        // Strategy 2: resolve input as a file path relative to the project root and add its
        // logical template names (e.g. "templates/home/index.html.twig" → "home/index.html.twig")
        ProjectUtil.getProjectDir(project)
            ?.findFileByRelativePath(templateFilter)
            ?.let { TwigUtil.getTemplateNamesForFile(project, it) }
            ?.forEach { matchingTemplates.add(it) }

        // Pre-scan {% use %} tags across all twig files for a reverse map:
        // usedTemplateName -> set of file paths that {% use %} it
        val useReverseMap = mutableMapOf<String, MutableSet<String>>()
        index.processValues(TwigBlockIndexExtension.KEY, "use", null, { file, templateSet ->
            val callerPath = VfsExUtil.getRelativeProjectPath(project, file) ?: return@processValues true
            for (usedTemplate in templateSet) {
                if (usedTemplate.lowercase().contains(filterLower)) {
                    matchingTemplates.add(usedTemplate)
                    useReverseMap.getOrPut(usedTemplate) { sortedSetOf() }.add(callerPath)
                }
            }
            true
        }, scope)

        val csv = StringBuilder("template,controller,twig_include,twig_embed,twig_extends,twig_import,twig_use,twig_form_theme,twig_component\n")

        for (templateName in matchingTemplates) {
            // PHP controller usages
            val controllers = sortedSetOf<String>()
            for (usage in index.getValues(PhpTwigTemplateUsageStubIndex.KEY, templateName, scope)) {
                for (fqn in usage.scopes) {
                    // Convert "App\Controller\HomeController.index" -> "App\Controller\HomeController::index"
                    val lastDot = fqn.lastIndexOf('.')
                    controllers.add(if (lastDot >= 0) fqn.substring(0, lastDot) + "::" + fqn.substring(lastDot + 1) else fqn)
                }
            }

            // Twig include / embed / import / form_theme
            val twigIncludes = sortedSetOf<String>()
            val twigEmbeds = sortedSetOf<String>()
            val twigImports = sortedSetOf<String>()
            val twigFormThemes = sortedSetOf<String>()

            index.processValues(TwigIncludeStubIndex.KEY, templateName, null, { file, includeObj ->
                val callerPath = VfsExUtil.getRelativeProjectPath(project, file) ?: return@processValues true
                when (includeObj.type) {
                    IncludeType.INCLUDE, IncludeType.INCLUDE_FUNCTION -> twigIncludes.add(callerPath)
                    IncludeType.EMBED -> twigEmbeds.add(callerPath)
                    IncludeType.IMPORT, IncludeType.FROM -> twigImports.add(callerPath)
                    IncludeType.FORM_THEME -> twigFormThemes.add(callerPath)
                    else -> {}
                }
                true
            }, scope)

            // Twig extends
            val twigExtends = sortedSetOf<String>()
            index.processValues(TwigExtendsStubIndex.KEY, templateName, null, { file, _ ->
                val callerPath = VfsExUtil.getRelativeProjectPath(project, file) ?: return@processValues true
                twigExtends.add(callerPath)
                true
            }, scope)

            val twigUses = useReverseMap[templateName] ?: emptySet<String>()

            // Twig component usages: template → file → component names → files using the component
            val twigComponents = sortedSetOf<String>()
            val componentScope = GlobalSearchScope.getScopeRestrictedByFileTypes(GlobalSearchScope.allScope(project), TwigFileType.INSTANCE)
            for (virtualFile in TwigUtil.getTemplateFiles(project, templateName)) {
                val psiFile = PsiManager.getInstance(project).findFile(virtualFile)
                if (psiFile !is TwigFile) continue

                for (componentName in UxUtil.getTemplateComponentNames(psiFile)) {
                    val normalized = TwigComponentUsageStubIndex.normalizeComponentName(componentName) ?: continue
                    for (usageFile in index.getContainingFiles(TwigComponentUsageStubIndex.KEY, normalized, componentScope)) {
                        if (usageFile == virtualFile) continue
                        val relativePath = VfsExUtil.getRelativeProjectPath(project, usageFile)
                        if (relativePath != null) {
                            twigComponents.add(relativePath)
                        }
                    }
                }
            }

            csv.append("${escapeCsv(templateName)},")
                .append("${escapeCsv(controllers.joinToString(";"))},")
                .append("${escapeCsv(twigIncludes.joinToString(";"))},")
                .append("${escapeCsv(twigEmbeds.joinToString(";"))},")
                .append("${escapeCsv(twigExtends.joinToString(";"))},")
                .append("${escapeCsv(twigImports.joinToString(";"))},")
                .append("${escapeCsv(twigUses.joinToString(";"))},")
                .append("${escapeCsv(twigFormThemes.joinToString(";"))},")
                .append("${escapeCsv(twigComponents.joinToString(";"))}\n")
        }

        return csv.toString()
    }

    private fun escapeCsv(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }
}
