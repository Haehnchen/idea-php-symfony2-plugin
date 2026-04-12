package fr.adrienbrault.idea.symfony2plugin.tests.mcp

import fr.adrienbrault.idea.symfony2plugin.mcp.collector.TwigTemplateVariablesCollector
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see TwigTemplateVariablesCollector
 */
class TwigTemplateVariablesCollectorTest : SymfonyLightCodeInsightFixtureTestCase() {

    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()
        myFixture.addFileToProject("ide-twig.json",
            "{\"namespaces\":[{\"namespace\":\"\",\"path\":\"templates\"}]}"
        )
    }

    fun testCsvHeaderStructure() {
        val result = TwigTemplateVariablesCollector(project).collect("nonexistent.html.twig")

        assertTrue("Should have CSV header", result.startsWith("variable,type,properties"))
    }

    fun testEmptyResultForNonExistentTemplate() {
        val result = TwigTemplateVariablesCollector(project).collect("nonexistent.html.twig")

        assertEquals("variable,type,properties\n", result)
    }

    fun testSingleVariableWithDocComment() {
        myFixture.addFileToProject("src/Entity/Article.php",
            "<?php\nnamespace App\\Entity;\n" +
            "class Article {\n" +
            "    public function getTitle(): string {}\n" +
            "}\n"
        )
        myFixture.addFileToProject("templates/blog/show.html.twig",
            "{# @var article \\App\\Entity\\Article #}\n" +
            "{{ article.title }}"
        )

        val result = TwigTemplateVariablesCollector(project).collect("blog/show.html.twig")

        assertTrue("Should find article variable\n$result",
            result.contains("article,\\App\\Entity\\Article,")
        )
        assertTrue("Should list 'title' property\n$result", result.contains("title"))
    }

    fun testMagicMethodsAreExcluded() {
        myFixture.addFileToProject("src/Entity/Token.php",
            "<?php\nnamespace App\\Entity;\n" +
            "class Token {\n" +
            "    public function __construct() {}\n" +
            "    public function __toString(): string {}\n" +
            "    public function getValue(): string {}\n" +
            "}\n"
        )
        myFixture.addFileToProject("templates/security/token.html.twig",
            "{# @var token \\App\\Entity\\Token #}\n" +
            "{{ token.value }}"
        )

        val result = TwigTemplateVariablesCollector(project).collect("security/token.html.twig")

        assertFalse("__construct must not appear\n$result", result.contains("__construct"))
        assertFalse("__toString must not appear\n$result", result.contains("__toString"))
        assertTrue("getValue → value should appear\n$result", result.contains("value"))
    }

    fun testPublicFieldsAreIncluded() {
        myFixture.addFileToProject("src/Entity/Config.php",
            "<?php\nnamespace App\\Entity;\n" +
            "class Config {\n" +
            "    public string \$name;\n" +
            "    public bool \$enabled;\n" +
            "    protected string \$internal;\n" +
            "    private int \$secret;\n" +
            "}\n"
        )
        myFixture.addFileToProject("templates/settings/view.html.twig",
            "{# @var config \\App\\Entity\\Config #}\n" +
            "{{ config.name }}"
        )

        val result = TwigTemplateVariablesCollector(project).collect("settings/view.html.twig")

        assertTrue("Public field 'name' should appear\n$result", result.contains("name"))
        assertTrue("Public field 'enabled' should appear\n$result", result.contains("enabled"))
        assertFalse("Protected field 'internal' must not appear\n$result", result.contains("internal"))
        assertFalse("Private field 'secret' must not appear\n$result", result.contains("secret"))
    }

    fun testMultipleTypesOnOneVariableMergesProperties() {
        myFixture.addFileToProject("src/Entity/Cat.php",
            "<?php\nnamespace App\\Entity;\n" +
            "class Cat {\n" +
            "    public function getName(): string {}\n" +
            "}\n"
        )
        myFixture.addFileToProject("src/Entity/Dog.php",
            "<?php\nnamespace App\\Entity;\n" +
            "class Dog {\n" +
            "    public function getName(): string {}\n" +
            "    public function getBreed(): string {}\n" +
            "}\n"
        )
        myFixture.addFileToProject("templates/animal/show.html.twig",
            "{# @var pet \\App\\Entity\\Cat #}\n" +
            "{# @var pet \\App\\Entity\\Dog #}\n" +
            "{{ pet.name }}"
        )

        val result = TwigTemplateVariablesCollector(project).collect("animal/show.html.twig")

        assertTrue("Should contain 'name'\n$result", result.contains("name"))
        assertTrue("Should contain 'breed'\n$result", result.contains("breed"))

        val propertiesCell = result.lines()
            .filter { it.startsWith("pet,") }
            .firstOrNull() ?: ""
        val firstName = propertiesCell.indexOf("name")
        val secondName = propertiesCell.indexOf("name", firstName + 1)
        assertEquals("'name' should be deduplicated in properties\n$propertiesCell", -1, secondName)
    }

    fun testPropertiesAreQuotedWhenContainCommas() {
        myFixture.addFileToProject("src/Entity/Invoice.php",
            "<?php\nnamespace App\\Entity;\n" +
            "class Invoice {\n" +
            "    public function getNumber(): string {}\n" +
            "    public function getTotal(): float {}\n" +
            "}\n"
        )
        myFixture.addFileToProject("templates/invoice/detail.html.twig",
            "{# @var invoice \\App\\Entity\\Invoice #}\n" +
            "{{ invoice.number }}"
        )

        val result = TwigTemplateVariablesCollector(project).collect("invoice/detail.html.twig")

        assertTrue("Properties with comma must be CSV-quoted\n$result",
            result.contains("\"number,total\"") || result.contains("\"total,number\"")
        )
    }

    fun testTemplateVariablesCanFilterByMatchingFileGlob() {
        myFixture.addFileToProject("src/Entity/Article.php",
            "<?php\nnamespace App\\Entity;\n" +
                "class Article {\n" +
                "    public function getTitle(): string {}\n" +
                "}\n"
        )
        myFixture.addFileToProject("templates/blog/show.html.twig",
            "{# @var article \\App\\Entity\\Article #}\n" +
                "{{ article.title }}"
        )

        val result = TwigTemplateVariablesCollector(project).collect(
            "blog/show.html.twig",
            "**/show.html.twig"
        )

        assertTrue("Should find article variable\n$result", result.contains("article,\\App\\Entity\\Article,"))
        assertTrue("Should list 'title' property\n$result", result.contains("title"))
    }

    fun testTemplateVariablesUseOrSemanticsForTemplateAndFileGlob() {
        myFixture.addFileToProject("src/Entity/Article.php",
            "<?php\nnamespace App\\Entity;\n" +
                "class Article {\n" +
                "    public function getTitle(): string {}\n" +
                "}\n"
        )
        myFixture.addFileToProject("templates/blog/show.html.twig",
            "{# @var article \\App\\Entity\\Article #}\n" +
                "{{ article.title }}"
        )

        val result = TwigTemplateVariablesCollector(project).collect(
            "blog/show.html.twig",
            "templates/admin/**/*.html.twig"
        )

        assertTrue("Template match should still return variables even when fileGlob does not match\n$result",
            result.contains("article,\\App\\Entity\\Article,")
        )
    }

    fun testTemplateVariablesSupportCommaSeparatedTemplateList() {
        myFixture.addFileToProject("src/Entity/Article.php",
            "<?php\nnamespace App\\Entity;\n" +
                "class Article {\n" +
                "    public function getTitle(): string {}\n" +
                "}\n"
        )
        myFixture.addFileToProject("src/Entity/User.php",
            "<?php\nnamespace App\\Entity;\n" +
                "class User {\n" +
                "    public function getEmail(): string {}\n" +
                "}\n"
        )
        myFixture.addFileToProject("templates/blog/show.html.twig",
            "{# @var article \\App\\Entity\\Article #}\n" +
                "{{ article.title }}"
        )
        myFixture.addFileToProject("templates/user/profile.html.twig",
            "{# @var user \\App\\Entity\\User #}\n" +
                "{{ user.email }}"
        )

        val result = TwigTemplateVariablesCollector(project).collect(
            "blog/show.html.twig, user/profile.html.twig"
        )

        assertTrue("Should include first template header\n$result", result.contains("template: blog/show.html.twig => (file: templates/blog/show.html.twig)"))
        assertTrue("Should include second template header\n$result", result.contains("template: user/profile.html.twig => (file: templates/user/profile.html.twig)"))
        assertTrue("Should include article row\n$result", result.contains("article,\\App\\Entity\\Article,"))
        assertTrue("Should include user row\n$result", result.contains("user,\\App\\Entity\\User,"))
    }

    fun testTemplateVariablesSupportFileGlobWithoutTemplateInput() {
        myFixture.addFileToProject("src/Entity/Article.php",
            "<?php\nnamespace App\\Entity;\n" +
                "class Article {\n" +
                "    public function getTitle(): string {}\n" +
                "}\n"
        )
        myFixture.addFileToProject("templates/blog/show.html.twig",
            "{# @var article \\App\\Entity\\Article #}\n" +
                "{{ article.title }}"
        )

        val result = TwigTemplateVariablesCollector(project).collect(
            fileGlob = "templates/blog/**/*.html.twig"
        )

        assertTrue("Should resolve template via file glob\n$result", result.contains("article,\\App\\Entity\\Article,"))
        assertTrue("Should include title property\n$result", result.contains("title"))
    }
}
