package fr.adrienbrault.idea.symfony2plugin.profiler.renderer

import fr.adrienbrault.idea.symfony2plugin.profiler.consumer.SymfonyProfilerProfile
import fr.adrienbrault.idea.symfony2plugin.profiler.consumer.SymfonyProfilerFallbackCollector
import fr.adrienbrault.idea.symfony2plugin.profiler.consumer.SymfonyProfilerFallbackCollectorConsumer

private val FALLBACK_CONTROL_CHARACTERS = Regex("[\\u0000-\\u001F\\u007F]+")

/** Renders bounded and sanitized data for collectors without a specialized renderer. */
internal object SymfonyProfilerFallbackDetailRenderer {
    fun renderDetails(profile: SymfonyProfilerProfile, collectorName: String, page: Int): String =
        formatDetails(SymfonyProfilerFallbackCollectorConsumer.read(profile, collectorName), page)

    internal fun formatDetails(collector: SymfonyProfilerFallbackCollector, page: Int = 1): String {
        val detailPage = paginateProfilerDetailEntries(
            ProfilerTextRenderer.render(collector.data, initialIndent = 2),
            page,
        )

        return buildString {
            appendLine("Collector: ${collector.name.renderCollectorName()}")
            if (detailPage.isPaginated) {
                appendLine("Page: ${detailPage.number} of ${detailPage.total}")
            }
            appendLine()
            appendLine("## Data")
            appendLine()
            detailPage.entries.forEach(::appendLine)
        }.trimEnd()
    }
}

private fun String.renderCollectorName(): String =
    replace(FALLBACK_CONTROL_CHARACTERS, " ").replace("|", "\\|")
