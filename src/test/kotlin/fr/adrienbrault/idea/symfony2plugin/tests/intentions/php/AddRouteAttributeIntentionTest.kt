package fr.adrienbrault.idea.symfony2plugin.tests.intentions.php

import com.jetbrains.php.lang.PhpFileType
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.intentions.php.AddRouteAttributeIntention
 */
class AddRouteAttributeIntentionTest : SymfonyLightCodeInsightFixtureTestCase() {
    override fun setUp() {
        super.setUp()
        myFixture.configureFromExistingVirtualFile(myFixture.copyFileToProject("classes.php"))
    }

    override fun getTestDataPath(): String {
        return "src/test/kotlin/fr/adrienbrault/idea/symfony2plugin/tests/intentions/php/fixtures"
    }

    fun testIntentionIsAvailableForControllerExtendingAbstractController() {
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
        )
    }

    fun testIntentionIsAvailableForControllerWithAsControllerAttribute() {
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
        )
    }

    fun testIntentionIsAvailableForControllerWithRouteOnAnotherMethod() {
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
        )
    }

    fun testIntentionIsAvailableForControllerWithRouteOnClass() {
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
        )
    }

    fun testIntentionNotAvailableForMethodAlreadyHavingRoute() {
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
        )

        assertFalse(
            myFixture.filterAvailableIntentions("Symfony: Add Route attribute")
                .any { action -> action.text == "Symfony: Add Route attribute" }
        )
    }

    fun testIntentionNotAvailableForPrivateMethod() {
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
        )

        assertFalse(
            myFixture.filterAvailableIntentions("Symfony: Add Route attribute")
                .any { action -> action.text == "Symfony: Add Route attribute" }
        )
    }

    fun testIntentionNotAvailableForStaticMethod() {
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
        )

        assertFalse(
            myFixture.filterAvailableIntentions("Symfony: Add Route attribute")
                .any { action -> action.text == "Symfony: Add Route attribute" }
        )
    }

    fun testIntentionNotAvailableForNonControllerClass() {
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
        )

        assertFalse(
            myFixture.filterAvailableIntentions("Symfony: Add Route attribute")
                .any { action -> action.text == "Symfony: Add Route attribute" }
        )
    }

    fun testIntentionAddsRouteAttributeWithPathAndName() {
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
        )

        val intention = myFixture.findSingleIntention("Symfony: Add Route attribute")
        myFixture.launchAction(intention)

        val result = myFixture.file.text

        assertTrue("Should have Route attribute with path", result.contains("#[Route('/test'"))
        assertTrue("Should have generated route name", result.contains("name: 'app_test_index'"))
    }

    fun testIntentionAddsRouteAttributeWithMethodNameInPath() {
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
        )

        val intention = myFixture.findSingleIntention("Symfony: Add Route attribute")
        myFixture.launchAction(intention)

        val result = myFixture.file.text

        assertTrue("Should have Route attribute with path including method name", result.contains("#[Route('/product/show'"))
        assertTrue("Should have generated route name", result.contains("name: 'app_product_show'"))
    }

    fun testIntentionAddsRouteAttributeWithNestedNamespace() {
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
        )

        val intention = myFixture.findSingleIntention("Symfony: Add Route attribute")
        myFixture.launchAction(intention)

        val result = myFixture.file.text

        assertTrue("Should have Route attribute with nested path", result.contains("#[Route('/admin/user/edit'"))
        assertTrue("Should have generated route name", result.contains("name: 'app_admin_user_edit'"))
    }
}
