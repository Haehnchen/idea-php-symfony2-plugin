package fr.adrienbrault.idea.symfony2plugin.tests.codeInspection.service

import fr.adrienbrault.idea.symfony2plugin.codeInspection.service.ServiceDeprecatedClassesInspection
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 *
 * @see fr.adrienbrault.idea.symfony2plugin.codeInspection.service.ServiceDeprecatedClassesInspection
 */
class ServiceDeprecatedClassesInspectionTest : SymfonyLightCodeInsightFixtureTestCase() {
    override fun setUp() {
        super.setUp()

        myFixture.configureFromExistingVirtualFile(myFixture.copyFileToProject("classes.php"))
        myFixture.configureFromExistingVirtualFile(myFixture.copyFileToProject("services.xml"))
    }

    override fun getTestDataPath(): String {
        return "src/test/kotlin/fr/adrienbrault/idea/symfony2plugin/tests/codeInspection/service/fixtures"
    }

    fun testPhpClassDocBlockDeprecated() {
        assertLocalInspectionContains(ServiceDeprecatedClassesInspection.ServiceDeprecatedClassesInspectionPhp::class.java, "foo.php", "<?php" +
                "/** @var \$c \\Symfony\\Component\\DependencyInjection\\ContainerInterface */\n" +
                "\$c->get('f<caret>oo');",
            "Class 'FooBar' is deprecated"
        )
    }

    fun testPhpServiceDeprecated() {
        assertLocalInspectionContains(ServiceDeprecatedClassesInspection.ServiceDeprecatedClassesInspectionPhp::class.java, "foo.php", "<?php" +
                "/** @var \$c \\Symfony\\Component\\DependencyInjection\\ContainerInterface */\n" +
                "\$c->get('foo_depr<caret>ecated');",
            "Service 'foo_deprecated' is deprecated"
        )
    }

    fun testPhpServiceInsideAutowireAttributeDeprecated() {
        assertLocalInspectionContains(ServiceDeprecatedClassesInspection.ServiceDeprecatedClassesInspectionPhp::class.java, "foo.php", "<?php" +
                "\n" +
                "class HandlerCollection\n" +
                "{\n" +
                "    public function __construct(\n" +
                "        #[\\Symfony\\Component\\DependencyInjection\\Attribute\\Autowire(service: 'foo_depr<caret>ecated')] \$handlers\n" +
                "    ) {}\n" +
                "}",
            "Service 'foo_deprecated' is deprecated"
        )

        assertLocalInspectionContains(ServiceDeprecatedClassesInspection.ServiceDeprecatedClassesInspectionPhp::class.java, "foo.php", "<?php" +
                "\n" +
                "class HandlerCollection\n" +
                "{\n" +
                "    public function __construct(\n" +
                "        #[\\Symfony\\Component\\DependencyInjection\\Attribute\\Autowire(service: \"foo_depr<caret>ecated\")] \$handlers\n" +
                "    ) {}\n" +
                "}",
            "Service 'foo_deprecated' is deprecated"
        )
    }

    fun testYmlClassDocBlockDeprecated() {
        assertLocalInspectionContains(ServiceDeprecatedClassesInspection.ServiceDeprecatedClassesInspectionYaml::class.java, "foo.yml", "@f<caret>oo", "Class 'FooBar' is deprecated")
        assertLocalInspectionContains(ServiceDeprecatedClassesInspection.ServiceDeprecatedClassesInspectionYaml::class.java, "foo.yml", "class: Foo\\Bar<caret>\\FooBar", "Class 'FooBar' is deprecated")
    }

    fun testYmlServiceDeprecated() {
        assertLocalInspectionContains(ServiceDeprecatedClassesInspection.ServiceDeprecatedClassesInspectionYaml::class.java, "foo.yml", "@foo_depr<caret>ecated", "Service 'foo_deprecated' is deprecated")
    }

    fun testXmlClassDocBlockDeprecated() {
        assertLocalInspectionContains(ServiceDeprecatedClassesInspection.ServiceDeprecatedClassesInspectionXml::class.java, "foo.xml", "<services><service><argument type=\"service\" id=\"fo<caret>o\" /></service></services>", "Class 'FooBar' is deprecated")
        assertLocalInspectionContains(ServiceDeprecatedClassesInspection.ServiceDeprecatedClassesInspectionXml::class.java, "foo.xml", "<services><service id=\"foo\" class=\"Foo\\Bar<caret>\\FooBar\"></service></services>", "Class 'FooBar' is deprecated")
    }

    fun testXmlServiceDeprecated() {
        assertLocalInspectionContains(ServiceDeprecatedClassesInspection.ServiceDeprecatedClassesInspectionXml::class.java, "foo.xml", "<services><service><argument type=\"service\" id=\"foo_depr<caret>ecated\" /></service></services>", "Service 'foo_deprecated' is deprecated")
    }
}
