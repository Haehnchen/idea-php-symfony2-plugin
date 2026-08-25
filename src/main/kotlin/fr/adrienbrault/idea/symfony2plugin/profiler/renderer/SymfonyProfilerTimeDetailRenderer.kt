package fr.adrienbrault.idea.symfony2plugin.profiler.renderer

import fr.adrienbrault.idea.symfony2plugin.profiler.consumer.SymfonyProfilerProfile
import fr.adrienbrault.idea.symfony2plugin.profiler.consumer.SymfonyProfilerTime
import fr.adrienbrault.idea.symfony2plugin.profiler.consumer.SymfonyProfilerTimeConsumer
import fr.adrienbrault.idea.symfony2plugin.profiler.consumer.SymfonyProfilerTimeEvent
import java.math.BigDecimal
import java.math.RoundingMode

private const val OVERVIEW_EVENT_LIMIT = 3
private const val EVENT_THRESHOLD_MS = 1.0

/** Renders Symfony Stopwatch events ordered by descending duration. */
internal object SymfonyProfilerTimeDetailRenderer : ProfilerDetailRenderer {
    override val name = "time"
    override val overviewWeight = 80

    override fun renderOverview(profile: SymfonyProfilerProfile): String =
        formatOverview(SymfonyProfilerTimeConsumer.read(profile))

    override fun renderDetails(profile: SymfonyProfilerProfile, page: Int): String =
        formatDetails(SymfonyProfilerTimeConsumer.read(profile))

    /** Renders the three slowest events meeting Symfony's default timeline threshold. */
    internal fun formatOverview(time: SymfonyProfilerTime): String {
        val visibleEvents = visibleEvents(time)
        return formatSection(
            time,
            visibleEvents.take(OVERVIEW_EVENT_LIMIT),
            "### Top $OVERVIEW_EVENT_LIMIT events by duration",
        )
    }

    /** Renders all events meeting Symfony's default timeline threshold without pagination. */
    internal fun formatDetails(time: SymfonyProfilerTime): String = formatSection(
        time,
        visibleEvents(time),
        "### Events ordered by duration",
    )

    private fun visibleEvents(time: SymfonyProfilerTime): List<SymfonyProfilerTimeEvent> = time.events
        .filter { it.durationMs >= EVENT_THRESHOLD_MS }
        .sortedByDescending { it.durationMs }

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
        appendLine("- Threshold: ${formatMilliseconds(EVENT_THRESHOLD_MS)} ms")
        time.memory?.let { memory ->
            appendLine()
            appendLine("### Memory")
            appendLine()
            appendLine("- Memory peak: ${formatMemoryMiB(memory.peakBytes)} MiB")
            appendLine("- PHP memory limit: ${formatMemoryLimit(memory.limitBytes)}")
        }
        appendLine()
        appendLine(heading)

        if (events.isEmpty()) {
            appendLine()
            appendLine(
                if (time.events.isEmpty()) {
                    "No timing events recorded."
                } else {
                    "No timing events meet the ${formatMilliseconds(EVENT_THRESHOLD_MS)} ms threshold."
                },
            )
            return@buildString
        }

        appendLine()
        events.forEachIndexed { index, event ->
            appendLine(
                "${index + 1}. ${ProfilerTextRenderer.inlineCode(event.name)} — " +
                        "${formatMilliseconds(event.durationMs)} ms",
            )
            appendLine("   - Category: ${ProfilerTextRenderer.inlineCode(event.category)}")
            appendLine(
                "   - Timeline: ${formatMilliseconds(event.startMs)}–${formatMilliseconds(event.endMs)} ms",
            )
            appendLine("   - Memory: ${formatMemoryMiB(event.memoryBytes)} MiB")
        }
    }.trimEnd()
}

private fun formatMilliseconds(value: Double): String =
    BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).toPlainString()

private fun formatMemoryMiB(bytes: Long): String = BigDecimal.valueOf(bytes)
    .divide(BigDecimal.valueOf(1024L * 1024L), 2, RoundingMode.HALF_UP)
    .toPlainString()

private fun formatMemoryLimit(bytes: Long): String =
    if (bytes < 0) "unlimited" else "${formatMemoryMiB(bytes)} MiB"
