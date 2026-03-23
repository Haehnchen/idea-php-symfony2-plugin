package fr.adrienbrault.idea.symfony2plugin.tests.mcp

import fr.adrienbrault.idea.symfony2plugin.mcp.collector.SymfonyServiceLocatorCollector

class SymfonyServiceLocatorCollectorTest : McpCollectorTestCase() {
    fun testCollectResolvesByServiceNameAndClass() {
        val byServiceName = SymfonyServiceLocatorCollector(project).collect("foo.yml_id")
        assertTrue("Unexpected CSV:\n$byServiceName", byServiceName.startsWith("serviceName,className,filePath,lineNumber"))
        assertTrue("Unexpected CSV:\n$byServiceName", byServiceName.contains("foo.yml_id"))
        assertTrue("Unexpected CSV:\n$byServiceName", byServiceName.contains("My\\Foo\\Service\\Targets"))
        assertTrue("Unexpected CSV:\n$byServiceName", byServiceName.contains("services.yml"))
        assertUsesRealLineBreaks(byServiceName)

        val byClassName = SymfonyServiceLocatorCollector(project).collect("My\\Foo\\Service\\Targets")
        assertTrue("Unexpected CSV:\n$byClassName", byClassName.contains("foo.yml_id"))
        assertTrue("Unexpected CSV:\n$byClassName", byClassName.contains("foo.xml_id"))
        assertUsesRealLineBreaks(byClassName)
    }
}
