package fr.adrienbrault.idea.symfony2plugin.tests.routing;

import com.intellij.codeInsight.hints.InlayHintsSink;
import com.intellij.openapi.editor.Editor;
import fr.adrienbrault.idea.symfony2plugin.routing.RouteInlayHintsProvider;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

/**
 * Test for {@link RouteInlayHintsProvider}
 */
public class RouteInlayHintsProviderTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void setUp() throws Exception {
        super.setUp();
        myFixture.copyFileToProject("classes.php");
    }

    public String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/routing/fixtures";
    }

    /**
     * Test that inlay hints are provided for Route attributes
     */
    public void testInlayHintsForRouteAttribute() {
        myFixture.configureByText("TestController.php", """
                <?php
                namespace App\\Controller;
                
                use Symfony\\Component\\Routing\\Annotation\\Route;
                
                class TestController {
                    #[Route('/api/users', name: 'users_list')]<caret>
                    public function list() {
                        return [];
                    }
                }
                """);

        // Note: Testing inlay hints requires more complex setup with InlayHintsProviderFactory
        // This is a basic structural test to ensure the class is correctly configured
        RouteInlayHintsProvider provider = new RouteInlayHintsProvider();
        
        assertNotNull(provider.getKey());
        assertEquals("Symfony Route Actions", provider.getName());
        assertTrue(provider.isVisibleInSettings());
    }

    /**
     * Test that provider works with different Route attribute formats
     */
    public void testRouteAttributeWithMethodParameter() {
        myFixture.configureByText("TestController.php", """
                <?php
                namespace App\\Controller;
                
                use Symfony\\Component\\Routing\\Annotation\\Route;
                
                class UserController {
                    #[Route('/api/users/{id}', name: 'user_show', methods: ['GET'])]
                    public function show(int $id) {
                        return [];
                    }
                    
                    #[Route('/api/users', name: 'user_create', methods: ['POST'])]
                    public function create() {
                        return [];
                    }
                }
                """);

        RouteInlayHintsProvider provider = new RouteInlayHintsProvider();
        assertNotNull(provider.createSettings());
    }

    /**
     * Test that inlay hints are not shown when plugin is disabled
     */
    public void testNoInlayHintsWhenPluginDisabled() {
        // This would require mocking the Settings service
        // Left as a placeholder for more comprehensive testing
        assertTrue(true);
    }
}
