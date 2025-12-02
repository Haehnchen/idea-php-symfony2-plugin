package fr.adrienbrault.idea.symfony2plugin.tests.templating.util;

import fr.adrienbrault.idea.symfony2plugin.templating.dict.TwigExtension;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigExtensionParser;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

import java.util.Map;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.templating.util.TwigExtensionParser
 */
public class TwigExtensionParserTest extends SymfonyLightCodeInsightFixtureTestCase {
    public void setUp() throws Exception {
        super.setUp();

        myFixture.copyFileToProject("twig_extensions.php");
    }

    public String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/templating/util/fixtures";
    }

    public void testExtensionAreCollected() {
        Map<String, TwigExtension> functions = TwigExtensionParser.getFunctions(getProject());
        Map<String, TwigExtension> simpleTest = TwigExtensionParser.getSimpleTest(getProject());
        Map<String, TwigExtension> filters = TwigExtensionParser.getFilters(getProject());

        assertEquals(
            "#M#C\\Twig\\Extensions.foobar",
            filters.get("trans").getSignature()
        );

        assertEquals(
            "#Fmax",
            functions.get("max").getSignature()
        );

        assertEquals(
            "SIMPLE_TEST",
            simpleTest.get("my_test").getType()
        );

        assertEquals(
            "#M#C\\My_Node_Test.compile",
            simpleTest.get("my_test").getSignature()
        );

        assertEquals(
            "#Ffoo_test",
            simpleTest.get("my_test_2").getSignature()
        );

        assertEquals(
            "OPERATOR",
            TwigExtensionParser.getOperators(getProject()).get("not").getType()
        );

        assertEquals(
            "OPERATOR",
            TwigExtensionParser.getOperators(getProject()).get("or").getType()
        );

        assertEquals(
            "#M#C\\ClassInstance.getFoobar",
            functions.get("class_instance_foobar").getSignature()
        );

        assertEquals(
            "#M#C\\Twig\\Extensions.getFoobar",
            functions.get("class_php_callable_method_foobar").getSignature()
        );

        assertEquals(
            "#Fmax",
            functions.get("class_php_callable_function_foobar").getSignature()
        );

        assertEquals(
            "#Fmax",
            functions.get("conditional_return").getSignature()
        );

        assertEquals(
            "#M#C\\App\\Twig\\AppExtension.formatProductNumberFilter",
            filters.get("product_number_filter").getSignature()
        );

        assertEquals(
            "#M#C\\App\\Twig\\AppExtension.formatProductNumberFunction",
            functions.get("product_number_function").getSignature()
        );

        assertEquals(
            "#M#C\\App\\Twig\\AppExtension.formatProductNumberTest",
            simpleTest.get("product_number_test").getSignature()
        );

        assertEquals(
            "#M#C\\Twig\\Extensions.parseAttributeFunction",
            functions.get("attribute_parser_callable").getSignature()
        );
    }

    public void testExtensionAreCollectedForDeprecated() {
        Map<String, TwigExtension> functions = TwigExtensionParser.getFunctions(getProject());
        Map<String, TwigExtension> filters = TwigExtensionParser.getFilters(getProject());

        assertEquals(
            "#M#C\\Symfony\\Bridge\\Twig\\Node\\FormEnctypeNode.compile",
            functions.get("form_enctype").getSignature()
        );

        assertEquals(
            "#M#C\\Twig\\Extensions.foobar",
            functions.get("hwi_oauth_login_url").getSignature()
        );

        assertEquals(
            "#M#C\\Twig\\Extensions.foobar",
            filters.get("doctrine_minify_query").getSignature()
        );

        assertEquals(
            "#Ffoobar",
            filters.get("localizeddate").getSignature()
        );
    }

    public void testExtensionDeprecatedOptions() {
        Map<String, TwigExtension> filters = TwigExtensionParser.getFilters(getProject());

        assertTrue(filters.get("spaceless_deprecation_info").isDeprecated());
        assertTrue(filters.get("spaceless_deprecation_deprecated").isDeprecated());
    }

    public void testExtensionAreCollectedForVersion2() {
        assertEquals(
            "#M#C\\Twig\\Extensions.foobar",
            TwigExtensionParser.getFilters(getProject()).get("trans_2").getSignature()
        );

        assertEquals(
            "#Fmax",
            TwigExtensionParser.getFunctions(getProject()).get("max_2").getSignature()
        );

        assertEquals(
            "#Ffoo_test",
            TwigExtensionParser.getSimpleTest(getProject()).get("iterable_2").getSignature()
        );
    }

    public void testExtensionAreCollectedForVersion3() {
        assertEquals(
            "#M#C\\Twig\\Extensions.foobar",
            TwigExtensionParser.getFilters(getProject()).get("trans_3").getSignature()
        );

        assertEquals(
            "#Fmax",
            TwigExtensionParser.getFunctions(getProject()).get("max_3").getSignature()
        );

        assertEquals(
            "#Ffoo_test",
            TwigExtensionParser.getSimpleTest(getProject()).get("iterable_3").getSignature()
        );
    }

    public void testExtensionAreCollectedForStringExtension() {
        myFixture.copyFileToProject("StringExtension.php");

        Map<String, TwigExtension> filters = TwigExtensionParser.getFilters(getProject());
        TwigExtension twigExtension = filters.get("u");

        assertEquals(
            "#M#C\\Twig\\Extra\\String\\StringExtension.createUnicodeString",
            twigExtension.getSignature()
        );

        assertContainsElements(
            twigExtension.getTypes(),
            "\\Symfony\\Component\\String\\UnicodeString"
        );
    }
}
