package fr.adrienbrault.idea.symfony2plugin.profiler.renderer

import fr.adrienbrault.idea.symfony2plugin.profiler.consumer.SymfonyProfilerEventListener
import fr.adrienbrault.idea.symfony2plugin.profiler.consumer.SymfonyProfilerEvents
import fr.adrienbrault.idea.symfony2plugin.profiler.consumer.SymfonyProfilerEventsConsumer
import fr.adrienbrault.idea.symfony2plugin.profiler.consumer.SymfonyProfilerProfile

private val CONTROL_CHARACTERS = Regex("[\\u0000-\\u001F\\u007F]+")

/** Renders every dispatched event list with its called listeners in source order. */
internal object SymfonyProfilerEventsDetailRenderer : ProfilerDetailRenderer {
    override val name = "events"
    override val overviewWeight = 50

    override fun renderOverview(profile: SymfonyProfilerProfile): String =
        formatOverview(SymfonyProfilerEventsConsumer.read(profile))

    override fun renderDetails(profile: SymfonyProfilerProfile, page: Int): String =
        formatDetails(SymfonyProfilerEventsConsumer.read(profile))

    internal fun formatOverview(events: SymfonyProfilerEvents): String = buildString {
        appendSummary(events)
    }.trimEnd()

    internal fun formatDetails(events: SymfonyProfilerEvents): String = buildString {
        appendSummary(events)

        if (events.dispatchers.isEmpty()) {
            appendLine()
            appendLine("No event dispatchers recorded.")
            return@buildString
        }

        events.dispatchers.forEach { dispatcher ->
            appendLine()
            appendLine("### Dispatcher: ${plainText(dispatcher.name)}")

            if (dispatcher.events.isEmpty()) {
                appendLine()
                appendLine("No dispatched events recorded.")
                return@forEach
            }

            dispatcher.events.forEach { event ->
                appendLine()
                appendLine("#### ${plainText(event.name)} (${formatListenerCount(event.listeners.size)})")
                appendLine()
                appendLine("| Priority | Listener |")
                appendLine("| ---: | --- |")
                event.listeners.forEach { appendListener(it) }
            }
        }
    }.trimEnd()

    private fun StringBuilder.appendSummary(events: SymfonyProfilerEvents) {
        appendLine("## Collector: events")
        appendLine()
        appendLine("- Dispatchers: ${events.dispatchers.size}")
        appendLine("- Dispatched events: ${events.dispatchedEventCount}")
        appendLine("- Called listeners: ${events.calledListenerCount}")
    }

    private fun StringBuilder.appendListener(listener: SymfonyProfilerEventListener) {
        appendLine("| ${listener.priority?.toString() ?: "-"} | ${plainText(listener.listener)} |")
    }
}

private fun formatListenerCount(count: Int): String = "$count ${if (count == 1) "listener" else "listeners"}"

/** Keeps untrusted event and listener values on one table-safe line. */
private fun plainText(value: String): String = value
    .replace(CONTROL_CHARACTERS, " ")
    .replace("|", "\\|")
