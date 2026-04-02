package fr.adrienbrault.idea.symfony2plugin.tests.mcp

import fr.adrienbrault.idea.symfony2plugin.mcp.collector.SymfonyRouteCollector

class SymfonyRouteCollectorTest : McpCollectorTestCase() {
    fun testCollectReturnsControllerAndPath() {
        val result = SymfonyRouteCollector(project).collect(null, null, null)

        assertTrue("Unexpected CSV:\n$result", result.startsWith("name,controller,path,filePath,lineNumber,templates"))
        assertTrue("Unexpected CSV:\n$result", result.contains("my_car_foo_stuff"))
        assertTrue("Unexpected CSV:\n$result", result.contains("CarController::fooAction"))
        assertTrue("Unexpected CSV:\n$result", result.contains("src/Controller/RouteHelper.php,30,"))
        assertTrue("Unexpected CSV:\n$result", result.contains("/edit/{id}"))
        assertUsesRealLineBreaks(result)
    }

    fun testCollectCanFilterByFullRequestUrl() {
        val result = SymfonyRouteCollector(project).collect(urlPath = "/edit/12")

        assertTrue("Unexpected CSV:\n$result", result.startsWith("name,controller,path,filePath,lineNumber,templates"))
        assertTrue("Unexpected CSV:\n$result", result.contains("my_car_foo_stuff"))
        assertTrue("Unexpected CSV:\n$result", result.contains("src/Controller/RouteHelper.php,30,"))
        assertTrue("Unexpected CSV:\n$result", result.contains("/edit/{id}"))
        assertUsesRealLineBreaks(result)
    }

    fun testCollectCanFilterByPartialUrlPath() {
        val result = SymfonyRouteCollector(project).collect(urlPath = "/edit")

        assertTrue("Unexpected CSV:\n$result", result.contains("my_car_foo_stuff"))
        assertTrue("Unexpected CSV:\n$result", result.contains("/edit/{id}"))
        assertUsesRealLineBreaks(result)
    }
}
