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
        assertEquals(
            "#M#C\\Twig\\Extensions.foobar",
            TwigExtensionParser.getFilters(getProject()).get("trans").getSignature()
        );

        assertEquals(
            "#Fmax",
            TwigExtensionParser.getFunctions(getProject()).get("max").getSignature()
        );

        assertEquals(
            "SIMPLE_TEST",
            TwigExtensionParser.getSimpleTest(getProject()).get("my_test").getType()
        );

        assertEquals(
            "#M#C\\My_Node_Test.compile",
            TwigExtensionParser.getSimpleTest(getProject()).get("my_test").getSignature()
        );

        assertEquals(
            "#Ffoo_test",
            TwigExtensionParser.getSimpleTest(getProject()).get("my_test_2").getSignature()
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
            TwigExtensionParser.getFunctions(getProject()).get("class_instance_foobar").getSignature()
        );

        assertEquals(
            "#M#C\\Twig\\Extensions.getFoobar",
            TwigExtensionParser.getFunctions(getProject()).get("class_php_callable_method_foobar").getSignature()
        );

        assertEquals(
            "#Fmax",
            TwigExtensionParser.getFunctions(getProject()).get("class_php_callable_function_foobar").getSignature()
        );

        assertEquals(
            "#Fmax",
            TwigExtensionParser.getFunctions(getProject()).get("conditional_return").getSignature()
        );
    }

    public void testExtensionAreCollectedForDeprecated() {
        assertEquals(
            "#M#C\\Symfony\\Bridge\\Twig\\Node\\FormEnctypeNode.compile",
            TwigExtensionParser.getFunctions(getProject()).get("form_enctype").getSignature()
        );

        assertEquals(
            "#M#C\\Twig\\Extensions.foobar",
            TwigExtensionParser.getFunctions(getProject()).get("hwi_oauth_login_url").getSignature()
        );

        assertEquals(
            "#M#C\\Twig\\Extensions.foobar",
            TwigExtensionParser.getFilters(getProject()).get("doctrine_minify_query").getSignature()
        );

        assertEquals(
            "#Ffoobar",
            TwigExtensionParser.getFilters(getProject()).get("localizeddate").getSignature()
        );
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
