package fr.adrienbrault.idea.symfony2plugin.tests.mcp

import fr.adrienbrault.idea.symfony2plugin.mcp.collector.SymfonyFormTypeOptionsCollector

class SymfonyFormTypeOptionsCollectorTest : McpCollectorTestCase() {
    fun testCollectReturnsInheritedAndExtensionOptions() {
        val result = SymfonyFormTypeOptionsCollector(project).collect("foo")

        assertTrue("Unexpected CSV:\n$result", result.startsWith("name,type,source"))
        assertTrue("Unexpected CSV:\n$result", result.contains("MyType"))
        assertTrue("Unexpected CSV:\n$result", result.contains("BarTypeParent"))
        assertTrue("Unexpected CSV:\n$result", result.contains("BarTypeExtension"))
        assertUsesRealLineBreaks(result)
    }
}
