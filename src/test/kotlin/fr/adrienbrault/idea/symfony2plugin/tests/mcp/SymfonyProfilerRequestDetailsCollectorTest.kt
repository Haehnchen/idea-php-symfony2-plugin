package fr.adrienbrault.idea.symfony2plugin.tests.mcp

import fr.adrienbrault.idea.symfony2plugin.mcp.collector.SymfonyProfilerRequestDetailsCollector
import fr.adrienbrault.idea.symfony2plugin.profiler.ProfilerIndexInterface
import fr.adrienbrault.idea.symfony2plugin.profiler.consumer.SymfonyProfilerDatabase
import fr.adrienbrault.idea.symfony2plugin.profiler.consumer.SymfonyProfilerDatabaseQueryGroup
import fr.adrienbrault.idea.symfony2plugin.profiler.consumer.SymfonyProfilerDatabaseStackFrame
import fr.adrienbrault.idea.symfony2plugin.profiler.dict.ProfilerRequestInterface

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
            fixtureCollector().collect("abcdef", "twig")
            fail("Expected unsupported collector to fail")
        } catch (exception: Throwable) {
            assertTrue("Unsupported profiler collector 'twig'" in exception.message.orEmpty())
        }
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
