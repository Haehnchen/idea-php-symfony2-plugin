package fr.adrienbrault.idea.symfony2plugin.tests.mcp;

import fr.adrienbrault.idea.symfony2plugin.mcp.collector.SymfonyServiceLocatorCollector;

public class SymfonyServiceLocatorCollectorTest extends McpCollectorTestCase {
    public void testCollectResolvesByServiceNameAndClass() {
        String byServiceName = new SymfonyServiceLocatorCollector(getProject()).collect("foo.yml_id");
        assertTrue("Unexpected CSV:\n" + byServiceName, byServiceName.startsWith("serviceName,className,filePath,lineNumber"));
        assertTrue("Unexpected CSV:\n" + byServiceName, byServiceName.contains("foo.yml_id"));
        assertTrue("Unexpected CSV:\n" + byServiceName, byServiceName.contains("My\\Foo\\Service\\Targets"));
        assertTrue("Unexpected CSV:\n" + byServiceName, byServiceName.contains("services.yml"));

        String byClassName = new SymfonyServiceLocatorCollector(getProject()).collect("My\\Foo\\Service\\Targets");
        assertTrue("Unexpected CSV:\n" + byClassName, byClassName.contains("foo.yml_id"));
        assertTrue("Unexpected CSV:\n" + byClassName, byClassName.contains("foo.xml_id"));
    }
}
