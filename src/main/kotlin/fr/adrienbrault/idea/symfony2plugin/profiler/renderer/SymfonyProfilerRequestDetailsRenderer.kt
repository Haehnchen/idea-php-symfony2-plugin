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

    fun render(
        profile: SymfonyProfilerProfile,
        requestedHash: String,
        collector: String? = null,
        page: Int = 1,
    ): String = buildString {
        appendRequestOverview(profile, requestedHash)

        val availableCollectorNames = profile.collectorNames.toSet()
        if (collector != null) {
            val renderer = renderers.firstOrNull { it.name == collector }
            if (renderer != null) {
                appendSection(render(renderer.name) { renderer.renderDetails(profile, page.coerceAtLeast(1)) })
            } else {
                appendSection(render(collector) {
                    SymfonyProfilerFallbackDetailRenderer.renderDetails(profile, collector, page.coerceAtLeast(1))
                })
            }
            return@buildString
        }

        val availableRenderers = renderers.filter { it.name in availableCollectorNames }
        if (availableCollectorNames.isEmpty()) {
            appendSection("No profiler collectors are available for this request.")
            return@buildString
        }

        availableRenderers.forEach { renderer ->
            val overview = render(renderer.name) { renderer.renderOverview(profile) } ?: return@forEach
            appendSection(overview)
        }

        val specializedNames = profile.collectorNames.filter { name -> renderers.any { it.name == name } }
        val fallbackNames = profile.collectorNames.filterNot { name -> renderers.any { it.name == name } }
        appendSection(buildString {
            appendLine("## Available collectors")
            appendLine()
            if (specializedNames.isNotEmpty()) {
                appendLine("- Specialized: ${specializedNames.joinToString { plainText(it) }}")
            }
            if (fallbackNames.isNotEmpty()) {
                appendLine("- Additional collectors: ${fallbackNames.joinToString { plainText(it) }}")
            }
        }.trimEnd())
    }.trimEnd()

    private fun <T> render(collectorName: String, render: () -> T): T = try {
        render()
    } catch (exception: Exception) {
        throw ProfilerRendererException(collectorName, exception)
    }

    /** Appends one Markdown section with exactly one blank line after the preceding section. */
    private fun StringBuilder.appendSection(section: String) {
        if (isNotEmpty()) {
            if (!endsWith("\n")) {
                appendLine()
            }
            if (!endsWith("\n\n")) {
                appendLine()
            }
        }
        append(section)
    }

    private fun StringBuilder.appendRequestOverview(
        profile: SymfonyProfilerProfile,
        requestedHash: String,
    ) {
        append("Symfony Profiler Request - ${plainText(profile.token ?: requestedHash)}")
        profile.url?.let { append(" ${plainText(it)}") }
        profile.statusCode?.let { append(" $it") }
        profile.method?.let { append(" ${plainText(it)}") }
        appendLine()
    }
}

internal class ProfilerRendererException(
    val collectorName: String,
    cause: Exception,
) : RuntimeException(cause.message, cause)

private fun plainText(value: String): String = value
    .replace(CONTROL_CHARACTERS, " ")
    .replace("|", "\\|")
