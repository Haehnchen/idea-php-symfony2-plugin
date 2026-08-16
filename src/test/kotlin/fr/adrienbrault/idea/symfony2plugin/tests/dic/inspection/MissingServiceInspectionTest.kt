package fr.adrienbrault.idea.symfony2plugin.tests.dic.inspection

import fr.adrienbrault.idea.symfony2plugin.dic.inspection.MissingServiceInspection
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.dic.inspection.MissingServiceInspection
 */
class MissingServiceInspectionTest : SymfonyLightCodeInsightFixtureTestCase() {
    override fun setUp() {
        super.setUp()
        myFixture.copyFileToProject("classes.php")
        myFixture.copyFileToProject("services.xml")
    }

    override fun getTestDataPath(): String {
        return "src/test/kotlin/fr/adrienbrault/idea/symfony2plugin/tests/dic/inspection/fixtures"
    }

    fun testThatPhpServiceInterfaceForGetMethodIsInspected() {
        assertLocalInspectionContains("test.php", "<?php\n" +
                "/** @var \$x \\Symfony\\Component\\DependencyInjection\\ContainerInterface */\n" +
                "\$x->get('fo<caret>obar')",
            MissingServiceInspection.INSPECTION_MESSAGE
        )

        assertLocalInspectionNotContains("test.php", "<?php\n" +
                "/** @var \$x \\Symfony\\Component\\DependencyInjection\\ContainerInterface */\n" +
                "\$x->get('app.ma<caret>iler')",
            MissingServiceInspection.INSPECTION_MESSAGE
        )
    }

    fun testThatContainerBagGetMethodIsNotInspectedAsService() {
        assertLocalInspectionNotContains("test.php", "<?php\n" +
                "use Symfony\\Component\\DependencyInjection\\ParameterBag\\ContainerBagInterface;\n" +
                "\n" +
                "class TestService\n" +
                "{\n" +
                "    public function __construct(private ContainerBagInterface \$containerBag) {}\n" +
                "\n" +
                "    public function testFoo()\n" +
                "    {\n" +
                "        \$this->containerBag->get('app.to<caret>ken');\n" +
                "    }\n" +
                "}",
            MissingServiceInspection.INSPECTION_MESSAGE
        )
    }

    fun testThatPhpAttributesForServiceAutowireIsInspected() {
        assertLocalInspectionContains("test.php", "<?php\n" +
                "use Symfony\\Component\\DependencyInjection\\Attribute\\Autowire;\n" +
                "\n" +
                "class HandlerCollection\n" +
                "{\n" +
                "    public function __construct(\n" +
                "        #[Autowire(service: 'fo<caret>obar')]" +
                "    ) {}\n" +
                "}",
            MissingServiceInspection.INSPECTION_MESSAGE
        )

        assertLocalInspectionContains("test.php", "<?php\n" +
                "use Symfony\\Component\\DependencyInjection\\Attribute\\Autowire;\n" +
                "\n" +
                "class HandlerCollection\n" +
                "{\n" +
                "    public function __construct(\n" +
                "        #[Autowire(service: \"fo<caret>obar\")]" +
                "    ) {}\n" +
                "}",
            MissingServiceInspection.INSPECTION_MESSAGE
        )
    }

    fun testThatPhpAttributesForServiceAsDecoratorIsInspected() {
        assertLocalInspectionContains("test.php", "<?php\n" +
                "use Symfony\\Component\\DependencyInjection\\Attribute\\AsDecorator;\n" +
                "#[AsDecorator(\"fo<caret>obar\")]\n" +
                "class HandlerCollection {}",
            MissingServiceInspection.INSPECTION_MESSAGE
        )
    }

    fun testThatYamlServiceInterfaceForGetMethodIsInspected() {
        assertLocalInspectionContains("services.yml", "services:\n   @args<caret>_unknown", MissingServiceInspection.INSPECTION_MESSAGE)
        assertLocalInspectionContains("services.yml", "services:\n   @Args<caret>_unknown", MissingServiceInspection.INSPECTION_MESSAGE)

        assertLocalInspectionNotContains("services.yml", "services:\n   @App.ma<caret>iler", MissingServiceInspection.INSPECTION_MESSAGE)
        assertLocalInspectionNotContains("services.yml", "services:\n   @app.ma<caret>iler", MissingServiceInspection.INSPECTION_MESSAGE)

        assertLocalInspectionNotContains("services.yml", "services:\n   @@args<caret>_unknown", MissingServiceInspection.INSPECTION_MESSAGE)
        assertLocalInspectionNotContains("services.yml", "services:\n   @=args<caret>_unknown", MissingServiceInspection.INSPECTION_MESSAGE)
    }
}
