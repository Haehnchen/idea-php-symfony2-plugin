package fr.adrienbrault.idea.symfony2plugin.profiler.renderer

import fr.adrienbrault.idea.symfony2plugin.profiler.consumer.SymfonyProfilerProfile
import fr.adrienbrault.idea.symfony2plugin.profiler.consumer.SymfonyProfilerRequest
import fr.adrienbrault.idea.symfony2plugin.profiler.consumer.SymfonyProfilerRequestConsumer
import fr.adrienbrault.idea.symfony2plugin.profiler.consumer.SymfonyProfilerRequestSummary

private val CONTROL_CHARACTERS = Regex("[\\u0000-\\u001F\\u007F]+")

/** Renders sanitized request metadata and the generic request value tree. */
internal object SymfonyProfilerRequestDetailRenderer : ProfilerDetailRenderer {
    override val name = "request"
    override val overviewWeight = 100

    override fun renderOverview(profile: SymfonyProfilerProfile): String =
        formatOverview(SymfonyProfilerRequestConsumer.read(profile))

    override fun renderDetails(profile: SymfonyProfilerProfile, page: Int): String =
        formatDetails(SymfonyProfilerRequestConsumer.read(profile), page)

    /** Renders only stable request metadata in the compact profile overview. */
    internal fun formatOverview(request: SymfonyProfilerRequest): String = buildString {
        appendLine("## Collector: request")
        appendLine()
        appendLine("- Path: ${request.summary.path.renderOverviewValue()}")
        appendLine("- Route: ${request.summary.route.renderOverviewValue()}")
        appendLine("- Content type: ${request.summary.contentType.renderOverviewValue()}")
    }.trimEnd()

    /** Renders a bounded page of the sanitized generic request value tree. */
    internal fun formatDetails(request: SymfonyProfilerRequest, page: Int = 1): String {
        val detailPage = paginateProfilerDetailEntries(ProfilerTextRenderer.render(request.data), page)

        return buildString {
            appendLine("Collector: request")
            if (detailPage.isPaginated) {
                appendLine("Page: ${detailPage.number} of ${detailPage.total}")
            }
            appendLine()
            appendLine("Summary")
            appendLine()
            appendRequestSummary(request.summary)
            appendLine()
            appendLine("Data")
            appendLine()
            detailPage.entries.forEach(::appendLine)
        }.trimEnd()
    }

    private fun StringBuilder.appendRequestSummary(summary: SymfonyProfilerRequestSummary) {
        appendLine("Method: ${summary.method.renderSummaryValue()}")
        appendLine("Path: ${summary.path.renderSummaryValue()}")
        appendLine("Route: ${summary.route.renderSummaryValue()}")
        appendLine("Status: ${summary.statusCode?.toString() ?: "none"}")
        appendLine("Content type: ${summary.contentType.renderSummaryValue()}")
    }
}

private fun String?.renderSummaryValue(): String =
    this?.takeIf { it.isNotEmpty() }?.replace(CONTROL_CHARACTERS, " ") ?: "none"

private fun String?.renderOverviewValue(): String =
    this?.takeIf { it.isNotEmpty() }?.let(::plainText) ?: "none"

private fun plainText(value: String): String = value
    .replace(CONTROL_CHARACTERS, " ")
    .replace("|", "\\|")
