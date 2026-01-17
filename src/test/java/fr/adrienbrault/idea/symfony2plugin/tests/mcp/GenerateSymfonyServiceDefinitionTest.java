package fr.adrienbrault.idea.symfony2plugin.tests.mcp;

import fr.adrienbrault.idea.symfony2plugin.mcp.service.ServiceDefinitionGenerator;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class GenerateSymfonyServiceDefinitionTest extends SymfonyLightCodeInsightFixtureTestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        myFixture.configureFromExistingVirtualFile(myFixture.copyFileToProject("classes.php"));
    }

    protected String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/action/ui/fixtures";
    }

    public void testGenerateYamlDefault() {
        // generate_symfony_service_definition('Foo\\Bar')
        ServiceDefinitionGenerator generator = new ServiceDefinitionGenerator(getProject());
        String result = generator.generate("Foo\\Bar", ServiceDefinitionGenerator.OutputType.YAML);

        assertNotNull(result);
        assertTrue(result.contains("Foo\\Bar:"));
    }

    public void testGenerateYamlWithClassNameAsIdTrue() {
        // generate_symfony_service_definition('Foo\\Bar', 'yaml', true)
        ServiceDefinitionGenerator generator = new ServiceDefinitionGenerator(getProject());
        String result = generator.generate("Foo\\Bar", ServiceDefinitionGenerator.OutputType.YAML, true);

        assertNotNull(result);
        assertTrue(result.contains("Foo\\Bar:"));
        // Should not have class attribute when using class name as ID
        assertFalse(result.contains("class: Foo\\Bar"));
    }

    public void testGenerateYamlWithClassNameAsIdFalse() {
        // generate_symfony_service_definition('Foo\\Bar', 'yaml', false)
        ServiceDefinitionGenerator generator = new ServiceDefinitionGenerator(getProject());
        String result = generator.generate("Foo\\Bar", ServiceDefinitionGenerator.OutputType.YAML, false);

        assertNotNull(result);
        // Should use snake_case service ID
        assertTrue(result.contains("foo.bar:"));
        // Should have class attribute
        assertTrue(result.contains("class: Foo\\Bar"));
    }

    public void testGenerateXmlDefault() {
        // generate_symfony_service_definition('Foo\\Bar', 'xml')
        ServiceDefinitionGenerator generator = new ServiceDefinitionGenerator(getProject());
        String result = generator.generate("Foo\\Bar", ServiceDefinitionGenerator.OutputType.XML);

        assertNotNull(result);
        assertTrue(result.contains("<service id=\"Foo\\Bar\""));
    }

    public void testGenerateXmlWithClassNameAsIdTrue() {
        // generate_symfony_service_definition('Foo\\Bar', 'xml', true)
        ServiceDefinitionGenerator generator = new ServiceDefinitionGenerator(getProject());
        String result = generator.generate("Foo\\Bar", ServiceDefinitionGenerator.OutputType.XML, true);

        assertNotNull(result);
        assertTrue(result.contains("<service id=\"Foo\\Bar\""));
        // Should not have class attribute when using class name as ID
        assertFalse(result.contains("class="));
    }

    public void testGenerateXmlWithClassNameAsIdFalse() {
        // generate_symfony_service_definition('Foo\\Bar', 'xml', false)
        ServiceDefinitionGenerator generator = new ServiceDefinitionGenerator(getProject());
        String result = generator.generate("Foo\\Bar", ServiceDefinitionGenerator.OutputType.XML, false);

        assertNotNull(result);
        // Should use snake_case service ID
        assertTrue(result.contains("id=\"foo.bar\""));
        // Should have class attribute
        assertTrue(result.contains("class=\"Foo\\Bar\""));
    }

    public void testGenerateReturnsNullForNonExistentClass() {
        // generate_symfony_service_definition('NonExistent\\Class')
        // MCP tool should throw IllegalArgumentException with "Class not found"
        ServiceDefinitionGenerator generator = new ServiceDefinitionGenerator(getProject());
        String result = generator.generate("NonExistent\\Class", ServiceDefinitionGenerator.OutputType.YAML, true);

        assertNull(result);
    }

    public void testGenerateWithLeadingBackslash() {
        // generate_symfony_service_definition('\\Foo\\Bar')
        // Should handle leading backslash gracefully
        ServiceDefinitionGenerator generator = new ServiceDefinitionGenerator(getProject());
        String result = generator.generate("\\Foo\\Bar", ServiceDefinitionGenerator.OutputType.YAML, true);

        assertNotNull(result);
        assertTrue(result.contains("Foo\\Bar:"));
    }
}
