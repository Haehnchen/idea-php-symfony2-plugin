package fr.adrienbrault.idea.symfony2plugin.tests.mcp

import fr.adrienbrault.idea.symfony2plugin.mcp.collector.SymfonyRoutePathCollector

class SymfonyRoutePathCollectorTest : McpCollectorTestCase() {
    fun testCollectMatchesDynamicPath() {
        val result = SymfonyRoutePathCollector(project).collect("/edit/12")

        assertTrue("Unexpected CSV:\n$result", result.startsWith("name,controller,path,filePath"))
        assertTrue("Unexpected CSV:\n$result", result.contains("my_car_foo_stuff"))
        assertTrue("Unexpected CSV:\n$result", result.contains("CarController::fooAction"))
        assertUsesRealLineBreaks(result)
    }
}
