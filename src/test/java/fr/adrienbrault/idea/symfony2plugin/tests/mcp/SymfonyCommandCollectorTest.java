package fr.adrienbrault.idea.symfony2plugin.tests.mcp;

import fr.adrienbrault.idea.symfony2plugin.mcp.collector.SymfonyCommandCollector;

public class SymfonyCommandCollectorTest extends McpCollectorTestCase {

    /**
     * Verifies the CSV header and that a command registered via setName() is exported
     * with its FQN and a non-empty file path.
     */
    public void testCollectExportsSetNameCommand() {
        String result = new SymfonyCommandCollector(getProject()).collect();

        assertTrue("CSV must start with header", result.startsWith("name,className,filePath"));
        assertTrue("Must contain command name 'foo'", result.contains("foo"));
        assertTrue("Must contain FQN \\Foo\\FooCommand", result.contains("\\Foo\\FooCommand"));
        assertTrue("Must contain source file path", result.contains("SymfonyCommandUtilTest.php"));
    }

    /**
     * Verifies that a command registered via #[AsCommand('app:create-user-1')] (positional arg) is exported.
     */
    public void testCollectExportsAsCommandAttributePositional() {
        String result = new SymfonyCommandCollector(getProject()).collect();

        assertTrue("Must contain 'app:create-user-1'", result.contains("app:create-user-1"));
        assertTrue("Must contain FQN \\Foo\\FoobarCommand1", result.contains("\\Foo\\FoobarCommand1"));
    }

    /**
     * Verifies that a command registered via #[AsCommand(name: 'app:create-user-2')] (named arg) is exported.
     */
    public void testCollectExportsAsCommandAttributeNamed() {
        String result = new SymfonyCommandCollector(getProject()).collect();

        assertTrue("Must contain 'app:create-user-2'", result.contains("app:create-user-2"));
        assertTrue("Must contain FQN \\Foo\\FoobarCommand2", result.contains("\\Foo\\FoobarCommand2"));
    }

    /**
     * Verifies that a command registered via protected static $defaultName is exported.
     */
    public void testCollectExportsDefaultNameProperty() {
        String result = new SymfonyCommandCollector(getProject()).collect();

        assertTrue("Must contain 'app:create-user-3'", result.contains("app:create-user-3"));
        assertTrue("Must contain FQN \\Foo\\FoobarCommand3", result.contains("\\Foo\\FoobarCommand3"));
    }

    /**
     * Verifies that a command whose setName() resolves via an instance property ($this->name) is exported.
     */
    public void testCollectExportsPropertyResolvedCommand() {
        String result = new SymfonyCommandCollector(getProject()).collect();

        assertTrue("Must contain command name 'property'", result.contains("property"));
        assertTrue("Must contain FQN \\Foo\\PropertyCommand", result.contains("\\Foo\\PropertyCommand"));
    }

    /**
     * Verifies that a command whose setName() resolves via a class constant (self::FOO) is exported.
     */
    public void testCollectExportsConstantResolvedCommand() {
        String result = new SymfonyCommandCollector(getProject()).collect();

        assertTrue("Must contain command name 'const'", result.contains("const"));
        assertTrue("Must contain FQN \\Foo\\ConstCommand", result.contains("\\Foo\\ConstCommand"));
    }
}
