package fr.adrienbrault.idea.symfony2plugin.tests.routing;

import com.intellij.patterns.XmlPatterns;
import com.intellij.psi.PsiFile;
import com.jetbrains.php.lang.PhpFileType;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 *
 * @see fr.adrienbrault.idea.symfony2plugin.routing.PhpLineMarkerProvider
 */
public class PhpLineMarkerProviderTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void setUp() throws Exception {
        super.setUp();

        myFixture.copyFileToProject("BundleScopeLineMarkerProvider.php");
        myFixture.copyFileToProject("XmlLineMarkerProvider.php");
    }

    protected String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/routing/fixtures";
    }

    public void testThatRouteLineMarkerForControllerIsGiven() {
        assertLineMarker(
                myFixture.configureByText(
                PhpFileType.INSTANCE,
                "<?php\n" +
                        "use Symfony\\Component\\Routing\\Loader\\Configurator\\RoutingConfigurator;\n" +
                        "\n" +
                        "return function(RoutingConfigurator $routes) {\n" +
                        "    $routes->add('xml_route', '/xml')\n" +
                        "        ->controller('Foo\\\\Bar')\n" +
                        "    ;\n" +
                        "};"
                ),
                new LineMarker.ToolTipEqualsAssert("Navigate to action")
        );
    }
}
