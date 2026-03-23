package fr.adrienbrault.idea.symfony2plugin.tests.mcp

import fr.adrienbrault.idea.symfony2plugin.mcp.collector.SymfonyRouteCollector

class SymfonyRouteCollectorTest : McpCollectorTestCase() {
    fun testCollectReturnsControllerAndPath() {
        val result = SymfonyRouteCollector(project).collect(null, null)

        assertTrue("Unexpected CSV:\n$result", result.startsWith("name,controller,path,filePath,templates"))
        assertTrue("Unexpected CSV:\n$result", result.contains("my_car_foo_stuff"))
        assertTrue("Unexpected CSV:\n$result", result.contains("CarController::fooAction"))
        assertTrue("Unexpected CSV:\n$result", result.contains("/edit/{id}"))
        assertUsesRealLineBreaks(result)
    }
}
