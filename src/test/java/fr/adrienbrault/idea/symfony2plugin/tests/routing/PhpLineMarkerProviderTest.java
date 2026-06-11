package fr.adrienbrault.idea.symfony2plugin.tests.routing;

import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiFile;
import com.jetbrains.php.lang.PhpFileType;
import fr.adrienbrault.idea.symfony2plugin.extension.TwigComponentDefinition;
import fr.adrienbrault.idea.symfony2plugin.extension.TwigComponentProvider;
import fr.adrienbrault.idea.symfony2plugin.extension.TwigComponentProviderParameter;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import fr.adrienbrault.idea.symfony2plugin.util.UxUtil;

import java.util.Collection;
import java.util.Collections;

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
        myFixture.copyFileToProject("classes.php");
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

        assertLineMarker(
            myFixture.configureByText(
                PhpFileType.INSTANCE,
                "<?php\n" +
                    "use Symfony\\Component\\Routing\\Loader\\Configurator\\RoutingConfigurator;\n" +
                    "\n" +
                    "return function(RoutingConfigurator $routes) {\n" +
                    "    $routes->add('xml_route', '/xml')\n" +
                    "        ->controller(\\Foo\\Bar::class)\n" +
                    "    ;\n" +
                    "};"
            ),
            new LineMarker.ToolTipEqualsAssert("Navigate to action")
        );

        assertLineMarker(
            myFixture.configureByText(
                PhpFileType.INSTANCE,
                "<?php\n" +
                    "use Symfony\\Component\\Routing\\Loader\\Configurator\\RoutingConfigurator;\n" +
                    "\n" +
                    "return function(RoutingConfigurator $routes) {\n" +
                    "    $routes->add('xml_route', '/xml')\n" +
                    "        ->controller([\\Foo\\Bar2::class, 'index'])\n" +
                    "    ;\n" +
                    "};"
            ),
            new LineMarker.ToolTipEqualsAssert("Navigate to action")
        );
    }

    public void testThatUxComponentLineMarkerUsesProvidedTemplateFile() {
        PsiFile templateFile = myFixture.addFileToProject(
            "external/package/templates/components/Button/Primary.html.twig",
            "<button></button>"
        );

        UxUtil.TWIG_COMPONENT_PROVIDERS.getPoint().registerExtension(
            new TestTwigComponentProvider(new TwigComponentDefinition(
                "ExternalPackage:Button:Primary",
                templateFile.getVirtualFile(),
                "\\ExternalPackage\\Components\\Button\\Primary"
            )),
            getTestRootDisposable()
        );

        PsiFile phpFile = myFixture.addFileToProject(
            "src/ExternalPackage/Components/Button/Primary.php",
            "<?php\n" +
                "namespace ExternalPackage\\Components\\Button;\n" +
                "class Primary {}\n"
        );

        assertLineMarker(phpFile, new LineMarker.ToolTipEqualsAssert("Navigate to UX Component template"));
        assertLineMarker(phpFile, new LineMarker.TargetAcceptsPattern(
            "Navigate to UX Component template",
            PlatformPatterns.psiFile().withName("Primary.html.twig")
        ));
    }

    private static class TestTwigComponentProvider implements TwigComponentProvider {
        private final TwigComponentDefinition definition;

        private TestTwigComponentProvider(TwigComponentDefinition definition) {
            this.definition = definition;
        }

        @Override
        public Collection<TwigComponentDefinition> getComponents(TwigComponentProviderParameter parameter) {
            return Collections.singletonList(this.definition);
        }
    }
}
