package fr.adrienbrault.idea.symfony2plugin.profiler.consumer

import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpArray
import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpFloat
import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpInteger
import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpStringKey
import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpValue
import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.utf8StringOrNull
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Reads Doctrine query information from a validated Symfony profile.
 * Formatting stays outside so other profiler collectors can contribute their own sections.
 */
object SymfonyProfilerDatabaseConsumer {
    private const val DOCTRINE_COLLECTOR =
        "Doctrine\\Bundle\\DoctrineBundle\\DataCollector\\DoctrineDataCollector"

    fun read(profile: SymfonyProfilerProfile): SymfonyProfilerDatabase {
        val collectorData = findDoctrineCollectorData(profile)
        val connections = readConnections(collectorData, profile)
        val queries = readQueries(collectorData, profile)
        val groupedQueries = groupQueries(queries)
        val totalTimeMs = roundToTwoDecimals(queries.sumOf { it.executionSeconds } * 1000)

        return SymfonyProfilerDatabase(
            queryCount = queries.size,
            totalTimeMs = totalTimeMs,
            connections = connections,
            duplicateQueryCount = groupedQueries.count { it.count > 1 },
            queryGroups = groupedQueries,
        )
    }

    private fun findDoctrineCollectorData(profile: SymfonyProfilerProfile): PhpArray {
        val collector = profile.collector("db")
            ?: error("Symfony profile does not contain the db collector")

        check(collector.className.utf8StringOrNull() == DOCTRINE_COLLECTOR) {
            "Symfony db collector is not a DoctrineDataCollector"
        }

        return collector["data"].resolve(profile.result) as? PhpArray
            ?: error("Doctrine collector does not contain its data array")
    }

    private fun readConnections(data: PhpArray, profile: SymfonyProfilerProfile): List<String> {
        val connections = data["connections"].resolve(profile.result) as? PhpArray ?: return emptyList()
        return connections.entries.mapNotNull { entry ->
            (entry.key as? PhpStringKey)?.bytes?.utf8StringOrNull()
        }
    }

    private fun readQueries(data: PhpArray, profile: SymfonyProfilerProfile): List<Query> {
        val result = profile.result
        val connections = data["queries"].resolve(result) as? PhpArray ?: return emptyList()
        return buildList {
            connections.entries.forEach { connection ->
                val queries = connection.value.resolve(result) as? PhpArray
                    ?: error("Doctrine connection queries must be a PHP array")
                queries.entries.forEach { entry ->
                    val query = entry.value.resolve(result) as? PhpArray
                        ?: error("Doctrine query must be a PHP array")
                    add(
                        Query(
                            sql = query["sql"].resolve(result)?.utf8StringOrNull() ?: "",
                            executionSeconds = query["executionMS"].resolve(result).numberOrZero(),
                            stackTrace = readStackTrace(query, profile),
                        ),
                    )
                }
            }
        }
    }

    private fun readStackTrace(
        query: PhpArray,
        profile: SymfonyProfilerProfile,
    ): List<SymfonyProfilerDatabaseStackFrame> {
        val result = profile.result
        val backtrace = query["backtrace"].resolve(result) as? PhpArray ?: return emptyList()

        // Frame arguments may contain application data and are deliberately not retained.
        return backtrace.entries.mapNotNull { entry ->
            val frame = entry.value.resolve(result) as? PhpArray ?: return@mapNotNull null
            SymfonyProfilerDatabaseStackFrame(
                file = frame["file"].resolve(result)?.utf8StringOrNull(),
                line = (frame["line"].resolve(result) as? PhpInteger)?.value
                    ?.takeIf { it in Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong() }
                    ?.toInt(),
                className = frame["class"].resolve(result)?.utf8StringOrNull(),
                function = frame["function"].resolve(result)?.utf8StringOrNull(),
                callType = frame["type"].resolve(result)?.utf8StringOrNull(),
            )
        }
    }

    private fun groupQueries(queries: List<Query>): List<SymfonyProfilerDatabaseQueryGroup> {
        val grouped = linkedMapOf<String, MutableQueryGroup>()
        queries.forEach { query ->
            val group = grouped.getOrPut(query.sql) {
                MutableQueryGroup(query.sql)
            }
            group.count++
            group.totalTimeMs += query.executionSeconds * 1000
            group.stackTraces.add(query.stackTrace)
        }

        return grouped.values.map { group ->
            val totalTimeMs = roundToTwoDecimals(group.totalTimeMs)
            SymfonyProfilerDatabaseQueryGroup(
                sql = group.sql,
                count = group.count,
                totalTimeMs = totalTimeMs,
                averageTimeMs = roundToTwoDecimals(totalTimeMs / group.count),
                stackTraces = group.stackTraces.toList(),
            )
        }.sortedByDescending { it.totalTimeMs }
    }

    private fun PhpValue?.numberOrZero(): Double = when (this) {
        is PhpFloat -> value
        is PhpInteger -> value.toDouble()
        else -> 0.0
    }

    private fun roundToTwoDecimals(value: Double): Double =
        BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).toDouble()

    private data class Query(
        val sql: String,
        val executionSeconds: Double,
        val stackTrace: List<SymfonyProfilerDatabaseStackFrame>,
    )

    private data class MutableQueryGroup(
        val sql: String,
        var count: Int = 0,
        var totalTimeMs: Double = 0.0,
        val stackTraces: MutableList<List<SymfonyProfilerDatabaseStackFrame>> = mutableListOf(),
    )
}

data class SymfonyProfilerDatabase(
    val queryCount: Int,
    val totalTimeMs: Double,
    val connections: List<String>,
    val duplicateQueryCount: Int,
    val queryGroups: List<SymfonyProfilerDatabaseQueryGroup>,
)

data class SymfonyProfilerDatabaseQueryGroup(
    val sql: String,
    val count: Int,
    val totalTimeMs: Double,
    val averageTimeMs: Double,
    /** One complete trace per query occurrence; frame arguments are never retained. */
    val stackTraces: List<List<SymfonyProfilerDatabaseStackFrame>> = emptyList(),
)

data class SymfonyProfilerDatabaseStackFrame(
    val file: String?,
    val line: Int?,
    val className: String?,
    val function: String?,
    val callType: String?,
)
