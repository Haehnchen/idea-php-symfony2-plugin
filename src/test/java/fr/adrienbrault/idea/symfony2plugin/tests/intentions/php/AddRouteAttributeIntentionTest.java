package fr.adrienbrault.idea.symfony2plugin.tests.intentions.php;

import com.jetbrains.php.lang.PhpFileType;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.intentions.php.AddRouteAttributeIntention
 */
public class AddRouteAttributeIntentionTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void setUp() throws Exception {
        super.setUp();
        myFixture.configureFromExistingVirtualFile(myFixture.copyFileToProject("classes.php"));
    }

    public String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/intentions/php/fixtures";
    }

    public void testIntentionIsAvailableForControllerExtendingAbstractController() {
        assertIntentionIsAvailable(
            PhpFileType.INSTANCE,
            "<?php\n" +
                "namespace App\\Controller;\n" +
                "use Symfony\\Bundle\\FrameworkBundle\\Controller\\AbstractController;\n" +
                "\n" +
                "class TestController extends AbstractController\n" +
                "{\n" +
                "    public function <caret>indexAction()\n" +
                "    {\n" +
                "    }\n" +
                "}\n",
            "Symfony: Add Route attribute"
        );
    }

    public void testIntentionIsAvailableForControllerWithAsControllerAttribute() {
        assertIntentionIsAvailable(
            PhpFileType.INSTANCE,
            "<?php\n" +
                "namespace App\\Controller;\n" +
                "use Symfony\\Component\\HttpKernel\\Attribute\\AsController;\n" +
                "\n" +
                "#[AsController]\n" +
                "class TestController\n" +
                "{\n" +
                "    public function <caret>indexAction()\n" +
                "    {\n" +
                "    }\n" +
                "}\n",
            "Symfony: Add Route attribute"
        );
    }

    public void testIntentionIsAvailableForControllerWithRouteOnAnotherMethod() {
        assertIntentionIsAvailable(
            PhpFileType.INSTANCE,
            "<?php\n" +
                "namespace App\\Controller;\n" +
                "use Symfony\\Component\\Routing\\Attribute\\Route;\n" +
                "\n" +
                "class TestController\n" +
                "{\n" +
                "    #[Route]\n" +
                "    public function otherAction()\n" +
                "    {\n" +
                "    }\n" +
                "\n" +
                "    public function <caret>indexAction()\n" +
                "    {\n" +
                "    }\n" +
                "}\n",
            "Symfony: Add Route attribute"
        );
    }

    public void testIntentionIsAvailableForControllerWithRouteOnClass() {
        assertIntentionIsAvailable(
            PhpFileType.INSTANCE,
            "<?php\n" +
                "namespace App\\Controller;\n" +
                "use Symfony\\Component\\Routing\\Attribute\\Route;\n" +
                "\n" +
                "#[Route(path: '/api')]\n" +
                "class TestController\n" +
                "{\n" +
                "    public function <caret>indexAction()\n" +
                "    {\n" +
                "    }\n" +
                "}\n",
            "Symfony: Add Route attribute"
        );
    }

    public void testIntentionNotAvailableForMethodAlreadyHavingRoute() {
        myFixture.configureByText(
            PhpFileType.INSTANCE,
            "<?php\n" +
                "namespace App\\Controller;\n" +
                "use Symfony\\Bundle\\FrameworkBundle\\Controller\\AbstractController;\n" +
                "use Symfony\\Component\\Routing\\Attribute\\Route;\n" +
                "\n" +
                "class TestController extends AbstractController\n" +
                "{\n" +
                "    #[Route(name: 'existing')]\n" +
                "    public function <caret>indexAction()\n" +
                "    {\n" +
                "    }\n" +
                "}\n"
        );

        assertFalse(
            myFixture.filterAvailableIntentions("Symfony: Add Route attribute")
                .stream()
                .anyMatch(action -> action.getText().equals("Symfony: Add Route attribute"))
        );
    }

    public void testIntentionNotAvailableForPrivateMethod() {
        myFixture.configureByText(
            PhpFileType.INSTANCE,
            "<?php\n" +
                "namespace App\\Controller;\n" +
                "use Symfony\\Bundle\\FrameworkBundle\\Controller\\AbstractController;\n" +
                "\n" +
                "class TestController extends AbstractController\n" +
                "{\n" +
                "    private function <caret>helperMethod()\n" +
                "    {\n" +
                "    }\n" +
                "}\n"
        );

        assertFalse(
            myFixture.filterAvailableIntentions("Symfony: Add Route attribute")
                .stream()
                .anyMatch(action -> action.getText().equals("Symfony: Add Route attribute"))
        );
    }

    public void testIntentionNotAvailableForStaticMethod() {
        myFixture.configureByText(
            PhpFileType.INSTANCE,
            "<?php\n" +
                "namespace App\\Controller;\n" +
                "use Symfony\\Bundle\\FrameworkBundle\\Controller\\AbstractController;\n" +
                "\n" +
                "class TestController extends AbstractController\n" +
                "{\n" +
                "    public static function <caret>staticMethod()\n" +
                "    {\n" +
                "    }\n" +
                "}\n"
        );

        assertFalse(
            myFixture.filterAvailableIntentions("Symfony: Add Route attribute")
                .stream()
                .anyMatch(action -> action.getText().equals("Symfony: Add Route attribute"))
        );
    }

    public void testIntentionNotAvailableForNonControllerClass() {
        myFixture.configureByText(
            PhpFileType.INSTANCE,
            "<?php\n" +
                "namespace App\\Service;\n" +
                "\n" +
                "class TestService\n" +
                "{\n" +
                "    public function <caret>doSomething()\n" +
                "    {\n" +
                "    }\n" +
                "}\n"
        );

        assertFalse(
            myFixture.filterAvailableIntentions("Symfony: Add Route attribute")
                .stream()
                .anyMatch(action -> action.getText().equals("Symfony: Add Route attribute"))
        );
    }

    public void testIntentionAddsRouteAttributeWithPathAndName() {
        myFixture.configureByText(
            PhpFileType.INSTANCE,
            "<?php\n" +
                "namespace App\\Controller;\n" +
                "use Symfony\\Bundle\\FrameworkBundle\\Controller\\AbstractController;\n" +
                "\n" +
                "class TestController extends AbstractController\n" +
                "{\n" +
                "    public function <caret>indexAction()\n" +
                "    {\n" +
                "    }\n" +
                "}\n"
        );

        var intention = myFixture.findSingleIntention("Symfony: Add Route attribute");
        myFixture.launchAction(intention);

        String result = myFixture.getFile().getText();

        assertTrue("Should have Route attribute with path", result.contains("#[Route('/test'"));
        assertTrue("Should have generated route name", result.contains("name: 'app_test_index'"));
    }

    public void testIntentionAddsRouteAttributeWithMethodNameInPath() {
        myFixture.configureByText(
            PhpFileType.INSTANCE,
            "<?php\n" +
                "namespace App\\Controller;\n" +
                "use Symfony\\Bundle\\FrameworkBundle\\Controller\\AbstractController;\n" +
                "\n" +
                "class ProductController extends AbstractController\n" +
                "{\n" +
                "    public function <caret>showAction()\n" +
                "    {\n" +
                "    }\n" +
                "}\n"
        );

        var intention = myFixture.findSingleIntention("Symfony: Add Route attribute");
        myFixture.launchAction(intention);

        String result = myFixture.getFile().getText();

        assertTrue("Should have Route attribute with path including method name", result.contains("#[Route('/product/show'"));
        assertTrue("Should have generated route name", result.contains("name: 'app_product_show'"));
    }

    public void testIntentionAddsRouteAttributeWithNestedNamespace() {
        myFixture.configureByText(
            PhpFileType.INSTANCE,
            "<?php\n" +
                "namespace App\\Controller\\Admin;\n" +
                "use Symfony\\Bundle\\FrameworkBundle\\Controller\\AbstractController;\n" +
                "\n" +
                "class UserController extends AbstractController\n" +
                "{\n" +
                "    public function <caret>editAction()\n" +
                "    {\n" +
                "    }\n" +
                "}\n"
        );

        var intention = myFixture.findSingleIntention("Symfony: Add Route attribute");
        myFixture.launchAction(intention);

        String result = myFixture.getFile().getText();

        assertTrue("Should have Route attribute with nested path", result.contains("#[Route('/admin/user/edit'"));
        assertTrue("Should have generated route name", result.contains("name: 'app_admin_user_edit'"));
    }
}
