package fr.adrienbrault.idea.symfony2plugin.tests.mcp

import fr.adrienbrault.idea.symfony2plugin.action.ui.ServiceBuilder
import fr.adrienbrault.idea.symfony2plugin.mcp.service.ServiceDefinitionGenerator
import fr.adrienbrault.idea.symfony2plugin.mcp.toolset.ServiceDefinitionMcpToolset
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
class GenerateSymfonyServiceDefinitionTest : SymfonyLightCodeInsightFixtureTestCase() {
    private val serviceDefinitionGenerator = ServiceDefinitionGenerator()
    private val serviceDefinitionMcpToolset = ServiceDefinitionMcpToolset()

    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()
        myFixture.configureFromExistingVirtualFile(myFixture.copyFileToProject("classes.php"))
        myFixture.addFileToProject(
            "app/config/services.xml",
            """
            <container xmlns="http://symfony.com/schema/dic/services">
                <services>
                    <service id="foo.bar" class="Foo\Bar"/>
                </services>
            </container>
            """.trimIndent()
        )
    }

    override fun getTestDataPath(): String {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/action/ui/fixtures"
    }

    fun testGenerateYamlDefault() {
        val result = serviceDefinitionGenerator.generate(project, "Foo\\Bar", ServiceBuilder.OutputType.Yaml)

        assertNotNull(result)
        assertTrue(result!!.contains("Foo\\Bar:"))
    }

    fun testGenerateYamlWithClassNameAsIdTrue() {
        val result = serviceDefinitionGenerator.generate(project, "Foo\\Bar", ServiceBuilder.OutputType.Yaml, true)

        assertNotNull(result)
        assertTrue(result!!.contains("Foo\\Bar:"))
        assertFalse(result.contains("class: Foo\\Bar"))
    }

    fun testGenerateYamlWithClassNameAsIdFalse() {
        val result = serviceDefinitionGenerator.generate(project, "Foo\\Bar", ServiceBuilder.OutputType.Yaml, false)

        assertNotNull(result)
        assertTrue(result!!.contains("foo.bar:"))
        assertTrue(result.contains("class: Foo\\Bar"))
    }

    fun testGenerateXmlDefault() {
        val result = serviceDefinitionGenerator.generate(project, "Foo\\Bar", ServiceBuilder.OutputType.XML)

        assertNotNull(result)
        assertTrue(result!!.contains("<service id=\"Foo\\Bar\""))
    }

    fun testGenerateXmlWithClassNameAsIdTrue() {
        val result = serviceDefinitionGenerator.generate(project, "Foo\\Bar", ServiceBuilder.OutputType.XML, true)

        assertNotNull(result)
        assertTrue(result!!.contains("<service id=\"Foo\\Bar\""))
        assertFalse(result.contains("class="))
    }

    fun testGenerateXmlWithClassNameAsIdFalse() {
        val result = serviceDefinitionGenerator.generate(project, "Foo\\Bar", ServiceBuilder.OutputType.XML, false)

        assertNotNull(result)
        assertTrue(result!!.contains("id=\"foo.bar\""))
        assertTrue(result.contains("class=\"Foo\\Bar\""))
    }

    fun testGenerateFluentWithClassNameAsIdTrue() {
        val result = serviceDefinitionGenerator.generate(project, "Foo\\Bar", ServiceBuilder.OutputType.Fluent, true)

        assertNotNull(result)
        assertTrue(result!!.contains("\$services->set(\\Foo\\Bar::class)"))
        assertFalse(result.contains(", \\Foo\\Bar::class"))
    }

    fun testGenerateFluentWithClassNameAsIdFalse() {
        val result = serviceDefinitionGenerator.generate(project, "Foo\\Bar", ServiceBuilder.OutputType.Fluent, false)

        assertNotNull(result)
        assertTrue(result!!.contains("\$services->set('foo.bar', \\Foo\\Bar::class)"))
    }

    fun testGeneratePhpArrayWithClassNameAsIdTrue() {
        val result = serviceDefinitionGenerator.generate(project, "Foo\\Bar", ServiceBuilder.OutputType.PhpArray, true)

        assertNotNull(result)
        assertTrue(result!!.contains("\\Foo\\Bar::class => ["))
        assertFalse(result.contains("'class'"))
    }

    fun testGeneratePhpArrayWithClassNameAsIdFalse() {
        val result = serviceDefinitionGenerator.generate(project, "Foo\\Bar", ServiceBuilder.OutputType.PhpArray, false)

        assertNotNull(result)
        assertTrue(result!!.contains("'foo.bar' => ["))
        assertTrue(result.contains("'class' => \\Foo\\Bar::class"))
    }

    fun testGenerateReturnsNullForNonExistentClass() {
        val result = serviceDefinitionGenerator.generate(project, "NonExistent\\Class", ServiceBuilder.OutputType.Yaml, true)

        assertNull(result)
    }

    fun testGenerateWithLeadingBackslash() {
        val result = serviceDefinitionGenerator.generate(project, "\\Foo\\Bar", ServiceBuilder.OutputType.Yaml, true)

        assertNotNull(result)
        assertTrue(result!!.contains("Foo\\Bar:"))
    }

    fun testCreateModelUsesSharedPrefillLogicForDeclaredTypes() {
        myFixture.addFileToProject(
            "union_types.php",
            """
            <?php

            namespace Foo {
                class UnionBar
                {
                    public function __construct(Bar|string ${'$'}bar)
                    {
                    }
                }
            }
            """.trimIndent()
        )

        val result = serviceDefinitionGenerator.createModel(project, "Foo\\UnionBar")

        assertNotNull(result)
        assertEquals(1, result!!.modelParameters.size)
        assertEquals("foo.bar", result.modelParameters[0].currentService)
    }

    fun testGenerateMultipleYamlDefinitionsFromCommaSeparatedClassNames() {
        myFixture.addFileToProject(
            "multiple_classes.php",
            """
            <?php

            namespace Foo {
                class Baz
                {
                }
            }
            """.trimIndent()
        )

        val result = serviceDefinitionMcpToolset.generateDefinitions(
            project,
            "Foo\\Bar, Foo\\Baz",
            ServiceBuilder.OutputType.Yaml,
            true
        )

        assertTrue(result.contains("Foo\\Bar:"))
        assertTrue(result.contains("Foo\\Baz:"))
        assertTrue(result.contains("\n---\n"))
    }

    fun testGenerateMultipleYamlDefinitionsIgnoresBlankCommaSeparatedItems() {
        myFixture.addFileToProject(
            "multiple_classes_blank_items.php",
            """
            <?php

            namespace Foo {
                class BazBlank
                {
                }
            }
            """.trimIndent()
        )

        val result = serviceDefinitionMcpToolset.generateDefinitions(
            project,
            " , Foo\\BazBlank , ",
            ServiceBuilder.OutputType.Yaml,
            true
        )

        assertEquals("Foo\\BazBlank: ~", result)
    }

    fun testGenerateMultipleYamlDefinitionsKeepsGoingWhenOneClassIsMissing() {
        val result = serviceDefinitionMcpToolset.generateDefinitions(
            project,
            "Foo\\Bar, Missing\\Class",
            ServiceBuilder.OutputType.Yaml,
            true
        )

        assertTrue(result.contains("Foo\\Bar:"))
        assertTrue(result.contains("Error: Class not found: Missing\\Class"))
        assertTrue(result.contains("\n---\n"))
    }
}
