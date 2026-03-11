package fr.adrienbrault.idea.symfony2plugin.tests.mcp

import fr.adrienbrault.idea.symfony2plugin.mcp.collector.SymfonyProfilerRequestsCollector

class SymfonyProfilerRequestsCollectorTest : McpCollectorTestCase() {
    fun testCollectFailsWithoutProfilerIndexOnLightVfs() {
        try {
            SymfonyProfilerRequestsCollector(project).collect(null, null, null, null)
            fail("Expected profiler collector to fail when no file-system profiler index is available")
        } catch (e: Throwable) {
            assertNotNull(e.message)
            assertTrue(e.message!!.contains("No profiler index available"))
        }
    }
}
