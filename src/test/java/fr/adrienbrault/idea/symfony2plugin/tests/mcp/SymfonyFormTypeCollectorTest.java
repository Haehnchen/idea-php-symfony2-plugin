package fr.adrienbrault.idea.symfony2plugin.tests.mcp;

import fr.adrienbrault.idea.symfony2plugin.mcp.collector.SymfonyFormTypeCollector;

public class SymfonyFormTypeCollectorTest extends McpCollectorTestCase {

    /**
     * Verifies the CSV header is correct.
     */
    public void testCollectHasCorrectHeader() {
        String result = new SymfonyFormTypeCollector(getProject()).collect();

        assertTrue("CSV must start with header", result.startsWith("name,className,filePath"));
    }

    /**
     * Verifies that a form type whose getName() returns a string literal is exported
     * with its name, FQN, and source file path.
     */
    public void testCollectExportsStringLiteralFormType() {
        String result = new SymfonyFormTypeCollector(getProject()).collect();

        assertTrue("Must contain form type name 'foo_type'", result.contains("foo_type"));
        assertTrue("Must contain FQN \\Form\\FormType\\Foo", result.contains("\\Form\\FormType\\Foo"));
        assertTrue("Must contain source file path", result.contains("classes.php"));
    }

    /**
     * Verifies that a form type whose getName() returns a class constant is exported.
     */
    public void testCollectExportsClassConstantFormType() {
        String result = new SymfonyFormTypeCollector(getProject()).collect();

        assertTrue("Must contain form type name 'foo_bar'", result.contains("foo_bar"));
        assertTrue("Must contain FQN \\Form\\FormType\\FooBar", result.contains("\\Form\\FormType\\FooBar"));
    }

    /**
     * Verifies that form types from additional fixture files are also collected.
     * Options\Bar\Foo implements FormTypeInterface with getName() returning 'foo'.
     */
    public void testCollectExportsFormTypesFromMultipleFixtureFiles() {
        String result = new SymfonyFormTypeCollector(getProject()).collect();

        assertTrue("Must contain form type name 'foo' from Options\\Bar\\Foo", result.contains("foo"));
        assertTrue("Must contain form type name 'foo_parent' from Options\\Bar\\FooParent", result.contains("foo_parent"));
    }
}
