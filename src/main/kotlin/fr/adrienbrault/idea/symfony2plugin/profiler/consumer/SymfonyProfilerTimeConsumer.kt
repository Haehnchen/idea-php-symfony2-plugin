package fr.adrienbrault.idea.symfony2plugin.profiler.consumer

import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpArray
import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpBoolean
import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpFloat
import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpInteger
import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpObject
import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpStringKey
import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpUnserializeResult
import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpValue
import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.utf8StringOrNull
import java.math.BigDecimal
import java.math.RoundingMode

/** Reads the Symfony time collector and orders its visible events by descending duration. */
object SymfonyProfilerTimeConsumer {
    private const val TIME_COLLECTOR =
        "Symfony\\Component\\HttpKernel\\DataCollector\\TimeDataCollector"
    private const val STOPWATCH_EVENT = "Symfony\\Component\\Stopwatch\\StopwatchEvent"
    private const val STOPWATCH_PERIOD = "Symfony\\Component\\Stopwatch\\StopwatchPeriod"
    private const val SECTION_EVENT = "__section__"

    fun read(profile: SymfonyProfilerProfile): SymfonyProfilerTime {
        val collector = profile.collector("time")
            ?: error("Symfony profile does not contain the time collector")
        check(collector.className.utf8StringOrNull() == TIME_COLLECTOR) {
            "Symfony time collector has an unsupported class"
        }

        val data = collector["data"].resolve(profile.result) as? PhpArray
            ?: error("Symfony time collector does not contain its data array")
        val eventsData = data["events"].resolve(profile.result) as? PhpArray
            ?: error("Symfony time collector does not contain its events array")
        val parsedEvents = eventsData.entries.map { entry ->
            val name = (entry.key as? PhpStringKey)?.bytes?.utf8StringOrNull()
                ?: error("Symfony time event name is not UTF-8 text")
            readEvent(name, entry.value, profile.result)
        }
        val section = parsedEvents.firstOrNull { it.event.name == SECTION_EVENT }
        val startTime = data["start_time"].resolve(profile.result).finiteNumberOrZero("start time")

        return SymfonyProfilerTime(
            durationMs = roundToTwoDecimals(
                section?.let { it.originMs + it.rawDurationMs - startTime } ?: 0.0,
            ),
            initializationTimeMs = roundToTwoDecimals(section?.let { it.originMs - startTime } ?: 0.0),
            stopwatchInstalled = (data["stopwatch_installed"].resolve(profile.result) as? PhpBoolean)?.value
                ?: parsedEvents.isNotEmpty(),
            events = parsedEvents
                .asSequence()
                .filterNot { it.event.name == SECTION_EVENT }
                .map { it.event }
                .sortedByDescending { it.durationMs }
                .toList(),
        )
    }

    private fun readEvent(
        name: String,
        value: PhpValue,
        result: PhpUnserializeResult,
    ): ParsedEvent {
        val event = value.resolve(result) as? PhpObject
            ?: error("Symfony time event is not a StopwatchEvent object")
        check(event.className.utf8StringOrNull() == STOPWATCH_EVENT) {
            "Symfony time event has an unsupported class"
        }

        val periodsData = event["periods"].resolve(result) as? PhpArray
            ?: error("Symfony time event does not contain its periods array")
        val periods = periodsData.entries.map { entry ->
            val period = entry.value.resolve(result) as? PhpObject
                ?: error("Symfony time period is not a StopwatchPeriod object")
            check(period.className.utf8StringOrNull() == STOPWATCH_PERIOD) {
                "Symfony time period has an unsupported class"
            }

            StopwatchPeriod(
                startMs = period["start"].resolve(result).finiteNumberOrZero("period start"),
                endMs = period["end"].resolve(result).finiteNumberOrZero("period end"),
                memoryBytes = (period["memory"].resolve(result) as? PhpInteger)?.value?.coerceAtLeast(0) ?: 0,
            )
        }
        val rawDuration = periods.sumOf { it.endMs - it.startMs }
        val category = event["category"].resolve(result)?.utf8StringOrNull() ?: "default"

        return ParsedEvent(
            event = SymfonyProfilerTimeEvent(
                name = name,
                category = category,
                startMs = periods.firstOrNull()?.startMs ?: 0.0,
                endMs = periods.lastOrNull()?.endMs ?: 0.0,
                durationMs = roundToTwoDecimals(rawDuration),
                memoryBytes = periods.maxOfOrNull { it.memoryBytes } ?: 0,
            ),
            originMs = event["origin"].resolve(result).finiteNumberOrZero("event origin"),
            rawDurationMs = rawDuration,
        )
    }

    private fun PhpValue?.finiteNumberOrZero(field: String): Double {
        val value = when (this) {
            is PhpFloat -> value
            is PhpInteger -> value.toDouble()
            else -> 0.0
        }
        check(value.isFinite()) { "Symfony time collector contains a non-finite $field" }
        return value
    }

    private fun roundToTwoDecimals(value: Double): Double =
        BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).toDouble()

    private data class ParsedEvent(
        val event: SymfonyProfilerTimeEvent,
        val originMs: Double,
        val rawDurationMs: Double,
    )

    private data class StopwatchPeriod(
        val startMs: Double,
        val endMs: Double,
        val memoryBytes: Long,
    )
}

data class SymfonyProfilerTime(
    val durationMs: Double,
    val initializationTimeMs: Double,
    val stopwatchInstalled: Boolean,
    val events: List<SymfonyProfilerTimeEvent>,
)

data class SymfonyProfilerTimeEvent(
    val name: String,
    val category: String,
    val startMs: Double,
    val endMs: Double,
    val durationMs: Double,
    val memoryBytes: Long,
)
