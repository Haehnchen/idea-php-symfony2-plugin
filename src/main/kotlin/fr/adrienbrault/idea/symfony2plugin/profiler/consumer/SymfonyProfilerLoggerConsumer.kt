package fr.adrienbrault.idea.symfony2plugin.profiler.consumer

import fr.adrienbrault.idea.symfony2plugin.profiler.decoder.ProfilerArray
import fr.adrienbrault.idea.symfony2plugin.profiler.decoder.ProfilerBoolean
import fr.adrienbrault.idea.symfony2plugin.profiler.decoder.ProfilerEnum
import fr.adrienbrault.idea.symfony2plugin.profiler.decoder.ProfilerFloat
import fr.adrienbrault.idea.symfony2plugin.profiler.decoder.ProfilerInteger
import fr.adrienbrault.idea.symfony2plugin.profiler.decoder.ProfilerNull
import fr.adrienbrault.idea.symfony2plugin.profiler.decoder.ProfilerOpaque
import fr.adrienbrault.idea.symfony2plugin.profiler.decoder.ProfilerReference
import fr.adrienbrault.idea.symfony2plugin.profiler.decoder.ProfilerString
import fr.adrienbrault.idea.symfony2plugin.profiler.decoder.ProfilerValue
import fr.adrienbrault.idea.symfony2plugin.profiler.decoder.SymfonyVarDumperDataReader
import java.time.Instant
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException

/** Reads Symfony log messages without retaining their potentially sensitive context values. */
object SymfonyProfilerLoggerConsumer {
    private val LOGGER_COLLECTORS = setOf(
        "Symfony\\Component\\HttpKernel\\DataCollector\\LoggerDataCollector",
        "Symfony\\Bundle\\FrameworkBundle\\DataCollector\\LoggerDataCollector",
    )

    fun read(profile: SymfonyProfilerProfile): SymfonyProfilerLogger {
        val collector = profile.collector("logger")
            ?: error("Symfony profile does not contain the logger collector")
        check(collector.className.utf8StringOrNull() in LOGGER_COLLECTORS) {
            "Symfony logger collector has an unsupported class"
        }

        val rawData = collector["data"]
            ?: error("Symfony logger collector does not contain its data value")
        val data = SymfonyVarDumperDataReader(profile.result).read(rawData) as? ProfilerArray
            ?: error("Symfony logger collector data is not an array")
        val logs = data["logs"] as? ProfilerArray ?: return SymfonyProfilerLogger(emptyList())

        return SymfonyProfilerLogger(
            logs.entries.mapIndexedNotNull { index, entry ->
                val log = entry.value as? ProfilerArray ?: return@mapIndexedNotNull null
                readLog(log, index)
            }.sortedWith(
                compareByDescending<IndexedLog> { it.log.timestampEpochMillis ?: Long.MIN_VALUE }
                    .thenByDescending { it.sourceIndex },
            ).map { it.log },
        )
    }

    private fun readLog(log: ProfilerArray, sourceIndex: Int): IndexedLog {
        val priority = (log["priority"] as? ProfilerInteger)?.value
            ?.takeIf { it in Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong() }
            ?.toInt()
            ?: 0
        val priorityName = (log.text("priorityName") ?: log.text("priority_name"))?.uppercase()
        val type = log.text("type")?.lowercase()
        val scream = (log["scream"] as? ProfilerBoolean)?.value
        val timestamp = log.text("timestamp_rfc3339")
        val epochMillis = timestamp?.let(::parseTimestamp)
            ?: log.number("timestamp")?.takeIf { it.isFinite() }?.let { (it * 1000.0).toLong() }

        return IndexedLog(
            sourceIndex = sourceIndex,
            log = SymfonyProfilerLog(
                section = logSection(priority, priorityName, type, scream),
                timestamp = timestamp ?: epochMillis?.let { Instant.ofEpochMilli(it).toString() },
                timestampEpochMillis = epochMillis,
                priority = priority,
                channel = log.text("channel")?.takeIf { it.isNotBlank() },
                message = logMessage(log["message"]),
                occurrences = (log["errorCount"] as? ProfilerInteger)?.value?.coerceAtLeast(1) ?: 1,
            ),
        )
    }

    private fun logSection(
        priority: Int,
        priorityName: String?,
        type: String?,
        scream: Boolean?,
    ): SymfonyProfilerLogSection {
        if (type == "deprecation" || (priority <= 300 && scream == false)) {
            return SymfonyProfilerLogSection.DEPRECATION
        }
        if (type == "scream" || type == "silenced" || (priority <= 300 && scream == true)) {
            return SymfonyProfilerLogSection.SILENCED
        }

        return when (priorityName) {
            "EMERGENCY" -> SymfonyProfilerLogSection.EMERGENCY
            "ALERT" -> SymfonyProfilerLogSection.ALERT
            "CRITICAL" -> SymfonyProfilerLogSection.CRITICAL
            "ERROR" -> SymfonyProfilerLogSection.ERROR
            "WARNING" -> SymfonyProfilerLogSection.WARNING
            "NOTICE" -> SymfonyProfilerLogSection.NOTICE
            "INFO" -> SymfonyProfilerLogSection.INFO
            "DEBUG" -> SymfonyProfilerLogSection.DEBUG
            else -> sectionFromPriority(priority)
        }
    }

    private fun sectionFromPriority(priority: Int): SymfonyProfilerLogSection = when {
        priority >= 600 -> SymfonyProfilerLogSection.EMERGENCY
        priority >= 550 -> SymfonyProfilerLogSection.ALERT
        priority >= 500 -> SymfonyProfilerLogSection.CRITICAL
        priority >= 400 -> SymfonyProfilerLogSection.ERROR
        priority >= 300 -> SymfonyProfilerLogSection.WARNING
        priority >= 250 -> SymfonyProfilerLogSection.NOTICE
        priority >= 200 -> SymfonyProfilerLogSection.INFO
        else -> SymfonyProfilerLogSection.DEBUG
    }

    private fun parseTimestamp(value: String): Long? = try {
        OffsetDateTime.parse(value).toInstant().toEpochMilli()
    } catch (_: DateTimeParseException) {
        null
    }

    private fun logMessage(value: ProfilerValue?): String = when (value) {
        null, ProfilerNull -> "none"
        is ProfilerString -> value.utf8StringOrNull() ?: "[binary log message, ${value.bytes.size} bytes]"
        is ProfilerBoolean -> value.value.toString()
        is ProfilerInteger -> value.value.toString()
        is ProfilerFloat -> value.value.toString()
        is ProfilerArray -> "[structured log message]"
        is ProfilerEnum -> {
            val enumName = value.enumName.utf8StringOrNull() ?: "binary enum"
            val caseName = value.caseName.utf8StringOrNull() ?: "binary case"
            "$enumName::$caseName"
        }
        is ProfilerReference -> "[reference to position ${value.position}]"
        is ProfilerOpaque -> "[opaque ${value.kind}]"
    }

    private fun ProfilerArray.text(key: String): String? =
        (this[key] as? ProfilerString)?.utf8StringOrNull()

    private fun ProfilerArray.number(key: String): Double? = when (val value = this[key]) {
        is ProfilerFloat -> value.value
        is ProfilerInteger -> value.value.toDouble()
        else -> null
    }

    private data class IndexedLog(
        val sourceIndex: Int,
        val log: SymfonyProfilerLog,
    )
}

data class SymfonyProfilerLogger(
    /** Newest messages first. */
    val logs: List<SymfonyProfilerLog>,
)

data class SymfonyProfilerLog(
    val section: SymfonyProfilerLogSection,
    val timestamp: String?,
    val timestampEpochMillis: Long?,
    val priority: Int,
    val channel: String?,
    val message: String,
    val occurrences: Long,
)

/** Deprecations are intentionally first; remaining levels follow Monolog severity. */
enum class SymfonyProfilerLogSection(
    val title: String,
    val important: Boolean,
) {
    DEPRECATION("Deprecations", true),
    EMERGENCY("Emergency", true),
    ALERT("Alert", false),
    CRITICAL("Critical", false),
    ERROR("Errors", true),
    WARNING("Warnings", true),
    SILENCED("Silenced", false),
    NOTICE("Notice", false),
    INFO("Info", false),
    DEBUG("Debug", false),
}
