package fr.adrienbrault.idea.symfony2plugin.profiler.renderer

import fr.adrienbrault.idea.symfony2plugin.profiler.consumer.SymfonyProfilerDatabase
import fr.adrienbrault.idea.symfony2plugin.profiler.consumer.SymfonyProfilerDatabaseConsumer
import fr.adrienbrault.idea.symfony2plugin.profiler.consumer.SymfonyProfilerDatabaseQueryGroup
import fr.adrienbrault.idea.symfony2plugin.profiler.consumer.SymfonyProfilerProfile
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.ceil

private const val OVERVIEW_QUERY_GROUP_LIMIT = 3
private const val DETAIL_PAGE_SIZE = 50
private const val CALL_LIMIT = 5
private val INTERNAL_CALL_ROOT_NAMESPACES = setOf("doctrine", "symfony")
private val CONTROL_CHARACTERS = Regex("[\\u0000-\\u001F\\u007F]+")

/** Renders Doctrine query summaries and paginated query groups. */
internal object SymfonyProfilerDatabaseDetailRenderer : ProfilerDetailRenderer {
    override val name = "db"

    override fun renderOverview(profile: SymfonyProfilerProfile): String =
        formatOverview(SymfonyProfilerDatabaseConsumer.read(profile))

    override fun renderDetails(profile: SymfonyProfilerProfile, page: Int): String =
        formatDetails(SymfonyProfilerDatabaseConsumer.read(profile), page)

    /** Renders the compact database summary with its top query groups. */
    internal fun formatOverview(database: SymfonyProfilerDatabase): String = formatSection(
        database,
        database.queryGroups.take(OVERVIEW_QUERY_GROUP_LIMIT),
        "### Top $OVERVIEW_QUERY_GROUP_LIMIT query groups",
    )

    /** Renders one paginated page containing all database query groups. */
    internal fun formatDetails(database: SymfonyProfilerDatabase, page: Int = 1): String {
        val totalPages = maxOf(1, ceil(database.queryGroups.size.toDouble() / DETAIL_PAGE_SIZE).toInt())
        val currentPage = page.coerceIn(1, totalPages)
        val groups = database.queryGroups.drop((currentPage - 1) * DETAIL_PAGE_SIZE).take(DETAIL_PAGE_SIZE)
        return formatSection(database, groups, "### Query groups (page $currentPage of $totalPages)")
    }

    private fun formatSection(
        database: SymfonyProfilerDatabase,
        groups: List<SymfonyProfilerDatabaseQueryGroup>,
        heading: String,
    ): String = buildString {
        appendLine("## Collector: db")
        appendLine()
        appendLine("- Queries: ${database.queryCount}")
        appendLine("- Query time: ${formatMilliseconds(database.totalTimeMs)} ms")
        appendLine("- Duplicate query groups: ${database.duplicateQueryCount}")
        appendLine(
            "- Connections: " +
                if (database.connections.isEmpty()) "none" else database.connections.joinToString { plainText(it) },
        )
        appendLine()
        appendLine(heading)

        if (groups.isEmpty()) {
            appendLine()
            appendLine("No queries recorded.")
            return@buildString
        }

        appendLine()
        appendLine("| Occurrences | Time (ms) | Average time (ms) | Query | Calls |")
        appendLine("| ---: | ---: | ---: | --- | --- |")
        groups.forEach { group ->
            appendLine(
                "| ${group.count} | ${formatMilliseconds(group.totalTimeMs)} | " +
                    "${formatMilliseconds(group.averageTimeMs)} | ${plainText(group.sql)} | " +
                    "${plainText(formatCalls(group))} |",
            )
        }
    }.trimEnd()
}

/** Reduces all occurrence traces to unique non-framework calls in their original order. */
private fun formatCalls(group: SymfonyProfilerDatabaseQueryGroup): String = group.stackTraces
    .asSequence()
    .flatten()
    .mapNotNull { frame ->
        val className = frame.className?.trim()?.trimStart('\\')?.takeIf { it.isNotEmpty() }
            ?: return@mapNotNull null
        val rootNamespace = className.substringBefore('\\').lowercase()
        if (rootNamespace in INTERNAL_CALL_ROOT_NAMESPACES) {
            return@mapNotNull null
        }

        val method = frame.function?.trim()?.takeIf { it.isNotEmpty() } ?: return@mapNotNull null
        val shortClassName = className.substringAfterLast('\\').takeIf { it.isNotEmpty() }
            ?: return@mapNotNull null
        "$shortClassName:$method"
    }
    .distinct()
    .take(CALL_LIMIT)
    .joinToString()

private fun formatMilliseconds(value: Double): String =
    BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).toPlainString()

private fun plainText(value: String): String = value
    .replace(CONTROL_CHARACTERS, " ")
    .replace("|", "\\|")
