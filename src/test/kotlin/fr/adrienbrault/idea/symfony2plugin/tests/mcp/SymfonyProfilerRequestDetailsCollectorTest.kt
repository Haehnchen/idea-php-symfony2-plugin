package fr.adrienbrault.idea.symfony2plugin.tests.mcp

import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpBytes
import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpStringKey
import fr.adrienbrault.idea.symfony2plugin.mcp.collector.SymfonyProfilerRequestDetailsCollector
import fr.adrienbrault.idea.symfony2plugin.profiler.ProfilerIndexInterface
import fr.adrienbrault.idea.symfony2plugin.profiler.consumer.SymfonyProfilerDatabase
import fr.adrienbrault.idea.symfony2plugin.profiler.consumer.SymfonyProfilerDatabaseQueryGroup
import fr.adrienbrault.idea.symfony2plugin.profiler.consumer.SymfonyProfilerDatabaseStackFrame
import fr.adrienbrault.idea.symfony2plugin.profiler.consumer.SymfonyProfilerRequest
import fr.adrienbrault.idea.symfony2plugin.profiler.consumer.SymfonyProfilerRequestSummary
import fr.adrienbrault.idea.symfony2plugin.profiler.consumer.SymfonyProfilerTime
import fr.adrienbrault.idea.symfony2plugin.profiler.consumer.SymfonyProfilerTimeEvent
import fr.adrienbrault.idea.symfony2plugin.profiler.dict.ProfilerRequestInterface
import fr.adrienbrault.idea.symfony2plugin.profiler.decoder.ProfilerArray
import fr.adrienbrault.idea.symfony2plugin.profiler.decoder.ProfilerEntry
import fr.adrienbrault.idea.symfony2plugin.profiler.decoder.ProfilerInteger
import java.nio.charset.StandardCharsets

class SymfonyProfilerRequestDetailsCollectorTest : McpCollectorTestCase() {
    fun testHashOnlyReturnsCompactOverview() {
        val text = fixtureCollector().collect("abcdef")

        assertTrue(text.startsWith("# Symfony Profiler Request\n"))
        assertTrue("- Method: GET" in text)
        assertTrue("- URL: http://example.test/orders/42" in text)
        assertTrue("- Status: 200" in text)
        assertTrue("## Collector: db" in text)
        assertTrue("- Queries: 3" in text)
        assertTrue("- Query time: 12.34 ms" in text)
        assertTrue("### Top 3 query groups" in text)
        assertTrue("| Occurrences | Time (ms) | Average time (ms) | Query | Calls |" in text)
        assertTrue(
            "| 2 | 10.00 | 5.00 | SELECT * FROM users WHERE id = ? | " +
                "ExampleRepository:findOne, ExampleService:load, ExampleController:show |" in text,
        )
        assertTrue(
            "| 1 | 2.34 | 2.34 | UPDATE users SET last_seen = ? WHERE id = ? | " +
                "ExampleRepository:updateOne, ExampleService:save, ExampleController:update |" in text,
        )
        assertFalse("Doctrine\\" in text)
        assertFalse("Symfony\\" in text)
        assertFalse("App\\" in text)
        assertFalse("executeQuery" in text)
        assertFalse("getOneOrNullResult" in text)
        assertFalse(":flush" in text)
        assertFalse("<code>" in text)
        assertFalse("<br>" in text)
        assertFalse("**" in text)
        assertFalse(text.trimStart().startsWith("{"))
    }

    fun testDbSelectorReturnsExpandedSection() {
        val text = fixtureCollector().collect("abcdef", "db")

        assertTrue("### Query group" in text)
        assertFalse("### Top query group" in text)
        assertFalse("***REDACTED***" in text)
        assertFalse("2026-08-13" in text)
    }

    fun testOverviewLimitsAndEscapesQueryGroups() {
        val collector = fixtureCollector()
        val groups = (1..6).map { index ->
            SymfonyProfilerDatabaseQueryGroup(
                sql = if (index == 1) "SELECT '<tag>' | value\r\n\tFROM test" else "query $index",
                count = index,
                totalTimeMs = index.toDouble(),
                averageTimeMs = 1.0,
            )
        }
        val text = collector.formatDatabaseOverview(
            SymfonyProfilerDatabase(
                queryCount = 21,
                totalTimeMs = 21.0,
                connections = listOf("default"),
                duplicateQueryCount = 5,
                queryGroups = groups,
            ),
        )

        assertTrue("### Top 3 query groups" in text)
        assertTrue("SELECT '<tag>' \\| value FROM test" in text)
        assertTrue("query 3" in text)
        assertFalse("query 4" in text)
    }

    fun testDetailPaginatesQueryGroupsWith50PerPage() {
        val collector = fixtureCollector()
        val database = SymfonyProfilerDatabase(
            queryCount = 120,
            totalTimeMs = 120.0,
            connections = listOf("default"),
            duplicateQueryCount = 0,
            queryGroups = (1..120).map { index ->
                SymfonyProfilerDatabaseQueryGroup(
                    sql = "query $index",
                    count = 1,
                    totalTimeMs = 1.0,
                    averageTimeMs = 1.0,
                )
            },
        )

        val defaultPage = collector.formatDatabaseDetails(database)
        assertTrue("### Query groups (page 1 of 3)" in defaultPage)

        val firstPage = collector.formatDatabaseDetails(database, page = 1)
        assertTrue("### Query groups (page 1 of 3)" in firstPage)
        assertTrue("| query 50 |" in firstPage)
        assertFalse("| query 51 |" in firstPage)

        val secondPage = collector.formatDatabaseDetails(database, page = 2)
        assertTrue("### Query groups (page 2 of 3)" in secondPage)
        assertTrue("| query 51 |" in secondPage)
        assertFalse("| query 50 |" in secondPage)

        val clampedPage = collector.formatDatabaseDetails(database, page = 99)
        assertTrue("### Query groups (page 3 of 3)" in clampedPage)
        assertTrue("| query 120 |" in clampedPage)
    }

    fun testCallsFilterFrameworkNamespacesDeduplicateAndLimitResults() {
        val database = SymfonyProfilerDatabase(
            queryCount = 1,
            totalTimeMs = 1.0,
            connections = listOf("default"),
            duplicateQueryCount = 0,
            queryGroups = listOf(
                SymfonyProfilerDatabaseQueryGroup(
                    sql = "SELECT 1",
                    count = 1,
                    totalTimeMs = 1.0,
                    averageTimeMs = 1.0,
                    stackTraces = listOf(
                        listOf(
                            stackFrame("Doctrine\\DBAL\\Connection", "executeQuery"),
                            stackFrame("Symfony\\Component\\HttpKernel\\HttpKernel", "handle"),
                            stackFrame("App\\Repository\\OrderRepository", "findOne"),
                            stackFrame("App\\Repository\\OrderRepository", "findOne"),
                            stackFrame("App\\Service\\OrderService", "load"),
                            stackFrame("Vendor\\Package\\Handler", "run"),
                            stackFrame("\\App\\Controller\\OrderController", "show"),
                            stackFrame("App\\Listener\\OrderListener", "onEvent"),
                            stackFrame("App\\Command\\OrderCommand", "execute"),
                        ),
                    ),
                ),
            ),
        )

        val text = fixtureCollector().formatDatabaseOverview(database)

        assertTrue(
            "OrderRepository:findOne, OrderService:load, Handler:run, " +
                "OrderController:show, OrderListener:onEvent" in text,
        )
        assertFalse("OrderCommand:execute" in text)
        assertFalse("Doctrine\\" in text)
        assertFalse("Symfony\\" in text)
        assertFalse("App\\" in text)
    }

    fun testRejectsUnsupportedCollectorBeforeLoadingRawProfile() {
        try {
            fixtureCollector().collect("abcdef", "logger")
            fail("Expected unsupported collector to fail")
        } catch (exception: Throwable) {
            assertTrue("Unsupported profiler collector 'logger'" in exception.message.orEmpty())
        }
    }

    fun testRequestSelectorReturnsSanitizedProjectNeutralPlainText() {
        val text = requestFixtureCollector().collect("abc123", "request")

        assertTrue("URL: http://example.test/login?page=2" in text)
        assertTrue("Collector: request" in text)
        assertTrue("Path: /login" in text)
        assertTrue("Route: app_login" in text)
        assertTrue("request_query:" in text)
        assertTrue("api_key: ***REDACTED***" in text)
        assertTrue("email: user@example.test" in text)
        assertTrue("feature: neutral-value" in text)
        assertTrue("\\0ProfilerFixture\\TraceContext\\0secretToken: ***REDACTED***" in text)

        listOf(
            "query-secret",
            "request-secret",
            "authorization-secret",
            "database-secret",
            "session-secret",
            "body-secret",
            "curl-secret",
            "object-secret",
            "future-secret",
        ).forEach { secret -> assertFalse(secret in text) }
        val requestDetails = text.substringAfter("Collector: request")
        assertFalse(requestDetails.lineSequence().any { line ->
            line.startsWith("#") || line.startsWith("- ") || line.startsWith("|") || line.startsWith("```")
        })
        assertFalse(requestDetails.trimStart().startsWith("{"))
    }

    fun testRequestOverviewAddsOnlyCompactRequestMetadata() {
        val text = requestFixtureCollector().collect("abc123")
        val requestOverview = text.substringAfter("## Collector: request")

        assertTrue("- Path: /login" in requestOverview)
        assertTrue("- Route: app_login" in requestOverview)
        assertTrue("- Content type: application/json" in requestOverview)
        assertFalse("request_query" in requestOverview)
        assertFalse("Summary" in requestOverview)
        assertFalse("Data" in requestOverview)
    }

    fun testRequestDetailsPaginatesGenericValueLines() {
        val request = SymfonyProfilerRequest(
            data = ProfilerArray(
                (1..205).map { index ->
                    ProfilerEntry(
                        PhpStringKey(PhpBytes("field_$index".toByteArray(StandardCharsets.UTF_8))),
                        ProfilerInteger(index.toLong()),
                    )
                },
            ),
            summary = SymfonyProfilerRequestSummary("GET", "/example", "app_example", 200, "text/html"),
        )

        val firstPage = requestFixtureCollector().formatRequestDetails(request, 1)
        assertTrue("Page: 1 of 3" in firstPage)
        assertTrue("field_100: 100" in firstPage)
        assertFalse("field_101: 101" in firstPage)

        val secondPage = requestFixtureCollector().formatRequestDetails(request, 2)
        assertTrue("Page: 2 of 3" in secondPage)
        assertTrue("field_101: 101" in secondPage)
        assertTrue("field_200: 200" in secondPage)
        assertFalse("field_201: 201" in secondPage)
    }

    fun testTimeOverviewShowsThreeSlowestEvents() {
        val text = timeFixtureCollector().collect("fedcba")

        assertTrue("## Collector: time" in text)
        assertTrue("- Total duration: 132.34 ms" in text)
        assertTrue("- Initialization time: 12.34 ms" in text)
        assertTrue("- Stopwatch installed: yes" in text)
        assertTrue("- Events: 4" in text)
        assertTrue("### Top 3 events by duration" in text)
        assertTrue("| Event | Category | Start (ms) | End (ms) | Duration (ms) | Memory (MiB) |" in text)
        assertTrue("| controller | section | 10.00 | 95.00 | 70.00 | 8.00 |" in text)
        assertTrue("| view | template | 96.00 | 120.00 | 24.00 | 7.00 |" in text)
        assertTrue("| response.listener | event_listener | 80.00 | 92.50 | 12.50 | 5.00 |" in text)
        assertFalse("| kernel.request |" in text)
        assertFalse("__section__" in text)
    }

    fun testTimeDetailsShowsAllEventsOrderedByDurationWithoutPagination() {
        val text = timeFixtureCollector().collect("fedcba", "time", page = 99)

        assertTrue("### Events ordered by duration" in text)
        assertTrue("| kernel.request | event_listener | 0.00 | 8.75 | 8.75 | 4.00 |" in text)
        assertTrue(text.indexOf("| controller |") < text.indexOf("| view |"))
        assertTrue(text.indexOf("| view |") < text.indexOf("| response.listener |"))
        assertTrue(text.indexOf("| response.listener |") < text.indexOf("| kernel.request |"))
        assertFalse("Page:" in text)
    }

    fun testTimeDetailsDoesNotLimitOrPaginateEvents() {
        val time = SymfonyProfilerTime(
            durationMs = 75.0,
            initializationTimeMs = 1.0,
            stopwatchInstalled = true,
            events = (1..75).map { index ->
                SymfonyProfilerTimeEvent(
                    name = if (index == 75) "event | 75\ncontinued" else "event $index",
                    category = "section",
                    startMs = index.toDouble(),
                    endMs = index + 1.0,
                    durationMs = index.toDouble(),
                    memoryBytes = 0,
                )
            },
        )

        val text = timeFixtureCollector().formatTimeDetails(time)

        assertTrue("event \\| 75 continued" in text)
        assertTrue("| event 1 |" in text)
        assertFalse("Page:" in text)
    }

    fun testTwigOverviewShowsFirstFiveUniqueTemplatesAndAggregatesDuplicates() {
        val text = twigFixtureCollector().collect("c0ffee")

        assertTrue("## Collector: twig" in text)
        assertTrue("- Render time: 14.00 ms" in text)
        assertTrue("- Template calls: 8" in text)
        assertTrue("- Block calls: 1" in text)
        assertTrue("- Macro calls: 1" in text)
        assertTrue("- Unique templates: 7" in text)
        assertTrue("### First 5 rendered templates" in text)
        assertTrue("| Template | Path | Render count |" in text)
        assertTrue("| components/card.html.twig | templates/components/card.html.twig | 2 |" in text)
        assertTrue("| emails/banner.html.twig | templates/emails/banner.html.twig | 1 |" in text)
        assertFalse("@WebProfiler/Profiler/toolbar_js.html.twig" in text)
        assertFalse("### Rendering call tree" in text)
    }

    fun testTwigDetailsShowsAllUniqueTemplatesAndCompleteCallTree() {
        val text = twigFixtureCollector().collect("c0ffee", "twig", page = 99)

        assertTrue("### Rendered templates" in text)
        assertTrue("@WebProfiler/Profiler/toolbar_js.html.twig" in text)
        assertTrue("@WebProfiler/Profiler/toolbar.html.twig" in text)
        assertTrue("### Rendering call tree" in text)
        assertTrue("    main 14.00ms/100%" in text)
        assertTrue("    └ catalog/detail.html.twig 12.00ms/86%" in text)
        assertTrue("    │ └ components/price.html.twig::macro(format_price)" in text)
        assertTrue("    │ └ base.html.twig 6.00ms/43%" in text)
        assertTrue("    │   └ catalog/detail.html.twig::block(title)" in text)
        assertTrue("    └ @WebProfiler/Profiler/toolbar_js.html.twig 2.00ms/14%" in text)
        val callTree = text.substringAfter("### Rendering call tree")
        assertEquals(2, callTree.windowed("components/card.html.twig".length)
            .count { it == "components/card.html.twig" })
        assertFalse("Page:" in text)
    }

    fun testReportsUnavailableRawProfile() {
        try {
            SymfonyProfilerRequestDetailsCollector(TestProfilerIndex(null)).collect("abcdef")
            fail("Expected unavailable raw profile to fail")
        } catch (exception: Throwable) {
            assertTrue("currently require a local Symfony profiler file" in exception.message.orEmpty())
        }
    }

    private fun fixtureCollector() = SymfonyProfilerRequestDetailsCollector(
        TestProfilerIndex(resourceFixture("symfony-profiler-db.gz")),
    )

    private fun requestFixtureCollector() = SymfonyProfilerRequestDetailsCollector(
        TestProfilerIndex(resourceFixture("symfony-profiler-request.gz")),
    )

    private fun timeFixtureCollector() = SymfonyProfilerRequestDetailsCollector(
        TestProfilerIndex(resourceFixture("symfony-profiler-time.gz")),
    )

    private fun twigFixtureCollector() = SymfonyProfilerRequestDetailsCollector(
        TestProfilerIndex(resourceFixture("symfony-profiler-twig.gz")),
    )

    private fun resourceFixture(name: String): ByteArray = requireNotNull(
        javaClass.getResourceAsStream("/profiler/generated/$name"),
    ).use { it.readAllBytes() }

    private fun stackFrame(className: String, function: String) = SymfonyProfilerDatabaseStackFrame(
        file = null,
        line = null,
        className = className,
        function = function,
        callType = null,
    )

    private class TestProfilerIndex(private val rawProfile: ByteArray?) : ProfilerIndexInterface {
        override fun getRequests(): List<ProfilerRequestInterface> = emptyList()

        override fun getUrlForRequest(request: ProfilerRequestInterface): String? = null

        override fun getRawProfile(hash: String): ByteArray? = rawProfile
    }
}
