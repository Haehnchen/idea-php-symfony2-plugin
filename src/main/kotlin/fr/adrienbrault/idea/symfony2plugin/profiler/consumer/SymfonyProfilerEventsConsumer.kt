package fr.adrienbrault.idea.symfony2plugin.profiler.consumer

import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpStringKey
import fr.adrienbrault.idea.symfony2plugin.profiler.decoder.ProfilerArray
import fr.adrienbrault.idea.symfony2plugin.profiler.decoder.ProfilerInteger
import fr.adrienbrault.idea.symfony2plugin.profiler.decoder.ProfilerString
import fr.adrienbrault.idea.symfony2plugin.profiler.decoder.SymfonyVarDumperDataReader

/** Reads dispatched events while preserving dispatcher, event-list, and listener order. */
object SymfonyProfilerEventsConsumer {
    private const val EVENT_COLLECTOR =
        "Symfony\\Component\\HttpKernel\\DataCollector\\EventDataCollector"
    private const val DEFAULT_DISPATCHER = "event_dispatcher"

    fun read(profile: SymfonyProfilerProfile): SymfonyProfilerEvents {
        val collector = profile.collector("events")
            ?: error("Symfony profile does not contain the events collector")
        check(collector.className.utf8StringOrNull() == EVENT_COLLECTOR) {
            "Symfony events collector has an unsupported class"
        }

        val rawData = collector["data"]
            ?: error("Symfony events collector does not contain its data value")
        val data = SymfonyVarDumperDataReader(profile.result).read(rawData) as? ProfilerArray
            ?: error("Symfony events collector data is not an array")

        // Symfony <= 6.2 stores listener lists directly; Symfony >= 6.3 groups them by dispatcher.
        val dispatchers = if (data["called_listeners"] != null) {
            listOf(readDispatcher(DEFAULT_DISPATCHER, data))
        } else {
            data.entries.map { entry ->
                val name = (entry.key as? PhpStringKey)?.bytes?.utf8StringOrNull()
                    ?: error("Symfony event dispatcher name is not UTF-8 text")
                val dispatcher = entry.value as? ProfilerArray
                    ?: error("Symfony event dispatcher data is not an array")
                readDispatcher(name, dispatcher)
            }
        }

        return SymfonyProfilerEvents(dispatchers)
    }

    private fun readDispatcher(name: String, data: ProfilerArray): SymfonyProfilerEventDispatcher {
        val calledListeners = data["called_listeners"] as? ProfilerArray
            ?: return SymfonyProfilerEventDispatcher(name, emptyList())
        val events = mutableListOf<MutableEvent>()

        calledListeners.entries.forEach { entry ->
            val data = entry.value as? ProfilerArray
                ?: error("Symfony called event listener data is not an array")
            val eventName = data.text("event")
                ?: error("Symfony called event listener does not contain its event name")
            val listener = SymfonyProfilerEventListener(
                priority = (data["priority"] as? ProfilerInteger)?.value
                    ?.takeIf { it in Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong() }
                    ?.toInt(),
                listener = data.text("stub")
                    ?: data.text("pretty")?.let(::callableName)
                    ?: error("Symfony called event listener does not contain its callable"),
            )

            val event = events.lastOrNull()?.takeIf { it.name == eventName }
                ?: MutableEvent(eventName).also(events::add)
            event.listeners.add(listener)
        }

        return SymfonyProfilerEventDispatcher(
            name = name,
            events = events.map { SymfonyProfilerDispatchedEvent(it.name, it.listeners.toList()) },
        )
    }

    private fun callableName(value: String): String = if (value.endsWith(')')) value else "$value()"

    private fun ProfilerArray.text(key: String): String? =
        (this[key] as? ProfilerString)?.utf8StringOrNull()

    private data class MutableEvent(
        val name: String,
        val listeners: MutableList<SymfonyProfilerEventListener> = mutableListOf(),
    )
}

data class SymfonyProfilerEvents(
    val dispatchers: List<SymfonyProfilerEventDispatcher>,
) {
    val dispatchedEventCount: Int
        get() = dispatchers.sumOf { it.events.size }

    val calledListenerCount: Int
        get() = dispatchers.sumOf { dispatcher -> dispatcher.events.sumOf { it.listeners.size } }
}

data class SymfonyProfilerEventDispatcher(
    val name: String,
    val events: List<SymfonyProfilerDispatchedEvent>,
)

data class SymfonyProfilerDispatchedEvent(
    val name: String,
    val listeners: List<SymfonyProfilerEventListener>,
)

data class SymfonyProfilerEventListener(
    val priority: Int?,
    val listener: String,
)
