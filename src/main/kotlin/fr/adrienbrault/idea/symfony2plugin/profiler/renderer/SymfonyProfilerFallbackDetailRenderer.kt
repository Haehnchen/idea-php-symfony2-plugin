package fr.adrienbrault.idea.symfony2plugin.profiler.renderer

import fr.adrienbrault.idea.symfony2plugin.profiler.consumer.SymfonyProfilerProfile
import fr.adrienbrault.idea.symfony2plugin.profiler.consumer.SymfonyProfilerFallbackCollector
import fr.adrienbrault.idea.symfony2plugin.profiler.consumer.SymfonyProfilerFallbackCollectorConsumer
import kotlin.math.ceil

private const val FALLBACK_DETAIL_PAGE_SIZE = 100
private val FALLBACK_CONTROL_CHARACTERS = Regex("[\\u0000-\\u001F\\u007F]+")

/** Renders bounded and sanitized data for collectors without a specialized renderer. */
internal object SymfonyProfilerFallbackDetailRenderer {
    fun renderDetails(profile: SymfonyProfilerProfile, collectorName: String, page: Int): String =
        formatDetails(SymfonyProfilerFallbackCollectorConsumer.read(profile, collectorName), page)

    internal fun formatDetails(collector: SymfonyProfilerFallbackCollector, page: Int = 1): String {
        val lines = ProfilerTextRenderer.render(collector.data, initialIndent = 2)
        val totalPages = maxOf(1, ceil(lines.size.toDouble() / FALLBACK_DETAIL_PAGE_SIZE).toInt())
        val currentPage = page.coerceIn(1, totalPages)
        val pageLines = lines.drop((currentPage - 1) * FALLBACK_DETAIL_PAGE_SIZE).take(FALLBACK_DETAIL_PAGE_SIZE)

        return buildString {
            appendLine("Collector: ${collector.name.renderCollectorName()}")
            appendLine("Format: sanitized raw fallback")
            appendLine("Page: $currentPage of $totalPages")
            appendLine()
            appendLine("Data")
            appendLine()
            pageLines.forEach(::appendLine)
        }.trimEnd()
    }
}

private fun String.renderCollectorName(): String =
    replace(FALLBACK_CONTROL_CHARACTERS, " ").replace("|", "\\|")
