package fr.adrienbrault.idea.symfony2plugin.profiler.renderer

import fr.adrienbrault.idea.symfony2plugin.profiler.consumer.SymfonyProfilerProfile
import fr.adrienbrault.idea.symfony2plugin.profiler.consumer.SymfonyProfilerTwig
import fr.adrienbrault.idea.symfony2plugin.profiler.consumer.SymfonyProfilerTwigConsumer
import fr.adrienbrault.idea.symfony2plugin.profiler.consumer.SymfonyProfilerTwigProfile
import fr.adrienbrault.idea.symfony2plugin.profiler.consumer.SymfonyProfilerTwigTemplate
import java.math.BigDecimal
import java.math.RoundingMode

private const val OVERVIEW_TEMPLATE_LIMIT = 5
private val CONTROL_CHARACTERS = Regex("[\\u0000-\\u001F\\u007F]+")

/** Renders Twig metrics, unique templates, and the complete rendering call tree. */
internal object SymfonyProfilerTwigDetailRenderer : ProfilerDetailRenderer {
    override val name = "twig"
    override val overviewWeight = 25

    override fun renderOverview(profile: SymfonyProfilerProfile): String =
        formatOverview(SymfonyProfilerTwigConsumer.read(profile))

    override fun renderDetails(profile: SymfonyProfilerProfile, page: Int): String =
        formatDetails(SymfonyProfilerTwigConsumer.read(profile))

    /** Renders the first five unique templates in their initial rendering order. */
    internal fun formatOverview(twig: SymfonyProfilerTwig): String = formatSection(
        twig,
        twig.renderedTemplates.take(OVERVIEW_TEMPLATE_LIMIT),
        "### First $OVERVIEW_TEMPLATE_LIMIT rendered templates",
        includeCallTree = false,
    )

    /** Renders all unique templates and the complete rendering call tree without pagination. */
    internal fun formatDetails(twig: SymfonyProfilerTwig): String = formatSection(
        twig,
        twig.renderedTemplates,
        "### Rendered templates",
        includeCallTree = true,
    )

    private fun formatSection(
        twig: SymfonyProfilerTwig,
        templates: List<SymfonyProfilerTwigTemplate>,
        heading: String,
        includeCallTree: Boolean,
    ): String = buildString {
        appendLine("## Collector: twig")
        appendLine()
        appendLine("- Render time: ${formatMilliseconds(twig.renderTimeMs)} ms")
        appendLine("- Template calls: ${twig.templateCallCount}")
        appendLine("- Block calls: ${twig.blockCallCount}")
        appendLine("- Macro calls: ${twig.macroCallCount}")
        appendLine("- Unique templates: ${twig.renderedTemplates.size}")
        appendLine()
        appendLine(heading)

        if (templates.isEmpty()) {
            appendLine()
            appendLine("No Twig templates were rendered.")
        } else {
            appendLine()
            appendLine("| Template | Path | Render count |")
            appendLine("| --- | --- | ---: |")
            templates.forEach { template ->
                appendLine(
                    "| ${plainText(template.name)} | ${plainText(template.path ?: "none")} | " +
                        "${template.renderCount} |",
                )
            }
        }

        if (includeCallTree) {
            appendLine()
            appendLine("### Rendering call tree")
            appendLine()
            appendTwigProfile(twig.root, twig.renderTimeMs)
        }
    }.trimEnd()

    private fun StringBuilder.appendTwigProfile(
        profile: SymfonyProfilerTwigProfile,
        rootDurationMs: Double,
        prefix: String = "",
        hasFollowingSibling: Boolean = false,
    ) {
        val label = when (profile.type) {
            "ROOT" -> treeText(profile.name)
            "template" -> "$prefix└ ${treeText(profile.template)}"
            else -> "$prefix└ ${treeText(profile.template)}::${treeText(profile.type)}(${treeText(profile.name)})"
        }
        val timing = if (profile.durationMs >= 1.0) {
            val percentage = if (rootDurationMs > 0.0) profile.durationMs / rootDurationMs * 100.0 else 0.0
            " ${formatMilliseconds(profile.durationMs)}ms/${formatPercentage(percentage)}%"
        } else {
            ""
        }
        appendLine("    $label$timing")

        val childPrefix = if (profile.type == "ROOT") {
            prefix
        } else {
            prefix + if (hasFollowingSibling) "│ " else "  "
        }
        profile.children.forEachIndexed { index, child ->
            appendTwigProfile(
                child,
                rootDurationMs,
                childPrefix,
                hasFollowingSibling = index + 1 < profile.children.size,
            )
        }
    }
}

private fun formatMilliseconds(value: Double): String =
    BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).toPlainString()

private fun formatPercentage(value: Double): String =
    BigDecimal.valueOf(value).setScale(0, RoundingMode.HALF_UP).toPlainString()

private fun plainText(value: String): String = value
    .replace(CONTROL_CHARACTERS, " ")
    .replace("|", "\\|")

private fun treeText(value: String): String = value.replace(CONTROL_CHARACTERS, " ")
