package fr.adrienbrault.idea.symfony2plugin.tests.codeInspection.service;

import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 *
 * @see fr.adrienbrault.idea.symfony2plugin.codeInspection.service.ServiceDeprecatedClassesInspection
 */
public class ServiceDeprecatedClassesInspectionTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void setUp() throws Exception {
        super.setUp();

        myFixture.configureFromExistingVirtualFile(myFixture.copyFileToProject("classes.php"));
        myFixture.configureFromExistingVirtualFile(myFixture.copyFileToProject("services.xml"));
    }

    public String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/codeInspection/service/fixtures";
    }

    public void testPhpClassDocBlockDeprecated() {
        assertLocalInspectionContains("foo.php", "<?php" +
                "/** @var $c \\Symfony\\Component\\DependencyInjection\\ContainerInterface */\n" +
                "$c->get('f<caret>oo');",
            "Class 'FooBar' is deprecated"
        );
    }

    public void testPhpServiceDeprecated() {
        assertLocalInspectionContains("foo.php", "<?php" +
                "/** @var $c \\Symfony\\Component\\DependencyInjection\\ContainerInterface */\n" +
                "$c->get('foo_depr<caret>ecated');",
            "Service 'foo_deprecated' is deprecated"
        );
    }

    public void testPhpServiceInsideAutowireAttributeDeprecated() {
        assertLocalInspectionContains("foo.php", "<?php" +
                "\n" +
                "class HandlerCollection\n" +
                "{\n" +
                "    public function __construct(\n" +
                "        #[\\Symfony\\Component\\DependencyInjection\\Attribute\\Autowire(service: 'foo_depr<caret>ecated')] $handlers\n" +
                "    ) {}\n" +
                "}",
            "Service 'foo_deprecated' is deprecated"
        );

        assertLocalInspectionContains("foo.php", "<?php" +
                "\n" +
                "class HandlerCollection\n" +
                "{\n" +
                "    public function __construct(\n" +
                "        #[\\Symfony\\Component\\DependencyInjection\\Attribute\\Autowire(service: \"foo_depr<caret>ecated\")] $handlers\n" +
                "    ) {}\n" +
                "}",
            "Service 'foo_deprecated' is deprecated"
        );
    }

    public void testYmlClassDocBlockDeprecated() {
        assertLocalInspectionContains("foo.yml", "@f<caret>oo", "Class 'FooBar' is deprecated");
        assertLocalInspectionContains("foo.yml", "class: Foo\\Bar<caret>\\FooBar", "Class 'FooBar' is deprecated");
    }

    public void testYmlServiceDeprecated() {
        assertLocalInspectionContains("foo.yml", "@foo_depr<caret>ecated", "Service 'foo_deprecated' is deprecated");
    }

    public void testXmlClassDocBlockDeprecated() {
        assertLocalInspectionContains("foo.xml", "<services><service><argument type=\"service\" id=\"fo<caret>o\" /></service></services>", "Class 'FooBar' is deprecated");
        assertLocalInspectionContains("foo.xml", "<services><service id=\"foo\" class=\"Foo\\Bar<caret>\\FooBar\"></service></services>", "Class 'FooBar' is deprecated");
    }

    public void testXmlServiceDeprecated() {
        assertLocalInspectionContains("foo.xml", "<services><service><argument type=\"service\" id=\"foo_depr<caret>ecated\" /></service></services>", "Service 'foo_deprecated' is deprecated");
    }

}
