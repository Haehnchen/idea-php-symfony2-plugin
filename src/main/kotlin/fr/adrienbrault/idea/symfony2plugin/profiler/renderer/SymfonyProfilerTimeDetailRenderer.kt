package fr.adrienbrault.idea.symfony2plugin.profiler.renderer

import fr.adrienbrault.idea.symfony2plugin.profiler.consumer.SymfonyProfilerProfile
import fr.adrienbrault.idea.symfony2plugin.profiler.consumer.SymfonyProfilerTime
import fr.adrienbrault.idea.symfony2plugin.profiler.consumer.SymfonyProfilerTimeConsumer
import fr.adrienbrault.idea.symfony2plugin.profiler.consumer.SymfonyProfilerTimeEvent
import java.math.BigDecimal
import java.math.RoundingMode

private const val OVERVIEW_EVENT_LIMIT = 3
private val CONTROL_CHARACTERS = Regex("[\\u0000-\\u001F\\u007F]+")

/** Renders Symfony Stopwatch events ordered by descending duration. */
internal object SymfonyProfilerTimeDetailRenderer : ProfilerDetailRenderer {
    override val name = "time"
    override val overviewWeight = 50

    override fun renderOverview(profile: SymfonyProfilerProfile): String =
        formatOverview(SymfonyProfilerTimeConsumer.read(profile))

    override fun renderDetails(profile: SymfonyProfilerProfile, page: Int): String =
        formatDetails(SymfonyProfilerTimeConsumer.read(profile))

    /** Renders the three slowest events in the compact performance overview. */
    internal fun formatOverview(time: SymfonyProfilerTime): String = formatSection(
        time,
        time.events.sortedByDescending { it.durationMs }.take(OVERVIEW_EVENT_LIMIT),
        "### Top $OVERVIEW_EVENT_LIMIT events by duration",
    )

    /** Renders all events in descending duration order without pagination. */
    internal fun formatDetails(time: SymfonyProfilerTime): String = formatSection(
        time,
        time.events.sortedByDescending { it.durationMs },
        "### Events ordered by duration",
    )

    private fun formatSection(
        time: SymfonyProfilerTime,
        events: List<SymfonyProfilerTimeEvent>,
        heading: String,
    ): String = buildString {
        appendLine("## Collector: time")
        appendLine()
        appendLine("- Total duration: ${formatMilliseconds(time.durationMs)} ms")
        appendLine("- Initialization time: ${formatMilliseconds(time.initializationTimeMs)} ms")
        appendLine("- Stopwatch installed: ${if (time.stopwatchInstalled) "yes" else "no"}")
        appendLine("- Events: ${time.events.size}")
        appendLine()
        appendLine(heading)

        if (events.isEmpty()) {
            appendLine()
            appendLine("No timing events recorded.")
            return@buildString
        }

        appendLine()
        appendLine("| Event | Category | Start (ms) | End (ms) | Duration (ms) | Memory (MiB) |")
        appendLine("| --- | --- | ---: | ---: | ---: | ---: |")
        events.forEach { event ->
            appendLine(
                "| ${plainText(event.name)} | ${plainText(event.category)} | " +
                    "${formatMilliseconds(event.startMs)} | ${formatMilliseconds(event.endMs)} | " +
                    "${formatMilliseconds(event.durationMs)} | ${formatMemoryMiB(event.memoryBytes)} |",
            )
        }
    }.trimEnd()
}

private fun formatMilliseconds(value: Double): String =
    BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).toPlainString()

private fun formatMemoryMiB(bytes: Long): String = BigDecimal.valueOf(bytes)
    .divide(BigDecimal.valueOf(1024L * 1024L), 2, RoundingMode.HALF_UP)
    .toPlainString()

private fun plainText(value: String): String = value
    .replace(CONTROL_CHARACTERS, " ")
    .replace("|", "\\|")
