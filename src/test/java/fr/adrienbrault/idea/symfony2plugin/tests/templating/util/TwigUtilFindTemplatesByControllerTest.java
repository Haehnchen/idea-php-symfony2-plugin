package fr.adrienbrault.idea.symfony2plugin.tests.templating.util;

import com.jetbrains.php.lang.PhpFileType;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.PhpTwigTemplateUsageStubIndex;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

import java.util.*;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil#findTemplatesByControllers
 * @see fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil#findTemplatesByController
 */
public class TwigUtilFindTemplatesByControllerTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void testFindTemplatesByControllersBatch() {
        myFixture.configureByText(PhpFileType.INSTANCE, "<?php\n" +
            "namespace App\\Controller;\n" +
            "\n" +
            "class HomeController\n" +
            "{\n" +
            "   public function index()\n" +
            "   {\n" +
            "       $this->render('home/index.html.twig');\n" +
            "       $this->renderView('home/_partial.html.twig');\n" +
            "   }\n" +
            "\n" +
            "   public function show()\n" +
            "   {\n" +
            "       $this->render('home/show.html.twig');\n" +
            "   }\n" +
            "}\n" +
            "\n" +
            "class UserController\n" +
            "{\n" +
            "   public function list()\n" +
            "   {\n" +
            "       $this->render('user/list.html.twig');\n" +
            "   }\n" +
            "}"
        );

        // Index must contain all templates
        assertIndexContains(PhpTwigTemplateUsageStubIndex.KEY,
            "home/index.html.twig",
            "home/_partial.html.twig",
            "home/show.html.twig",
            "user/list.html.twig"
        );

        // Batch lookup for multiple controllers - single index iteration
        Set<String> controllers = new HashSet<>(Arrays.asList(
            "App\\Controller\\HomeController::index",
            "App\\Controller\\HomeController::show",
            "App\\Controller\\UserController::list"
        ));

        Map<String, Set<String>> result = TwigUtil.findTemplatesByControllers(getProject(), controllers);

        // Verify HomeController::index
        assertEquals(2, result.get("App\\Controller\\HomeController.index").size());
        assertContainsElements(result.get("App\\Controller\\HomeController.index"),
            "home/index.html.twig",
            "home/_partial.html.twig"
        );

        // Verify HomeController::show
        assertEquals(1, result.get("App\\Controller\\HomeController.show").size());
        assertContainsElements(result.get("App\\Controller\\HomeController.show"),
            "home/show.html.twig"
        );

        // Verify UserController::list
        assertEquals(1, result.get("App\\Controller\\UserController.list").size());
        assertContainsElements(result.get("App\\Controller\\UserController.list"),
            "user/list.html.twig"
        );
    }

    public void testFindTemplatesReturnsEmptyForUnknownController() {
        myFixture.configureByText(PhpFileType.INSTANCE, "<?php\n" +
            "namespace App\\Controller;\n" +
            "\n" +
            "class ExistingController\n" +
            "{\n" +
            "   public function action()\n" +
            "   {\n" +
            "       $this->render('existing/template.html.twig');\n" +
            "   }\n" +
            "}"
        );

        Set<String> templates = TwigUtil.findTemplatesByController(
            getProject(),
            "App\\Controller\\NonExistentController::action"
        );

        assertTrue(templates.isEmpty());
    }

    public void testFindTemplatesWithLeadingBackslash() {
        myFixture.configureByText(PhpFileType.INSTANCE, "<?php\n" +
            "namespace App\\Controller;\n" +
            "\n" +
            "class BackslashController\n" +
            "{\n" +
            "   public function test()\n" +
            "   {\n" +
            "       $this->render('backslash/test.html.twig');\n" +
            "   }\n" +
            "}"
        );

        // Should handle leading backslash gracefully
        Set<String> templates = TwigUtil.findTemplatesByController(
            getProject(),
            "\\App\\Controller\\BackslashController::test"
        );

        assertContainsElements(templates, "backslash/test.html.twig");
    }

}
