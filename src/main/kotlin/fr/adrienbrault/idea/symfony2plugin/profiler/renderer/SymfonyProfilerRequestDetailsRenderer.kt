package fr.adrienbrault.idea.symfony2plugin.profiler.renderer

import fr.adrienbrault.idea.symfony2plugin.profiler.consumer.SymfonyProfilerProfile

private val CONTROL_CHARACTERS = Regex("[\\u0000-\\u001F\\u007F]+")

/** Composes the request header and all available collector renderers. */
internal object SymfonyProfilerRequestDetailsRenderer {
    private val renderers: List<ProfilerDetailRenderer> = listOf(
        SymfonyProfilerRequestDetailRenderer,
        SymfonyProfilerLoggerDetailRenderer,
        SymfonyProfilerEventsDetailRenderer,
        SymfonyProfilerTimeDetailRenderer,
        SymfonyProfilerTwigDetailRenderer,
        SymfonyProfilerDatabaseDetailRenderer,
    ).sortedByDescending { it.overviewWeight }

    val supportedCollectorNames: List<String> = renderers.map { it.name }

    fun supports(collector: String): Boolean = renderers.any { it.name == collector }

    fun isAvailable(profile: SymfonyProfilerProfile, collector: String): Boolean =
        collector in profile.collectorNames

    fun render(
        profile: SymfonyProfilerProfile,
        requestedHash: String,
        collector: String? = null,
        page: Int = 1,
    ): String = buildString {
        appendRequestOverview(profile, requestedHash)

        val availableCollectorNames = profile.collectorNames.toSet()
        if (collector != null) {
            val renderer = requireNotNull(renderers.firstOrNull { it.name == collector }) {
                "Unsupported profiler collector '$collector'."
            }
            appendLine()
            append(render(renderer) { renderer.renderDetails(profile, page.coerceAtLeast(1)) })
            return@buildString
        }

        val availableRenderers = renderers.filter { it.name in availableCollectorNames }
        if (availableRenderers.isEmpty()) {
            appendLine()
            appendLine("No supported profiler detail collectors are available for this request.")
            return@buildString
        }

        availableRenderers.forEach { renderer ->
            val overview = render(renderer) { renderer.renderOverview(profile) } ?: return@forEach
            appendLine()
            append(overview)
        }
    }.trimEnd()

    private fun <T> render(renderer: ProfilerDetailRenderer, render: () -> T): T = try {
        render()
    } catch (exception: Exception) {
        throw ProfilerRendererException(renderer.name, exception)
    }

    private fun StringBuilder.appendRequestOverview(
        profile: SymfonyProfilerProfile,
        requestedHash: String,
    ) {
        appendLine("# Symfony Profiler Request")
        appendLine()
        appendLine("- Token: ${plainText(profile.token ?: requestedHash)}")
        profile.method?.let { appendLine("- Method: ${plainText(it)}") }
        profile.url?.let { appendLine("- URL: ${plainText(it)}") }
        profile.statusCode?.let { appendLine("- Status: $it") }
    }
}

internal class ProfilerRendererException(
    val collectorName: String,
    cause: Exception,
) : RuntimeException(cause.message, cause)

private fun plainText(value: String): String = value
    .replace(CONTROL_CHARACTERS, " ")
    .replace("|", "\\|")
