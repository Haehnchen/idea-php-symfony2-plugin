package fr.adrienbrault.idea.symfony2plugin.tests.templating.util;

import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigExtensionParser;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

import java.io.File;

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
        return new File(this.getClass().getResource("fixtures").getFile()).getAbsolutePath();
    }

    public void testExtensionAreCollected() {
        TwigExtensionParser extensionParser = new TwigExtensionParser(getProject());

        assertEquals(
            "#M#C\\Twig\\Extensions.foobar",
            extensionParser.getFilters().get("trans").getSignature()
        );

        assertEquals(
            "#Fmax",
            extensionParser.getFunctions().get("max").getSignature()
        );

        assertEquals(
            "SIMPLE_TEST",
            extensionParser.getSimpleTest().get("my_test").getType()
        );

        assertEquals(
            "#M#C\\My_Node_Test.compile",
            extensionParser.getSimpleTest().get("my_test").getSignature()
        );

        assertEquals(
            "#Ffoo_test",
            extensionParser.getSimpleTest().get("my_test_2").getSignature()
        );

        assertEquals(
            "OPERATOR",
            extensionParser.getOperators().get("not").getType()
        );

        assertEquals(
            "OPERATOR",
            extensionParser.getOperators().get("or").getType()
        );
    }

    public void testExtensionAreCollectedForDeprecated() {
        TwigExtensionParser extensionParser = new TwigExtensionParser(getProject());

        assertEquals(
            "#M#C\\Symfony\\Bridge\\Twig\\Node\\FormEnctypeNode.compile",
            extensionParser.getFunctions().get("form_enctype").getSignature()
        );

        assertEquals(
            "#M#C\\Twig\\Extensions.foobar",
            extensionParser.getFunctions().get("hwi_oauth_login_url").getSignature()
        );

        assertEquals(
            "#M#C\\Twig\\Extensions.foobar",
            extensionParser.getFilters().get("doctrine_minify_query").getSignature()
        );

        assertEquals(
            "#Ffoobar",
            extensionParser.getFilters().get("localizeddate").getSignature()
        );
    }

    public void testExtensionAreCollectedForVersion2() {
        TwigExtensionParser extensionParser = new TwigExtensionParser(getProject());

        assertEquals(
            "#M#C\\Twig\\Extensions.foobar",
            extensionParser.getFilters().get("trans_2").getSignature()
        );

        assertEquals(
            "#Fmax",
            extensionParser.getFunctions().get("max_2").getSignature()
        );

        assertEquals(
            "#Ffoo_test",
            extensionParser.getSimpleTest().get("iterable_2").getSignature()
        );
    }

    public void testExtensionAreCollectedForVersion3() {
        TwigExtensionParser extensionParser = new TwigExtensionParser(getProject());

        assertEquals(
            "#M#C\\Twig\\Extensions.foobar",
            extensionParser.getFilters().get("trans_3").getSignature()
        );

        assertEquals(
            "#Fmax",
            extensionParser.getFunctions().get("max_3").getSignature()
        );

        assertEquals(
            "#Ffoo_test",
            extensionParser.getSimpleTest().get("iterable_3").getSignature()
        );
    }
}
