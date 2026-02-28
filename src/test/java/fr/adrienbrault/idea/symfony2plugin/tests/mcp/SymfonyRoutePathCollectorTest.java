package fr.adrienbrault.idea.symfony2plugin.tests.mcp;

import fr.adrienbrault.idea.symfony2plugin.mcp.collector.SymfonyRoutePathCollector;

public class SymfonyRoutePathCollectorTest extends McpCollectorTestCase {
    public void testCollectMatchesDynamicPath() {
        String result = new SymfonyRoutePathCollector(getProject()).collect("/edit/12");

        assertTrue("Unexpected CSV:\n" + result, result.startsWith("name,controller,path,filePath"));
        assertTrue("Unexpected CSV:\n" + result, result.contains("my_car_foo_stuff"));
        assertTrue("Unexpected CSV:\n" + result, result.contains("CarController::fooAction"));
    }
}
