package fr.adrienbrault.idea.symfony2plugin.profiler.renderer

import fr.adrienbrault.idea.symfony2plugin.profiler.consumer.SymfonyProfilerLog
import fr.adrienbrault.idea.symfony2plugin.profiler.consumer.SymfonyProfilerLogger
import fr.adrienbrault.idea.symfony2plugin.profiler.consumer.SymfonyProfilerLoggerConsumer
import fr.adrienbrault.idea.symfony2plugin.profiler.consumer.SymfonyProfilerLogSection
import fr.adrienbrault.idea.symfony2plugin.profiler.consumer.SymfonyProfilerProfile
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

private const val OVERVIEW_LOG_LIMIT = 5
private const val DETAIL_LOG_LIMIT = 25
private val CONTROL_CHARACTERS = Regex("[\\u0000-\\u001F\\u007F]+")
private val LOG_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS")

/** Renders prioritized and bounded log sections without retaining log context. */
internal object SymfonyProfilerLoggerDetailRenderer : ProfilerDetailRenderer {
    override val name = "logger"
    override val overviewWeight = 75

    override fun renderOverview(profile: SymfonyProfilerProfile): String =
        formatOverview(SymfonyProfilerLoggerConsumer.read(profile))

    override fun renderDetails(profile: SymfonyProfilerProfile, page: Int): String =
        formatDetails(SymfonyProfilerLoggerConsumer.read(profile))

    /** Renders at most five high-priority log entries across all important sections. */
    internal fun formatOverview(logger: SymfonyProfilerLogger): String = buildString {
        val importantLogs = SymfonyProfilerLogSection.entries
            .asSequence()
            .filter { it.important }
            .flatMap { section -> logger.logs.asSequence().filter { it.section == section } }
            .toList()

        appendLine("## Collector: logger")
        appendLine()
        appendLine("- Log entries: ${logger.logs.size}")
        appendLine()
        appendLine("### Important logs (${formatEntryCount(importantLogs.size)})")

        if (importantLogs.isEmpty()) {
            appendLine()
            appendLine("No important log messages recorded.")
            return@buildString
        }

        appendLine()
        appendLogTable(importantLogs.take(OVERVIEW_LOG_LIMIT), includeLevel = true)
        if (importantLogs.size > OVERVIEW_LOG_LIMIT) {
            appendLine()
            appendLine(
                "_Truncated: showing $OVERVIEW_LOG_LIMIT of ${importantLogs.size} important log entries._",
            )
        }
    }.trimEnd()

    /** Renders up to 25 newest entries for every non-empty log section. */
    internal fun formatDetails(logger: SymfonyProfilerLogger): String = buildString {
        appendLine("## Collector: logger")
        appendLine()
        appendLine("- Log entries: ${logger.logs.size}")

        if (logger.logs.isEmpty()) {
            appendLine()
            appendLine("No log messages available.")
            return@buildString
        }

        SymfonyProfilerLogSection.entries.forEach { section ->
            val logs = logger.logs.filter { it.section == section }
            if (logs.isEmpty()) {
                return@forEach
            }

            appendLine()
            appendLine("### ${section.title} (${formatEntryCount(logs.size)})")
            appendLine()
            appendLogTable(logs.take(DETAIL_LOG_LIMIT), includeLevel = false)
            if (logs.size > DETAIL_LOG_LIMIT) {
                appendLine()
                appendLine(
                    "_Truncated: showing the newest $DETAIL_LOG_LIMIT of ${logs.size} entries._",
                )
            }
        }
    }.trimEnd()

    private fun StringBuilder.appendLogTable(logs: List<SymfonyProfilerLog>, includeLevel: Boolean) {
        if (includeLevel) {
            appendLine("| Level | Time | Channel | Occurrences | Message |")
            appendLine("| --- | --- | --- | ---: | --- |")
        } else {
            appendLine("| Time | Channel | Occurrences | Message |")
            appendLine("| --- | --- | ---: | --- |")
        }
        logs.forEach { log ->
            val prefix = if (includeLevel) "| ${plainText(log.section.title)} " else ""
            appendLine(
                "$prefix| ${plainText(formatLogTimestamp(log.timestamp))} | " +
                    "${plainText(log.channel ?: "none")} | ${log.occurrences} | ${plainText(log.message)} |",
            )
        }
    }
}

private fun formatEntryCount(count: Int): String = "$count ${if (count == 1) "entry" else "entries"}"

private fun formatLogTimestamp(value: String?): String {
    if (value == null) {
        return "unknown"
    }

    return try {
        OffsetDateTime.parse(value).format(LOG_TIME_FORMATTER)
    } catch (_: DateTimeParseException) {
        value
    }
}

/** Keeps untrusted log values on one table-safe line. */
private fun plainText(value: String): String = value
    .replace(CONTROL_CHARACTERS, " ")
    .replace("|", "\\|")
