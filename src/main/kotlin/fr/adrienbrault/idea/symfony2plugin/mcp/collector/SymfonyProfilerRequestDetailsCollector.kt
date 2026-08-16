package fr.adrienbrault.idea.symfony2plugin.mcp.collector

import com.intellij.mcpserver.mcpFail
import fr.adrienbrault.idea.symfony2plugin.profiler.ProfilerIndexInterface
import fr.adrienbrault.idea.symfony2plugin.profiler.consumer.SymfonyProfilerDatabase
import fr.adrienbrault.idea.symfony2plugin.profiler.consumer.SymfonyProfilerDatabaseConsumer
import fr.adrienbrault.idea.symfony2plugin.profiler.consumer.SymfonyProfilerDatabaseQueryGroup
import fr.adrienbrault.idea.symfony2plugin.profiler.consumer.SymfonyProfilerProfile
import fr.adrienbrault.idea.symfony2plugin.profiler.consumer.SymfonyProfilerRequest
import fr.adrienbrault.idea.symfony2plugin.profiler.consumer.SymfonyProfilerRequestConsumer
import fr.adrienbrault.idea.symfony2plugin.profiler.consumer.SymfonyProfilerRequestSummary
import fr.adrienbrault.idea.symfony2plugin.profiler.consumer.SymfonyProfilerTime
import fr.adrienbrault.idea.symfony2plugin.profiler.consumer.SymfonyProfilerTimeConsumer
import fr.adrienbrault.idea.symfony2plugin.profiler.consumer.SymfonyProfilerTimeEvent
import fr.adrienbrault.idea.symfony2plugin.profiler.consumer.SymfonyProfilerTwig
import fr.adrienbrault.idea.symfony2plugin.profiler.consumer.SymfonyProfilerTwigConsumer
import fr.adrienbrault.idea.symfony2plugin.profiler.consumer.SymfonyProfilerTwigProfile
import fr.adrienbrault.idea.symfony2plugin.profiler.consumer.SymfonyProfilerTwigTemplate
import fr.adrienbrault.idea.symfony2plugin.profiler.renderer.ProfilerDetailRenderer
import fr.adrienbrault.idea.symfony2plugin.profiler.renderer.SymfonyProfilerLoggerDetailRenderer
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.ceil

private const val OVERVIEW_QUERY_GROUP_LIMIT = 3
private const val OVERVIEW_TIME_EVENT_LIMIT = 3
private const val OVERVIEW_TWIG_TEMPLATE_LIMIT = 5
private const val DETAIL_PAGE_SIZE = 50
private const val REQUEST_DETAIL_PAGE_SIZE = 100
private const val CALL_LIMIT = 5
private val INTERNAL_CALL_ROOT_NAMESPACES = setOf("doctrine", "symfony")
private val CONTROL_CHARACTERS = Regex("[\\u0000-\\u001F\\u007F]+")

/**
 * Loads and renders one profiler request through an extensible collector renderer registry.
 */
class SymfonyProfilerRequestDetailsCollector(
    private val profilerIndex: ProfilerIndexInterface,
) {
    private val renderers: List<ProfilerDetailRenderer> = listOf(
        RequestProfilerDetailRenderer(),
        SymfonyProfilerLoggerDetailRenderer(),
        TimeProfilerDetailRenderer(),
        TwigProfilerDetailRenderer(),
        DatabaseProfilerDetailRenderer(),
    )
        .sortedByDescending { it.overviewWeight }

    /**
     * @param hash exact hexadecimal profiler token
     * @param collector optional collector name; `null` renders the compact overview
     * @param page 1-based page for the paginated collector detail view
     */
    fun collect(hash: String, collector: String? = null, page: Int = 1): String {
        val normalizedHash = hash.trim()
        if (!normalizedHash.matches(Regex("^[a-fA-F0-9]{6,64}$"))) {
            mcpFail("hash must be a 6-64 character hexadecimal profiler token.")
        }

        val selectedCollector = collector?.trim()?.lowercase()?.takeIf { it.isNotEmpty() }
        val selectedRenderer = selectedCollector?.let { requested ->
            renderers.firstOrNull { it.name == requested }
                ?: mcpFail(
                    "Unsupported profiler collector '$requested'. Supported collectors: " +
                        renderers.joinToString { it.name } + ".",
                )
        }

        val rawProfile = profilerIndex.getRawProfile(normalizedHash)
            ?: mcpFail(
                "Raw profiler data for '$normalizedHash' is not available. " +
                    "Profiler details currently require a local Symfony profiler file.",
            )

        val profile = try {
            SymfonyProfilerProfile.read(rawProfile)
        } catch (exception: Exception) {
            mcpFail("Unable to parse profiler request '$normalizedHash': ${exception.message}")
        }

        // Validate availability once so individual renderers only handle their collector payload.
        val availableCollectorNames = profile.collectorNames.toSet()
        val availableRenderers = renderers.filter { it.name in availableCollectorNames }
        if (selectedRenderer != null && selectedRenderer !in availableRenderers) {
            mcpFail("Profiler request '$normalizedHash' does not contain the '${selectedRenderer.name}' collector.")
        }

        return buildString {
            appendRequestOverview(profile, normalizedHash)

            if (selectedRenderer != null) {
                appendLine()
                append(renderDetails(selectedRenderer, profile, page.coerceAtLeast(1), normalizedHash))
            } else if (availableRenderers.isNotEmpty()) {
                availableRenderers.forEach { renderer ->
                    val overview = renderOverview(renderer, profile, normalizedHash) ?: return@forEach
                    appendLine()
                    append(overview)
                }
            } else {
                appendLine()
                appendLine("No supported profiler detail collectors are available for this request.")
            }
        }.trimEnd()
    }

    private fun renderOverview(
        renderer: ProfilerDetailRenderer,
        profile: SymfonyProfilerProfile,
        hash: String,
    ): String? = render(renderer, hash) {
        renderer.renderOverview(profile)
    }

    private fun renderDetails(
        renderer: ProfilerDetailRenderer,
        profile: SymfonyProfilerProfile,
        page: Int,
        hash: String,
    ): String = render(renderer, hash) {
        renderer.renderDetails(profile, page)
    }

    private fun <T> render(
        renderer: ProfilerDetailRenderer,
        hash: String,
        render: () -> T,
    ): T = try {
        render()
    } catch (exception: Exception) {
        mcpFail("Unable to read the '${renderer.name}' collector for '$hash': ${exception.message}")
    }

    private fun StringBuilder.appendRequestOverview(
        profile: SymfonyProfilerProfile,
        requestedHash: String,
    ) {
        appendLine("# Symfony Profiler Request")
        appendLine()
        appendLine("- Token: ${plainText(profile.token ?: requestedHash)}")
        val method = profile.method
        if (method != null) {
            appendLine("- Method: ${plainText(method)}")
        }
        val url = profile.url
        if (url != null) {
            appendLine("- URL: ${plainText(url)}")
        }
        val statusCode = profile.statusCode
        if (statusCode != null) {
            appendLine("- Status: $statusCode")
        }
    }

    /** Renders only stable request metadata in the compact profile overview. */
    internal fun formatRequestOverview(request: SymfonyProfilerRequest): String = buildString {
        appendLine("## Collector: request")
        appendLine()
        appendLine("- Path: ${request.summary.path.renderOverviewValue()}")
        appendLine("- Route: ${request.summary.route.renderOverviewValue()}")
        appendLine("- Content type: ${request.summary.contentType.renderOverviewValue()}")
    }.trimEnd()

    /** Renders a bounded page of the sanitized generic request value tree. */
    internal fun formatRequestDetails(request: SymfonyProfilerRequest, page: Int = 1): String {
        val lines = ProfilerTextRenderer.render(request.data, initialIndent = 2)
        val totalPages = maxOf(1, ceil(lines.size.toDouble() / REQUEST_DETAIL_PAGE_SIZE).toInt())
        val currentPage = page.coerceIn(1, totalPages)
        val pageLines = lines.drop((currentPage - 1) * REQUEST_DETAIL_PAGE_SIZE).take(REQUEST_DETAIL_PAGE_SIZE)

        return buildString {
            appendLine("Collector: request")
            appendLine("Page: $currentPage of $totalPages")
            appendLine()
            appendLine("Summary")
            appendLine()
            appendRequestSummary(request.summary)
            appendLine()
            appendLine("Data")
            appendLine()
            pageLines.forEach(::appendLine)
        }.trimEnd()
    }

    private fun StringBuilder.appendRequestSummary(summary: SymfonyProfilerRequestSummary) {
        appendLine("Method: ${summary.method.renderRequestSummaryValue()}")
        appendLine("Path: ${summary.path.renderRequestSummaryValue()}")
        appendLine("Route: ${summary.route.renderRequestSummaryValue()}")
        appendLine("Status: ${summary.statusCode?.toString() ?: "none"}")
        appendLine("Content type: ${summary.contentType.renderRequestSummaryValue()}")
    }

    /** Renders the three slowest events in the compact performance overview. */
    internal fun formatTimeOverview(time: SymfonyProfilerTime): String = formatTimeSection(
        time,
        time.events.sortedByDescending { it.durationMs }.take(OVERVIEW_TIME_EVENT_LIMIT),
        "### Top $OVERVIEW_TIME_EVENT_LIMIT events by duration",
    )

    /** Renders all events in descending duration order without pagination. */
    internal fun formatTimeDetails(time: SymfonyProfilerTime): String = formatTimeSection(
        time,
        time.events.sortedByDescending { it.durationMs },
        "### Events ordered by duration",
    )

    private fun formatTimeSection(
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

    /** Renders the first five unique templates in their initial rendering order. */
    internal fun formatTwigOverview(twig: SymfonyProfilerTwig): String = formatTwigSection(
        twig,
        twig.renderedTemplates.take(OVERVIEW_TWIG_TEMPLATE_LIMIT),
        "### First $OVERVIEW_TWIG_TEMPLATE_LIMIT rendered templates",
        includeCallTree = false,
    )

    /** Renders all unique templates and the complete rendering call tree without pagination. */
    internal fun formatTwigDetails(twig: SymfonyProfilerTwig): String = formatTwigSection(
        twig,
        twig.renderedTemplates,
        "### Rendered templates",
        includeCallTree = true,
    )

    private fun formatTwigSection(
        twig: SymfonyProfilerTwig,
        templates: List<SymfonyProfilerTwigTemplate>,
        heading: String,
        includeCallTree: Boolean,
    ): String = buildString {
        appendLine("## Collector: twig")
        appendLine()
        appendLine("- Render time: ${formatMilliseconds(twig.renderTimeMs)} ms")
        appendLine("- Template calls: ${twig.templateCallCount}")
        appendLine("- Block calls: ${twig.blockCallCount}")
        appendLine("- Macro calls: ${twig.macroCallCount}")
        appendLine("- Unique templates: ${twig.renderedTemplates.size}")
        appendLine()
        appendLine(heading)

        if (templates.isEmpty()) {
            appendLine()
            appendLine("No Twig templates were rendered.")
        } else {
            appendLine()
            appendLine("| Template | Path | Render count |")
            appendLine("| --- | --- | ---: |")
            templates.forEach { template ->
                appendLine(
                    "| ${plainText(template.name)} | ${plainText(template.path ?: "none")} | " +
                        "${template.renderCount} |",
                )
            }
        }

        if (includeCallTree) {
            appendLine()
            appendLine("### Rendering call tree")
            appendLine()
            appendTwigProfile(twig.root, twig.renderTimeMs)
        }
    }.trimEnd()

    private fun StringBuilder.appendTwigProfile(
        profile: SymfonyProfilerTwigProfile,
        rootDurationMs: Double,
        prefix: String = "",
        hasFollowingSibling: Boolean = false,
    ) {
        val label = when (profile.type) {
            "ROOT" -> treeText(profile.name)
            "template" -> "$prefix└ ${treeText(profile.template)}"
            else -> "$prefix└ ${treeText(profile.template)}::${treeText(profile.type)}(${treeText(profile.name)})"
        }
        val timing = if (profile.durationMs >= 1.0) {
            val percentage = if (rootDurationMs > 0.0) profile.durationMs / rootDurationMs * 100.0 else 0.0
            " ${formatMilliseconds(profile.durationMs)}ms/${formatPercentage(percentage)}%"
        } else {
            ""
        }
        appendLine("    $label$timing")

        val childPrefix = if (profile.type == "ROOT") {
            prefix
        } else {
            prefix + if (hasFollowingSibling) "│ " else "  "
        }
        profile.children.forEachIndexed { index, child ->
            appendTwigProfile(
                child,
                rootDurationMs,
                childPrefix,
                hasFollowingSibling = index + 1 < profile.children.size,
            )
        }
    }

    /** Renders the compact database summary with its top query groups. */
    internal fun formatDatabaseOverview(database: SymfonyProfilerDatabase): String = formatDatabaseSection(
        database,
        database.queryGroups.take(OVERVIEW_QUERY_GROUP_LIMIT),
        "### Top $OVERVIEW_QUERY_GROUP_LIMIT query groups",
    )

    /** Renders one paginated page containing all database query groups. */
    internal fun formatDatabaseDetails(
        database: SymfonyProfilerDatabase,
        page: Int = 1,
    ): String {
        val totalPages = maxOf(1, ceil(database.queryGroups.size.toDouble() / DETAIL_PAGE_SIZE).toInt())
        val currentPage = page.coerceIn(1, totalPages)
        val groups = database.queryGroups.drop((currentPage - 1) * DETAIL_PAGE_SIZE).take(DETAIL_PAGE_SIZE)
        return formatDatabaseSection(database, groups, "### Query groups (page $currentPage of $totalPages)")
    }

    private fun formatDatabaseSection(
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

    /** Adds sanitized request data while keeping the renderer registry collector-neutral. */
    private inner class RequestProfilerDetailRenderer : ProfilerDetailRenderer {
        override val name = "request"
        override val overviewWeight = 100

        override fun renderOverview(profile: SymfonyProfilerProfile): String =
            formatRequestOverview(SymfonyProfilerRequestConsumer.read(profile))

        override fun renderDetails(profile: SymfonyProfilerProfile, page: Int): String =
            formatRequestDetails(SymfonyProfilerRequestConsumer.read(profile), page)
    }

    /** Renders Symfony Stopwatch events exposed by the profiler's time collector. */
    private inner class TimeProfilerDetailRenderer : ProfilerDetailRenderer {
        override val name = "time"
        override val overviewWeight = 50

        override fun renderOverview(profile: SymfonyProfilerProfile): String =
            formatTimeOverview(SymfonyProfilerTimeConsumer.read(profile))

        override fun renderDetails(profile: SymfonyProfilerProfile, page: Int): String =
            formatTimeDetails(SymfonyProfilerTimeConsumer.read(profile))
    }

    /** Renders Twig metrics, unique templates, and their complete call tree. */
    private inner class TwigProfilerDetailRenderer : ProfilerDetailRenderer {
        override val name = "twig"
        override val overviewWeight = 25

        override fun renderOverview(profile: SymfonyProfilerProfile): String =
            formatTwigOverview(SymfonyProfilerTwigConsumer.read(profile))

        override fun renderDetails(profile: SymfonyProfilerProfile, page: Int): String =
            formatTwigDetails(SymfonyProfilerTwigConsumer.read(profile))
    }

    /** Renders Doctrine data exposed by the profiler's db collector. */
    private inner class DatabaseProfilerDetailRenderer : ProfilerDetailRenderer {
        override val name = "db"

        override fun renderOverview(profile: SymfonyProfilerProfile): String =
            formatDatabaseOverview(SymfonyProfilerDatabaseConsumer.read(profile))

        override fun renderDetails(profile: SymfonyProfilerProfile, page: Int): String =
            formatDatabaseDetails(SymfonyProfilerDatabaseConsumer.read(profile), page)
    }
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

private fun formatPercentage(value: Double): String =
    BigDecimal.valueOf(value).setScale(0, RoundingMode.HALF_UP).toPlainString()

private fun formatMemoryMiB(bytes: Long): String = BigDecimal.valueOf(bytes)
    .divide(BigDecimal.valueOf(1024L * 1024L), 2, RoundingMode.HALF_UP)
    .toPlainString()

/** Keeps untrusted profiler values on one table-safe line. */
private fun plainText(value: String): String = value
    .replace(CONTROL_CHARACTERS, " ")
    .replace("|", "\\|")

private fun treeText(value: String): String = value.replace(CONTROL_CHARACTERS, " ")

private fun String?.renderRequestSummaryValue(): String =
    this?.takeIf { it.isNotEmpty() }?.replace(CONTROL_CHARACTERS, " ") ?: "none"

private fun String?.renderOverviewValue(): String =
    this?.takeIf { it.isNotEmpty() }?.let(::plainText) ?: "none"
