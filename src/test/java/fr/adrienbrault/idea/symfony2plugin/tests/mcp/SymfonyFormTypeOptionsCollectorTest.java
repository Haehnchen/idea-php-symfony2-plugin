package fr.adrienbrault.idea.symfony2plugin.tests.mcp;

import fr.adrienbrault.idea.symfony2plugin.mcp.collector.SymfonyFormTypeOptionsCollector;

public class SymfonyFormTypeOptionsCollectorTest extends McpCollectorTestCase {
    public void testCollectReturnsInheritedAndExtensionOptions() {
        String result = new SymfonyFormTypeOptionsCollector(getProject()).collect("foo");

        assertTrue("Unexpected CSV:\n" + result, result.startsWith("name,type,source"));
        assertTrue("Unexpected CSV:\n" + result, result.contains("MyType"));
        assertTrue("Unexpected CSV:\n" + result, result.contains("BarTypeParent"));
        assertTrue("Unexpected CSV:\n" + result, result.contains("BarTypeExtension"));
    }
}
