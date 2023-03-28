package fr.adrienbrault.idea.symfony2plugin.tests.routing.inspection;

import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 *
 * @see fr.adrienbrault.idea.symfony2plugin.routing.inspection.DuplicateLocalRouteInspection
 */
public class DuplicateLocalRouteInspectionTest extends SymfonyLightCodeInsightFixtureTestCase {
    public void setUp() throws Exception {
        super.setUp();

        myFixture.copyFileToProject("DuplicateLocalRouteInspection.php");
    }
    
    protected String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/routing/inspection/fixtures";
    }

    public void testDuplicateRouteKeyProvidesWarning() {
        assertLocalInspectionContains("routing.yml", "" +
                "foo:\n" +
                "  car: foo\n" +
                "f<caret>oo:\n" +
                "  car: foo\n",
            "Symfony: Duplicate route name"
        );

        assertLocalInspectionContains("routing.yml", "" +
                "fo<caret>o:\n" +
                "  car: foo\n" +
                "foo:\n" +
                "  car: foo\n",
            "Symfony: Duplicate route name"
        );

        assertLocalInspectionNotContains("routing.yml", "" +
                "foo:\n" +
                "  car: foo\n" +
                "foo<caret>bar:\n" +
                "  car: foo\n" +
                "foo:\n" +
                "  car: foo\n",
            "Symfony: Duplicate route name"
        );
    }

    public void testDuplicateRoutingKeysForPhpAttribute() {
        assertLocalInspectionContains("controller.php", "<?php\n" +
                "\n" +
                "use Symfony\\Component\\Routing\\Annotation\\Route;\n" +
                "\n" +
                "class FooController\n" +
                "{\n" +
                "    #[Route(name: 'foobar<caret>_index')]\n" +
                "    #[Route(name: 'foobar_index')]\n" +
                "    #[Route(name: 'foobar_index_1')]\n" +
                "    public function index() {}\n" +
                "}\n",
            "Symfony: Duplicate route name"
        );

        assertLocalInspectionContains("controller.php", "<?php\n" +
                "\n" +
                "use Symfony\\Component\\Routing\\Annotation\\Route;\n" +
                "\n" +
                "class FooController\n" +
                "{\n" +
                "    #[Route(name: 'foobar<caret>_index')]\n" +
                "    public function index() {}\n" +
                "\n" +
                "    #[Route(name: 'foobar_index')]\n" +
                "    public function foo() {}\n" +
                "}\n",
            "Symfony: Duplicate route name"
        );

        assertLocalInspectionNotContains("controller.php", "<?php\n" +
                "\n" +
                "use Symfony\\Component\\Routing\\Annotation\\Route;\n" +
                "\n" +
                "class FooController\n" +
                "{\n" +
                "    #[Route(name: 'foobar<caret>_index')]\n" +
                "    public function index() {}\n" +
                "\n" +
                "    #[Route(name: 'foobar_index_1')]\n" +
                "    public function foo() {}\n" +
                "}\n",
            "Symfony: Duplicate route name"
        );
    }

    public void testDuplicateRoutingKeysForPhpDocBlocks() {
        assertLocalInspectionContains("controller.php", "<?php\n" +
                "\n" +
                "use Symfony\\Component\\Routing\\Annotation\\Route;\n" +
                "\n" +
                "class FooController\n" +
                "{\n" +
                "    /**\n" +
                "     * @Route(name=\"test\") \n" +
                "     */\n" +
                "    public function index() {}\n" +
                "\n" +
                "    /**\n" +
                "     * @Route(name=\"te<caret>st\")\n" +
                "     */\n" +
                "    public function index2()\n" +
                "    {\n" +
                "    }\n" +
                "}\n",
            "Symfony: Duplicate route name"
        );
    }

    public void testDuplicateRoutingKeysForXml() {
        assertLocalInspectionContains("routing.xml", "" +
                "<routes>\n" +
                "    <route id=\"blog_list\" path=\"/blog\"/>\n" +
                "    <route id=\"blog<caret>_list\" path=\"/blog1\"/>\n" +
                "</routes>",
            "Symfony: Duplicate route name"
        );
    }
}
