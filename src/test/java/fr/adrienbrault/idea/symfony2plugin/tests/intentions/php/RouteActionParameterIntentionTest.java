package fr.adrienbrault.idea.symfony2plugin.tests.intentions.php;

import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.php.lang.PhpFileType;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.intentions.php.RouteActionParameterIntention;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

import java.util.List;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.intentions.php.RouteActionParameterIntention
 */
public class RouteActionParameterIntentionTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void setUp() throws Exception {
        super.setUp();
        myFixture.configureFromExistingVirtualFile(myFixture.copyFileToProject("classes.php"));
    }

    public String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/intentions/php/fixtures";
    }

    public void testIntentionIsAvailableForRouteActionWithAttribute() {
        assertIntentionIsAvailable(
            PhpFileType.INSTANCE,
            "<?php\n" +
                "namespace App\\Controller;\n" +
                "use Symfony\\Component\\Routing\\Attribute\\Route;\n" +
                "\n" +
                "class TestController\n" +
                "{\n" +
                "    #[Route('/test')]\n" +
                "    public function <caret>index(): void\n" +
                "    {\n" +
                "    }\n" +
                "}\n",
            "Symfony: Add parameter to route action"
        );
    }

    public void testIntentionIsAvailableForRouteActionWithAnnotation() {
        assertIntentionIsAvailable(
            PhpFileType.INSTANCE,
            "<?php\n" +
                "namespace App\\Controller;\n" +
                "use Symfony\\Component\\Routing\\Annotation\\Route;\n" +
                "\n" +
                "class TestController\n" +
                "{\n" +
                "    /**\n" +
                "     * @Route(\"/test\")\n" +
                "     */\n" +
                "    public function <caret>index(): void\n" +
                "    {\n" +
                "    }\n" +
                "}\n",
            "Symfony: Add parameter to route action"
        );
    }

    public void testIntentionIsAvailableForInvokeWithClassLevelRoute() {
        assertIntentionIsAvailable(
            PhpFileType.INSTANCE,
            "<?php\n" +
                "namespace App\\Controller;\n" +
                "use Symfony\\Component\\Routing\\Attribute\\Route;\n" +
                "\n" +
                "#[Route('/test')]\n" +
                "class TestController\n" +
                "{\n" +
                "    public function <caret>__invoke(): void\n" +
                "    {\n" +
                "    }\n" +
                "}\n",
            "Symfony: Add parameter to route action"
        );
    }

    public void testIntentionIsNotAvailableForPrivateMethod() {
        myFixture.configureByText(
            PhpFileType.INSTANCE,
            "<?php\n" +
                "namespace App\\Controller;\n" +
                "use Symfony\\Component\\Routing\\Attribute\\Route;\n" +
                "\n" +
                "class TestController\n" +
                "{\n" +
                "    #[Route('/test')]\n" +
                "    private function <caret>index(): void\n" +
                "    {\n" +
                "    }\n" +
                "}\n"
        );

        assertFalse(myFixture.filterAvailableIntentions("Symfony: Add parameter to route action").stream().findFirst().isPresent());
    }

    public void testIntentionIsNotAvailableForMethodWithoutRoute() {
        myFixture.configureByText(
            PhpFileType.INSTANCE,
            "<?php\n" +
                "namespace App\\Controller;\n" +
                "\n" +
                "class TestController\n" +
                "{\n" +
                "    public function <caret>index(): void\n" +
                "    {\n" +
                "    }\n" +
                "}\n"
        );

        assertFalse(myFixture.filterAvailableIntentions("Symfony: Add parameter to route action").stream().findFirst().isPresent());
    }

    public void testGetAvailableParameterFqnsReturnsAllWhenEmpty() {
        myFixture.configureByText(
            PhpFileType.INSTANCE,
            "<?php\n" +
                "namespace App\\Controller;\n" +
                "\n" +
                "class TestController\n" +
                "{\n" +
                "    public function index(): void\n" +
                "    {\n" +
                "    }\n" +
                "}\n"
        );

        PhpClass phpClass = PsiTreeUtil.findChildOfType(myFixture.getFile(), PhpClass.class);
        assertNotNull(phpClass);

        Method indexMethod = phpClass.findOwnMethodByName("index");
        assertNotNull(indexMethod);

        List<String> availableParams = RouteActionParameterIntention.getAvailableParameterFqns(indexMethod);

        assertTrue(availableParams.contains("Symfony\\Component\\HttpFoundation\\Request"));
        assertTrue(availableParams.contains("Symfony\\Component\\Security\\Core\\User\\UserInterface"));
    }

    public void testGetAvailableParameterFqnsFiltersExisting() {
        myFixture.configureByText(
            PhpFileType.INSTANCE,
            "<?php\n" +
                "namespace App\\Controller;\n" +
                "use Symfony\\Component\\HttpFoundation\\Request;\n" +
                "\n" +
                "class TestController\n" +
                "{\n" +
                "    public function index(Request $request): void\n" +
                "    {\n" +
                "    }\n" +
                "}\n"
        );

        PhpClass phpClass = PsiTreeUtil.findChildOfType(myFixture.getFile(), PhpClass.class);
        assertNotNull(phpClass);

        Method indexMethod = phpClass.findOwnMethodByName("index");
        assertNotNull(indexMethod);

        List<String> availableParams = RouteActionParameterIntention.getAvailableParameterFqns(indexMethod);

        assertFalse(availableParams.contains("Symfony\\Component\\HttpFoundation\\Request"));
        assertTrue(availableParams.contains("Symfony\\Component\\Security\\Core\\User\\UserInterface"));
    }
}
