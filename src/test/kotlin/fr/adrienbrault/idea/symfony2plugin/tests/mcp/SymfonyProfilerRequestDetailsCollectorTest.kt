package fr.adrienbrault.idea.symfony2plugin.tests.mcp

import fr.adrienbrault.idea.symfony2plugin.mcp.collector.SymfonyProfilerRequestDetailsCollector
import fr.adrienbrault.idea.symfony2plugin.profiler.ProfilerIndexInterface
import fr.adrienbrault.idea.symfony2plugin.profiler.dict.ProfilerRequestInterface

class SymfonyProfilerRequestDetailsCollectorTest : McpCollectorTestCase() {
    fun testHashOnlyReturnsCompactOverview() {
        val text = fixtureCollector("symfony-profiler-db.gz").collect("abcdef")

        assertTrue(text.startsWith("# Symfony Profiler Request\n"))
        assertTrue("- Method: GET" in text)
        assertTrue("- URL: http://example.test/orders/42" in text)
        assertTrue("- Status: 200" in text)
        assertTrue("## Collector: db" in text)
        assertTrue("### Top 3 query groups" in text)
    }

    fun testCollectorCanBeSelected() {
        val fixtures = listOf(
            Triple("symfony-profiler-request.gz", "abc123", "request"),
            Triple("symfony-profiler-time.gz", "fedcba", "time"),
            Triple("symfony-profiler-twig.gz", "c0ffee", "twig"),
            Triple("symfony-profiler-logger.gz", "10ca11", "logger"),
            Triple("symfony-profiler-events-symfony-6.3.gz", "e71e17", "events"),
            Triple("symfony-profiler-db.gz", "abcdef", "db"),
        )

        fixtures.forEach { (fixture, hash, collector) ->
            val text = fixtureCollector(fixture).collect(hash, collector, page = 99)
            assertTrue("Missing selected collector '$collector'", "Collector: $collector" in text)
        }
    }

    fun testRejectsUnsupportedCollectorBeforeLoadingRawProfile() {
        try {
            fixtureCollector("symfony-profiler-db.gz").collect("abcdef", "mailer")
            fail("Expected unsupported collector to fail")
        } catch (exception: Throwable) {
            assertTrue("Unsupported profiler collector 'mailer'" in exception.message.orEmpty())
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

    private fun fixtureCollector(fixture: String) = SymfonyProfilerRequestDetailsCollector(
        TestProfilerIndex(resourceFixture(fixture)),
    )

    private fun resourceFixture(name: String): ByteArray = requireNotNull(
        javaClass.getResourceAsStream("/profiler/generated/$name"),
    ).use { it.readAllBytes() }

    private class TestProfilerIndex(private val rawProfile: ByteArray?) : ProfilerIndexInterface {
        override fun getRequests(): List<ProfilerRequestInterface> = emptyList()

        override fun getUrlForRequest(request: ProfilerRequestInterface): String? = null

        override fun getRawProfile(hash: String): ByteArray? = rawProfile
    }
}
