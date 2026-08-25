package fr.adrienbrault.idea.symfony2plugin.tests.profiler.renderer

import fr.adrienbrault.idea.symfony2plugin.profiler.consumer.SymfonyProfilerDatabase
import fr.adrienbrault.idea.symfony2plugin.profiler.consumer.SymfonyProfilerDatabaseConsumer
import fr.adrienbrault.idea.symfony2plugin.profiler.consumer.SymfonyProfilerDatabaseQueryGroup
import fr.adrienbrault.idea.symfony2plugin.profiler.consumer.SymfonyProfilerDatabaseStackFrame
import fr.adrienbrault.idea.symfony2plugin.profiler.consumer.SymfonyProfilerProfile
import fr.adrienbrault.idea.symfony2plugin.profiler.renderer.SymfonyProfilerDatabaseDetailRenderer
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SymfonyProfilerDatabaseDetailRendererTest {
    private val renderer = SymfonyProfilerDatabaseDetailRenderer

    @Test
    fun `overview renders neutral query summary`() {
        val database = SymfonyProfilerDatabaseConsumer.read(readProfile("symfony-profiler-db.gz"))
        val text = renderer.formatOverview(database)

        assertTrue("- Queries: 3" in text)
        assertTrue("- Query time: 12.34 ms" in text)
        assertTrue("### Top 3 query groups" in text)
        assertTrue("| Occurrences | Time (ms) | Average time (ms) | Query | Calls |" in text)
        assertTrue(
            "| 2 | 10.00 | 5.00 | SELECT * FROM users WHERE id = ? | " +
                    "ExampleRepository:findOne, ExampleService:load, ExampleController:show |" in text,
        )
        assertFalse("Doctrine\\" in text)
        assertFalse("Symfony\\" in text)
        assertFalse("<code>" in text)
    }

    @Test
    fun `overview limits and escapes query groups`() {
        val groups = (1..6).map { index ->
            SymfonyProfilerDatabaseQueryGroup(
                sql = if (index == 1) "SELECT '<tag>' | value\r\n\tFROM test" else "query $index",
                count = index,
                totalTimeMs = index.toDouble(),
                averageTimeMs = 1.0,
            )
        }
        val text = renderer.formatOverview(
            SymfonyProfilerDatabase(21, 21.0, listOf("default"), 5, groups),
        )

        assertTrue("SELECT '<tag>' \\| value FROM test" in text)
        assertTrue("query 3" in text)
        assertFalse("query 4" in text)
    }

    @Test
    fun `details paginate complete query groups around the token target`() {
        val compactDatabase = SymfonyProfilerDatabase(
            queryCount = 120,
            totalTimeMs = 120.0,
            connections = listOf("default"),
            duplicateQueryCount = 0,
            queryGroups = (1..120).map { index ->
                SymfonyProfilerDatabaseQueryGroup("query $index", 1, 1.0, 1.0)
            },
        )

        val compactDetails = renderer.formatDetails(compactDatabase)
        assertTrue("### Query groups" in compactDetails)
        assertFalse("### Query groups (page" in compactDetails)
        assertTrue("| query 120 |" in compactDetails)

        val largeDatabase = SymfonyProfilerDatabase(
            queryCount = 4,
            totalTimeMs = 4.0,
            connections = listOf("default"),
            duplicateQueryCount = 0,
            queryGroups = (1..4).map { index ->
                SymfonyProfilerDatabaseQueryGroup("query $index ${"x".repeat(5_000)}", 1, 1.0, 1.0)
            },
        )

        val firstPage = renderer.formatDetails(largeDatabase)
        assertTrue("### Query groups (page 1 of 2)" in firstPage)
        assertTrue("| query 3 " in firstPage)
        assertFalse("| query 4 " in firstPage)

        val secondPage = renderer.formatDetails(largeDatabase, page = 2)
        assertTrue("### Query groups (page 2 of 2)" in secondPage)
        assertTrue("| query 4 " in secondPage)
        assertFalse("| query 3 " in secondPage)

        val clampedPage = renderer.formatDetails(largeDatabase, page = 99)
        assertTrue("### Query groups (page 2 of 2)" in clampedPage)
        assertTrue("| query 4 " in clampedPage)
    }

    @Test
    fun `calls filter framework namespaces deduplicate and limit results`() {
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

        val text = renderer.formatOverview(database)

        assertTrue(
            "OrderRepository:findOne, OrderService:load, Handler:run, " +
                    "OrderController:show, OrderListener:onEvent" in text,
        )
        assertFalse("OrderCommand:execute" in text)
        assertFalse("Doctrine\\" in text)
        assertFalse("Symfony\\" in text)
        assertFalse("App\\" in text)
    }

    private fun readProfile(name: String) = SymfonyProfilerProfile.read(resourceFixture(name))

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
}
