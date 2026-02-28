package fr.adrienbrault.idea.symfony2plugin.mcp

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil
import fr.adrienbrault.idea.symfony2plugin.util.UxUtil
import fr.adrienbrault.idea.symfony2plugin.util.VfsExUtil

/**
 * Collects Twig components for MCP usage.
 *
 * Returns component metadata and ready-to-use Twig syntax snippets for AI agents.
 */
class TwigComponentCollector(private val project: Project) {

    fun collect(search: String?): String {
        val searchLower = search?.trim()?.takeIf { it.isNotBlank() }?.lowercase()

        val componentNames = UxUtil.getAllComponentNames(project)
            .asSequence()
            .map { it.name() }
            .filter { it.isNotBlank() }
            .filter { name -> searchLower == null || name.lowercase().contains(searchLower) }
            .distinct()
            .sortedWith(String.CASE_INSENSITIVE_ORDER)
            .toList()

        val csv = StringBuilder(
            "component_name,template_relative_path,component_tag,twig_component_syntax,component_print_block_syntax,twig_tag_composition_syntax,props,template_blocks\n"
        )

        for (componentName in componentNames) {
            val row = buildRow(componentName)
            csv.append(escapeCsv(row.componentName)).append(',')
                .append(escapeCsv(row.templateRelativePath)).append(',')
                .append(escapeCsv(row.componentTag)).append(',')
                .append(escapeCsv(row.twigComponentSyntax)).append(',')
                .append(escapeCsv(row.componentPrintBlockSyntax)).append(',')
                .append(escapeCsv(row.twigTagCompositionSyntax)).append(',')
                .append(escapeCsv(row.props)).append(',')
                .append(escapeCsv(row.templateBlocks)).append('\n')
        }

        return csv.toString()
    }

    private fun buildRow(componentName: String): ComponentRow {
        val templateRelativePaths = sortedSetOf(String.CASE_INSENSITIVE_ORDER)
        val templateVirtualFiles = linkedSetOf<VirtualFile>()

        UxUtil.getComponentTemplates(project, componentName)
            .mapNotNull { it.virtualFile }
            .forEach { virtualFile ->
                templateVirtualFiles.add(virtualFile)
                VfsExUtil.getRelativeProjectPath(project, virtualFile)?.let {
                    templateRelativePaths.add(it)
                }
            }

        val blocks = sortedSetOf(String.CASE_INSENSITIVE_ORDER)
        TwigUtil.getBlockNamesForFiles(project, templateVirtualFiles)
            .values
            .flatten()
            .filter { it.isNotBlank() }
            .forEach { blocks.add(it) }

        val props = sortedSetOf(String.CASE_INSENSITIVE_ORDER)

        UxUtil.getTwigComponentPhpClasses(project, componentName).forEach { phpClass ->
            UxUtil.visitComponentVariables(phpClass) { pair ->
                val name = pair.first
                if (!name.isNullOrBlank()) {
                    props.add(name)
                }
            }
        }

        UxUtil.visitComponentTemplateProps(project, componentName) { pair ->
            val name = pair.first
            if (!name.isNullOrBlank()) {
                props.add(name)
            }
        }

        val componentLiteral = escapeTwigSingleQuotedString(componentName)

        val printBlockSyntax = blocks.joinToString(";") { "{{ block('${escapeTwigSingleQuotedString(it)}') }}" }

        val tagBlocks = blocks.joinToString(separator = "") {
            "{% block ${escapeTwigBlockName(it)} %}{% endblock %}"
        }

        val twigTagCompositionSyntax = "{% component '$componentLiteral' %}$tagBlocks{% endcomponent %}"

        return ComponentRow(
            componentName = componentName,
            componentTag = "<twig:$componentName></twig:$componentName>",
            templateRelativePath = templateRelativePaths.joinToString(";"),
            twigComponentSyntax = "{{ component('$componentLiteral') }}",
            componentPrintBlockSyntax = printBlockSyntax,
            twigTagCompositionSyntax = twigTagCompositionSyntax,
            props = props.joinToString(";"),
            templateBlocks = blocks.joinToString(";")
        )
    }

    private fun escapeCsv(value: String): String {
        return if (value.contains(',') || value.contains('"') || value.contains('\n')) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }

    private fun escapeTwigSingleQuotedString(value: String): String {
        return value.replace("'", "\\'")
    }

    private fun escapeTwigBlockName(value: String): String {
        return value.replace("'", "")
    }

    private data class ComponentRow(
        val componentName: String,
        val componentTag: String,
        val templateRelativePath: String,
        val twigComponentSyntax: String,
        val componentPrintBlockSyntax: String,
        val twigTagCompositionSyntax: String,
        val props: String,
        val templateBlocks: String
    )
}
