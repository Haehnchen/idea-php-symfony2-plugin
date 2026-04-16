package fr.adrienbrault.idea.symfony2plugin.tests.routing;

import com.intellij.patterns.PlatformPatterns;
import com.jetbrains.php.lang.PhpFileType;
import com.jetbrains.php.lang.psi.elements.Method;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

import java.util.ArrayList;
import java.util.Collection;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 *
 * @see fr.adrienbrault.idea.symfony2plugin.routing.PhpRouteReferenceContributor
 */
public class PhpRouteReferenceContributorTest extends SymfonyLightCodeInsightFixtureTestCase {


    public void setUp() throws Exception {
        super.setUp();

        myFixture.configureFromExistingVirtualFile(myFixture.copyFileToProject("PhpRouteReferenceContributor.php"));
    }

    protected String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/routing/fixtures";
    }

    public void testGenerateUrlProvidesNavigation() {

        Collection<String[]> providers = new ArrayList<>() {{
            add(new String[]{"Symfony\\Component\\Routing\\Generator\\UrlGeneratorInterface", "generate"});
            add(new String[]{"Symfony\\Bundle\\FrameworkBundle\\Controller\\Controller", "generateUrl"});
            add(new String[]{"Symfony\\Bundle\\FrameworkBundle\\Controller\\Controller", "redirectToRoute"});
            add(new String[]{"My\\Proxy\\Routing\\Controller", "generateUrl"});
        }};

        for (String[] provider : providers) {
            assertCompletionContains(PhpFileType.INSTANCE,
                String.format("<?php\n" +
                    "/** @var $f \\%s */\n" +
                    "$f->%s('<caret>')",
                    provider[0], provider[1]
                ),
                "foo_bar"
            );
        }

    }

    public void testRoutingConfiguratorControllerProvidesNavigation() {
        myFixture.addFileToProject("routing/classes.php", "<?php\n" +
            "namespace Symfony\\Component\\Routing\\Loader\\Configurator\n" +
            "{\n" +
            "    use Symfony\\Component\\Routing\\Loader\\Configurator\\Traits\\AddTrait;\n" +
            "    use Symfony\\Component\\Routing\\Loader\\Configurator\\Traits\\RouteTrait;\n" +
            "\n" +
            "    class RouteConfigurator\n" +
            "    {\n" +
            "        use AddTrait;\n" +
            "        use RouteTrait;\n" +
            "    }\n" +
            "\n" +
            "    class RoutingConfigurator\n" +
            "    {\n" +
            "        use AddTrait;\n" +
            "    }\n" +
            "}\n" +
            "\n" +
            "namespace Symfony\\Component\\Routing\\Loader\\Configurator\\Traits\n" +
            "{\n" +
            "    use Symfony\\Component\\Routing\\Loader\\Configurator\\RouteConfigurator;\n" +
            "\n" +
            "    trait RouteTrait\n" +
            "    {\n" +
            "        final public function controller(callable|string|array $controller): static {}\n" +
            "    }\n" +
            "\n" +
            "    trait AddTrait\n" +
            "    {\n" +
            "        public function add(string $name, string|array $path): RouteConfigurator {}\n" +
            "    }\n" +
            "}\n");

        myFixture.addFileToProject("src/MyController.php", "<?php\n" +
            "namespace App\\Controller;\n" +
            "class MyController\n" +
            "{\n" +
            "    public function detailAction() {}\n" +
            "}\n");

        assertNavigationMatch(PhpFileType.INSTANCE, "<?php\n" +
            "use Symfony\\Component\\Routing\\Loader\\Configurator\\RoutingConfigurator;\n" +
            "\n" +
            "return static function (RoutingConfigurator $routes): void {\n" +
            "    $routes->add('app_array_route', '/array')\n" +
            "        ->controller('App\\\\Controller\\\\MyCont<caret>roller::detailAction');\n" +
            "};", PlatformPatterns.psiElement(Method.class).withName("detailAction"));
    }
}
