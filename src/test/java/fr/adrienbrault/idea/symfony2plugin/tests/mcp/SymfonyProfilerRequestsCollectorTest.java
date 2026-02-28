package fr.adrienbrault.idea.symfony2plugin.tests.mcp;

import fr.adrienbrault.idea.symfony2plugin.mcp.collector.SymfonyProfilerRequestsCollector;

public class SymfonyProfilerRequestsCollectorTest extends McpCollectorTestCase {
    public void testCollectFailsWithoutProfilerIndexOnLightVfs() {
        try {
            new SymfonyProfilerRequestsCollector(getProject()).collect(null, null, null, null);
            fail("Expected profiler collector to fail when no file-system profiler index is available");
        } catch (Throwable e) {
            assertNotNull(e.getMessage());
            assertTrue(e.getMessage().contains("No profiler index available"));
        }
    }
}
