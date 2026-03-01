package fr.adrienbrault.idea.symfony2plugin.tests.mcp;

import fr.adrienbrault.idea.symfony2plugin.mcp.collector.TwigTemplateVariablesCollector;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see TwigTemplateVariablesCollector
 */
public class TwigTemplateVariablesCollectorTest extends SymfonyLightCodeInsightFixtureTestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        myFixture.addFileToProject("ide-twig.json",
            "{\"namespaces\":[{\"namespace\":\"\",\"path\":\"templates\"}]}"
        );
    }

    public void testCsvHeaderStructure() {
        String result = new TwigTemplateVariablesCollector(getProject()).collect("nonexistent");
        assertEquals("variable,type,properties\n", result);
    }

    public void testTemplateNotFoundReturnsHeaderOnly() {
        String result = new TwigTemplateVariablesCollector(getProject()).collect("nonexistent/template.html.twig");
        assertEquals("variable,type,properties\n", result);
    }

    /**
     * A template with a single @var docblock produces one variable row.
     * The type column contains the FQN; properties lists shortcut method names and public fields.
     */
    public void testBasicVariableWithTypeAndProperties() {
        myFixture.addFileToProject("src/Entity/User.php",
            "<?php\nnamespace App\\Entity;\n" +
            "class User {\n" +
            "    public int $id;\n" +
            "    public function getEmail(): string {}\n" +
            "    public function isActive(): bool {}\n" +
            "    private function getSecret(): string {}\n" +
            "}\n"
        );
        myFixture.addFileToProject("templates/user/profile.html.twig",
            "{# @var user \\App\\Entity\\User #}\n" +
            "{{ user.email }}"
        );

        String result = new TwigTemplateVariablesCollector(getProject()).collect("user/profile.html.twig");

        assertTrue("Should have header\n" + result, result.startsWith("variable,type,properties\n"));
        assertTrue("Should contain user variable\n" + result, result.contains("user,\\App\\Entity\\User,"));
        // getEmail → email, isActive → active, public field id
        assertTrue("Should include shortcut property 'email'\n" + result, result.contains("email"));
        assertTrue("Should include shortcut property 'active'\n" + result, result.contains("active"));
        assertTrue("Should include public field 'id'\n" + result, result.contains("id"));
        // private method should NOT appear
        assertFalse("Private method 'getSecret' must not appear\n" + result, result.contains("secret"));
    }

    /**
     * A primitive-typed variable (string) has an empty properties column.
     */
    public void testPrimitiveTypeHasEmptyProperties() {
        myFixture.addFileToProject("templates/page/title.html.twig",
            "{# @var title string #}\n" +
            "{{ title }}"
        );

        String result = new TwigTemplateVariablesCollector(getProject()).collect("page/title.html.twig");

        assertTrue("Should have header\n" + result, result.startsWith("variable,type,properties\n"));
        // The row: "title,string,\n" — properties column empty
        assertTrue("Primitive type should have empty properties\n" + result,
            result.contains("title,string,\n")
        );
    }

    /**
     * Array type (\App\Entity\Product[]) — the type column keeps the [] suffix;
     * properties are resolved from the base class (strip []).
     */
    public void testArrayTypeStripsSquareBracketsForPropertyResolution() {
        myFixture.addFileToProject("src/Entity/Product.php",
            "<?php\nnamespace App\\Entity;\n" +
            "class Product {\n" +
            "    public function getTitle(): string {}\n" +
            "    public function getPrice(): float {}\n" +
            "}\n"
        );
        myFixture.addFileToProject("templates/shop/list.html.twig",
            "{# @var products \\App\\Entity\\Product[] #}\n" +
            "{% for product in products %}{{ product.title }}{% endfor %}"
        );

        String result = new TwigTemplateVariablesCollector(getProject()).collect("shop/list.html.twig");

        // Type column must keep the [] suffix
        assertTrue("Type should include [] suffix\n" + result, result.contains("\\App\\Entity\\Product[]"));
        // Properties from the base class
        assertTrue("Should include 'title'\n" + result, result.contains("title"));
        assertTrue("Should include 'price'\n" + result, result.contains("price"));
    }

    /**
     * Multiple variables are output sorted alphabetically by name.
     */
    public void testMultipleVariablesSortedAlphabetically() {
        myFixture.addFileToProject("templates/dashboard/index.html.twig",
            "{# @var zebra string #}\n" +
            "{# @var apple string #}\n" +
            "{# @var mango string #}\n" +
            "{{ zebra }} {{ apple }} {{ mango }}"
        );

        String result = new TwigTemplateVariablesCollector(getProject()).collect("dashboard/index.html.twig");

        int applePos = result.indexOf("apple,");
        int mangoPos = result.indexOf("mango,");
        int zebraPos = result.indexOf("zebra,");

        assertTrue("apple should appear before mango\n" + result, applePos < mangoPos);
        assertTrue("mango should appear before zebra\n" + result, mangoPos < zebraPos);
    }

    /**
     * Resolving by logical template name finds variables defined in the template.
     * (Path-based resolution via project.getBaseDir() is not tested here because
     * the light test VFS does not align project.getBaseDir() with the fixture content root.)
     */
    public void testLogicalTemplateNameResolution() {
        myFixture.addFileToProject("src/Entity/Article.php",
            "<?php\nnamespace App\\Entity;\n" +
            "class Article {\n" +
            "    public function getTitle(): string {}\n" +
            "}\n"
        );
        myFixture.addFileToProject("templates/blog/show.html.twig",
            "{# @var article \\App\\Entity\\Article #}\n" +
            "{{ article.title }}"
        );

        // "blog/show.html.twig" is the logical name (the ide-twig.json maps "templates/" → "")
        String result = new TwigTemplateVariablesCollector(getProject()).collect("blog/show.html.twig");

        assertTrue("Should find article variable\n" + result,
            result.contains("article,\\App\\Entity\\Article,")
        );
        assertTrue("Should list 'title' property\n" + result, result.contains("title"));
    }

    /**
     * Magic __* methods and the constructor must not appear in the properties column.
     */
    public void testMagicMethodsAreExcluded() {
        myFixture.addFileToProject("src/Entity/Token.php",
            "<?php\nnamespace App\\Entity;\n" +
            "class Token {\n" +
            "    public function __construct() {}\n" +
            "    public function __toString(): string {}\n" +
            "    public function getValue(): string {}\n" +
            "}\n"
        );
        myFixture.addFileToProject("templates/security/token.html.twig",
            "{# @var token \\App\\Entity\\Token #}\n" +
            "{{ token.value }}"
        );

        String result = new TwigTemplateVariablesCollector(getProject()).collect("security/token.html.twig");

        assertFalse("__construct must not appear\n" + result, result.contains("__construct"));
        assertFalse("__toString must not appear\n" + result, result.contains("__toString"));
        assertTrue("getValue → value should appear\n" + result, result.contains("value"));
    }

    /**
     * Public fields on the class appear in the properties column.
     */
    public void testPublicFieldsAreIncluded() {
        myFixture.addFileToProject("src/Entity/Config.php",
            "<?php\nnamespace App\\Entity;\n" +
            "class Config {\n" +
            "    public string $name;\n" +
            "    public bool $enabled;\n" +
            "    protected string $internal;\n" +
            "    private int $secret;\n" +
            "}\n"
        );
        myFixture.addFileToProject("templates/settings/view.html.twig",
            "{# @var config \\App\\Entity\\Config #}\n" +
            "{{ config.name }}"
        );

        String result = new TwigTemplateVariablesCollector(getProject()).collect("settings/view.html.twig");

        assertTrue("Public field 'name' should appear\n" + result, result.contains("name"));
        assertTrue("Public field 'enabled' should appear\n" + result, result.contains("enabled"));
        assertFalse("Protected field 'internal' must not appear\n" + result, result.contains("internal"));
        assertFalse("Private field 'secret' must not appear\n" + result, result.contains("secret"));
    }

    /**
     * Properties from multiple types on the same variable are merged and deduplicated.
     */
    public void testMultipleTypesOnOneVariableMergesProperties() {
        myFixture.addFileToProject("src/Entity/Cat.php",
            "<?php\nnamespace App\\Entity;\n" +
            "class Cat {\n" +
            "    public function getName(): string {}\n" +
            "}\n"
        );
        myFixture.addFileToProject("src/Entity/Dog.php",
            "<?php\nnamespace App\\Entity;\n" +
            "class Dog {\n" +
            "    public function getName(): string {}\n" +
            "    public function getBreed(): string {}\n" +
            "}\n"
        );
        myFixture.addFileToProject("templates/animal/show.html.twig",
            "{# @var pet \\App\\Entity\\Cat #}\n" +
            "{# @var pet \\App\\Entity\\Dog #}\n" +
            "{{ pet.name }}"
        );

        String result = new TwigTemplateVariablesCollector(getProject()).collect("animal/show.html.twig");

        // 'name' from both Cat and Dog (deduplicated), 'breed' from Dog
        assertTrue("Should contain 'name'\n" + result, result.contains("name"));
        assertTrue("Should contain 'breed'\n" + result, result.contains("breed"));

        // 'name' should appear only once in the properties column
        String propertiesCell = result.lines()
            .filter(l -> l.startsWith("pet,"))
            .findFirst().orElse("");
        int firstName = propertiesCell.indexOf("name");
        int secondName = propertiesCell.indexOf("name", firstName + 1);
        assertEquals("'name' should be deduplicated in properties\n" + propertiesCell, -1, secondName);
    }

    /**
     * CSV quoting: properties containing commas are wrapped in double quotes.
     * A class with many properties whose joined string naturally contains commas
     * must be quoted in the CSV output.
     */
    public void testPropertiesAreQuotedWhenContainCommas() {
        myFixture.addFileToProject("src/Entity/Invoice.php",
            "<?php\nnamespace App\\Entity;\n" +
            "class Invoice {\n" +
            "    public function getNumber(): string {}\n" +
            "    public function getTotal(): float {}\n" +
            "}\n"
        );
        myFixture.addFileToProject("templates/invoice/detail.html.twig",
            "{# @var invoice \\App\\Entity\\Invoice #}\n" +
            "{{ invoice.number }}"
        );

        String result = new TwigTemplateVariablesCollector(getProject()).collect("invoice/detail.html.twig");

        // With at least two properties the joined string contains a comma → must be quoted
        assertTrue("Properties with comma must be CSV-quoted\n" + result,
            result.contains("\"number,total\"") || result.contains("\"total,number\"")
        );
    }
}
