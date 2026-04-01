package fr.adrienbrault.idea.symfony2plugin.tests.mcp

import fr.adrienbrault.idea.symfony2plugin.mcp.service.ServiceDefinitionGenerator
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
class GenerateSymfonyServiceDefinitionTest : SymfonyLightCodeInsightFixtureTestCase() {

    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()
        myFixture.configureFromExistingVirtualFile(myFixture.copyFileToProject("classes.php"))
    }

    override fun getTestDataPath(): String {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/action/ui/fixtures"
    }

    fun testGenerateYamlDefault() {
        val generator = ServiceDefinitionGenerator(project)
        val result = generator.generate("Foo\\Bar", ServiceDefinitionGenerator.OutputType.YAML)

        assertNotNull(result)
        assertTrue(result!!.contains("Foo\\Bar:"))
    }

    fun testGenerateYamlWithClassNameAsIdTrue() {
        val generator = ServiceDefinitionGenerator(project)
        val result = generator.generate("Foo\\Bar", ServiceDefinitionGenerator.OutputType.YAML, true)

        assertNotNull(result)
        assertTrue(result!!.contains("Foo\\Bar:"))
        assertFalse(result.contains("class: Foo\\Bar"))
    }

    fun testGenerateYamlWithClassNameAsIdFalse() {
        val generator = ServiceDefinitionGenerator(project)
        val result = generator.generate("Foo\\Bar", ServiceDefinitionGenerator.OutputType.YAML, false)

        assertNotNull(result)
        assertTrue(result!!.contains("foo.bar:"))
        assertTrue(result.contains("class: Foo\\Bar"))
    }

    fun testGenerateXmlDefault() {
        val generator = ServiceDefinitionGenerator(project)
        val result = generator.generate("Foo\\Bar", ServiceDefinitionGenerator.OutputType.XML)

        assertNotNull(result)
        assertTrue(result!!.contains("<service id=\"Foo\\Bar\""))
    }

    fun testGenerateXmlWithClassNameAsIdTrue() {
        val generator = ServiceDefinitionGenerator(project)
        val result = generator.generate("Foo\\Bar", ServiceDefinitionGenerator.OutputType.XML, true)

        assertNotNull(result)
        assertTrue(result!!.contains("<service id=\"Foo\\Bar\""))
        assertFalse(result.contains("class="))
    }

    fun testGenerateXmlWithClassNameAsIdFalse() {
        val generator = ServiceDefinitionGenerator(project)
        val result = generator.generate("Foo\\Bar", ServiceDefinitionGenerator.OutputType.XML, false)

        assertNotNull(result)
        assertTrue(result!!.contains("id=\"foo.bar\""))
        assertTrue(result.contains("class=\"Foo\\Bar\""))
    }

    fun testGenerateFluentWithClassNameAsIdTrue() {
        val generator = ServiceDefinitionGenerator(project)
        val result = generator.generate("Foo\\Bar", ServiceDefinitionGenerator.OutputType.FLUENT, true)

        assertNotNull(result)
        assertTrue(result!!.contains("\$services->set(\\Foo\\Bar::class)"))
        assertFalse(result.contains(", \\Foo\\Bar::class"))
    }

    fun testGenerateFluentWithClassNameAsIdFalse() {
        val generator = ServiceDefinitionGenerator(project)
        val result = generator.generate("Foo\\Bar", ServiceDefinitionGenerator.OutputType.FLUENT, false)

        assertNotNull(result)
        assertTrue(result!!.contains("\$services->set('foo.bar', \\Foo\\Bar::class)"))
    }

    fun testGeneratePhpArrayWithClassNameAsIdTrue() {
        val generator = ServiceDefinitionGenerator(project)
        val result = generator.generate("Foo\\Bar", ServiceDefinitionGenerator.OutputType.PHP_ARRAY, true)

        assertNotNull(result)
        assertTrue(result!!.contains("\\Foo\\Bar::class => ["))
        assertFalse(result.contains("'class'"))
    }

    fun testGeneratePhpArrayWithClassNameAsIdFalse() {
        val generator = ServiceDefinitionGenerator(project)
        val result = generator.generate("Foo\\Bar", ServiceDefinitionGenerator.OutputType.PHP_ARRAY, false)

        assertNotNull(result)
        assertTrue(result!!.contains("'foo.bar' => ["))
        assertTrue(result.contains("'class' => \\Foo\\Bar::class"))
    }

    fun testGenerateReturnsNullForNonExistentClass() {
        val generator = ServiceDefinitionGenerator(project)
        val result = generator.generate("NonExistent\\Class", ServiceDefinitionGenerator.OutputType.YAML, true)

        assertNull(result)
    }

    fun testGenerateWithLeadingBackslash() {
        val generator = ServiceDefinitionGenerator(project)
        val result = generator.generate("\\Foo\\Bar", ServiceDefinitionGenerator.OutputType.YAML, true)

        assertNotNull(result)
        assertTrue(result!!.contains("Foo\\Bar:"))
    }
}
