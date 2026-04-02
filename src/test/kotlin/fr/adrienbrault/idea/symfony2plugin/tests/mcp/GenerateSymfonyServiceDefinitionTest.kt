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

    override fun getTestDataPath(): String {
        return "src/test/kotlin/fr/adrienbrault/idea/symfony2plugin/tests/mcp/fixtures"
    }

    private fun configureBaseClassesPhp() {
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

    private fun configureBaseServicesXml() {
        myFixture.addFileToProject(
            "app/config/services.xml",
            """
            <container xmlns="http://symfony.com/schema/dic/services">
                <services>
                    <service id="foo.bar" class="Foo\Bar"/>
                    <service id="app.mailer" class="Foo\Mailer"/>
                </services>
            </container>
            """.trimIndent()
        )
    }

    fun testGenerateYamlDefault() {
        configureBaseClassesPhp()
        val result = serviceDefinitionGenerator.generate(project, "Foo\\Bar", ServiceBuilder.OutputType.Yaml)

        assertNotNull(result)
        assertTrue(result!!.contains("Foo\\Bar:"))
        assertFalse(result.contains("Possible services per parameter:"))
    }

    fun testGenerateYamlWithClassNameAsIdTrue() {
        configureBaseClassesPhp()
        val result = serviceDefinitionGenerator.generate(project, "Foo\\Bar", ServiceBuilder.OutputType.Yaml, true)

        assertNotNull(result)
        assertTrue(result!!.contains("Foo\\Bar:"))
        assertFalse(result.contains("class: Foo\\Bar"))
    }

    fun testGenerateYamlWithClassNameAsIdFalse() {
        configureBaseClassesPhp()
        val result = serviceDefinitionGenerator.generate(project, "Foo\\Bar", ServiceBuilder.OutputType.Yaml, false)

        assertNotNull(result)
        assertTrue(result!!.contains("foo.bar:"))
        assertTrue(result.contains("class: Foo\\Bar"))
    }

    fun testGenerateXmlDefault() {
        configureBaseClassesPhp()
        val result = serviceDefinitionGenerator.generate(project, "Foo\\Bar", ServiceBuilder.OutputType.XML)

        assertNotNull(result)
        assertTrue(result!!.contains("<service id=\"Foo\\Bar\""))
        assertFalse(result.contains("Possible services per parameter:"))
    }

    fun testGenerateXmlWithClassNameAsIdTrue() {
        configureBaseClassesPhp()
        val result = serviceDefinitionGenerator.generate(project, "Foo\\Bar", ServiceBuilder.OutputType.XML, true)

        assertNotNull(result)
        assertTrue(result!!.contains("<service id=\"Foo\\Bar\""))
        assertFalse(result.contains("class="))
    }

    fun testGenerateXmlWithClassNameAsIdFalse() {
        configureBaseClassesPhp()
        val result = serviceDefinitionGenerator.generate(project, "Foo\\Bar", ServiceBuilder.OutputType.XML, false)

        assertNotNull(result)
        assertTrue(result!!.contains("id=\"foo.bar\""))
        assertTrue(result.contains("class=\"Foo\\Bar\""))
    }

    fun testGenerateFluentWithClassNameAsIdTrue() {
        configureBaseClassesPhp()
        val result = serviceDefinitionGenerator.generate(project, "Foo\\Bar", ServiceBuilder.OutputType.Fluent, true)

        assertNotNull(result)
        assertTrue(result!!.contains($$"$services->set(\\Foo\\Bar::class)"))
        assertFalse(result.contains(", \\Foo\\Bar::class"))
        assertFalse(result.contains("Possible services per parameter:"))
    }

    fun testGenerateFluentWithClassNameAsIdFalse() {
        configureBaseClassesPhp()
        val result = serviceDefinitionGenerator.generate(project, "Foo\\Bar", ServiceBuilder.OutputType.Fluent, false)

        assertNotNull(result)
        assertTrue(result!!.contains($$"$services->set('foo.bar', \\Foo\\Bar::class)"))
    }

    fun testGeneratePhpArrayWithClassNameAsIdTrue() {
        configureBaseClassesPhp()
        val result = serviceDefinitionGenerator.generate(project, "Foo\\Bar", ServiceBuilder.OutputType.PhpArray, true)

        assertNotNull(result)
        assertTrue(result!!.contains("\\Foo\\Bar::class => ["))
        assertFalse(result.contains("'class'"))
        assertFalse(result.contains("Possible services per parameter:"))
    }


    fun testGeneratePhpArrayWithClassNameAsIdFalse() {
        configureBaseClassesPhp()
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
        configureBaseClassesPhp()
        val result = serviceDefinitionGenerator.generate(project, "\\Foo\\Bar", ServiceBuilder.OutputType.Yaml, true)

        assertNotNull(result)
        assertTrue(result!!.contains("Foo\\Bar:"))
    }

    fun testCreateModelUsesSharedPrefillLogicForDeclaredTypes() {
        configureBaseClassesPhp()
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

    fun testMcpShowsSuggestionsWhenConstructorTypeHasMultipleMatchingServices() {
        configureBaseClassesPhp()
        myFixture.addFileToProject(
            "app/config/multi_match_services.xml",
            """
            <container xmlns="http://symfony.com/schema/dic/services">
                <services>
                    <service id="foo.bar" class="Foo\Bar"/>
                    <service id="foo.foo" class="Foo\Foo"/>
                </services>
            </container>
            """.trimIndent()
        )

        val result = serviceDefinitionMcpToolset.generateDefinitions(
            project,
            "Foo\\MultiMatchBar",
            ServiceBuilder.OutputType.Yaml,
            true
        )

        assertEquals(
            listOf(
                "Foo\\MultiMatchBar:",
                "    arguments: ['@foo.foo']",
                "",
                "# Possible services per parameter:",
                $$"""# $foo [Foo\Bar|\Foo\Foo] => foo.foo, foo.bar""",
            ),
            result.lines()
        )
    }

    fun testMcpGenerateXmlAppendsSuggestionCommentForMultipleMatchingServices() {
        configureBaseClassesPhp()
        myFixture.addFileToProject(
            "app/config/multi_match_xml_services.xml",
            """
            <container xmlns="http://symfony.com/schema/dic/services">
                <services>
                    <service id="foo.bar" class="Foo\Bar"/>
                    <service id="foo.foo" class="Foo\Foo"/>
                </services>
            </container>
            """.trimIndent()
        )

        val result = serviceDefinitionMcpToolset.generateDefinitions(
            project,
            "Foo\\MultiMatchXml",
            ServiceBuilder.OutputType.XML,
            true
        )

        assertEquals(
            listOf(
                "<service id=\"Foo\\MultiMatchXml\">",
                "  <argument id=\"foo.foo\" type=\"service\"/>",
                "</service>",
                "",
                "",
                "<!-- Possible services per parameter: -->",
                $$"""<!-- $foo [Foo\Bar|\Foo\Foo] => foo.foo, foo.bar -->""",
            ),
            result.lines()
        )
    }

    fun testMcpGenerateFluentAppendsSuggestionCommentForMultipleMatchingServices() {
        configureBaseClassesPhp()
        myFixture.addFileToProject(
            "app/config/multi_match_fluent_services.xml",
            """
            <container xmlns="http://symfony.com/schema/dic/services">
                <services>
                    <service id="foo.bar" class="Foo\Bar"/>
                    <service id="foo.foo" class="Foo\Foo"/>
                </services>
            </container>
            """.trimIndent()
        )

        val result = serviceDefinitionMcpToolset.generateDefinitions(
            project,
            "Foo\\MultiMatchFluent",
            ServiceBuilder.OutputType.Fluent,
            true
        )

        assertEquals(
            listOf(
                $$"""$services->set(\Foo\MultiMatchFluent::class)""",
                "    ->args([",
                "        service('foo.foo'),",
                "    ]);",
                "",
                "// Possible services per parameter:",
                $$"""// $foo [Foo\Bar|\Foo\Foo] => foo.foo, foo.bar""",
            ),
            result.lines()
        )
    }

    fun testMcpShowsOnlyKnownSuggestionLinesWhenFollowingParameterIsUnknown() {
        configureBaseClassesPhp()
        myFixture.addFileToProject(
            "app/config/multi_match_with_unknown_second.xml",
            """
            <container xmlns="http://symfony.com/schema/dic/services">
                <services>
                    <service id="foo.bar" class="Foo\Bar"/>
                    <service id="foo.foo" class="Foo\Foo"/>
                </services>
            </container>
            """.trimIndent()
        )

        val result = serviceDefinitionMcpToolset.generateDefinitions(
            project,
            "Foo\\MultiMatchWithUnknownSecond",
            ServiceBuilder.OutputType.Yaml,
            true
        )

        assertEquals(
            listOf(
                "Foo\\MultiMatchWithUnknownSecond:",
                "    arguments: ['@foo.foo', '@?']",
                "",
                "# Possible services per parameter:",
                $$"""# $foo [Foo\Bar|\Foo\Foo] => foo.foo, foo.bar""",
                $$"""# $unknown [Foo\UnknownType]""",
            ),
            result.lines()
        )
        assertEquals(1, result.lines().count { it == "# Possible services per parameter:" })
        assertEquals(2, result.lines().count { it.startsWith("# $") })
    }

    fun testGenerateMultipleYamlDefinitionsFromCommaSeparatedClassNames() {
        configureBaseClassesPhp()
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

        assertEquals(
            """
            Foo\Bar: ~
            ---
            Foo\Baz: ~
            """.trimIndent(),
            result
        )
    }

    fun testGenerateMultipleYamlDefinitionsIgnoresBlankCommaSeparatedItems() {
        configureBaseClassesPhp()
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

        assertEquals(
            """
            Foo\BazBlank: ~
            """.trimIndent(),
            result
        )
    }

    fun testGenerateMultipleYamlDefinitionsKeepsGoingWhenOneClassIsMissing() {
        configureBaseClassesPhp()
        val result = serviceDefinitionMcpToolset.generateDefinitions(
            project,
            "Foo\\Bar, Missing\\Class",
            ServiceBuilder.OutputType.Yaml,
            true
        )

        assertEquals(
            """
            Foo\Bar: ~
            ---
            Error: Class not found: Missing\Class
            """.trimIndent(),
            result
        )
    }
}
