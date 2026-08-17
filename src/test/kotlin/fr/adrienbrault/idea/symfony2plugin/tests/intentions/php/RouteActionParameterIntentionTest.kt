package fr.adrienbrault.idea.symfony2plugin.tests.intentions.php

import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.php.lang.PhpFileType
import com.jetbrains.php.lang.psi.elements.PhpClass
import fr.adrienbrault.idea.symfony2plugin.intentions.php.getAvailableRouteActionParameterFqns
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.intentions.php.RouteActionParameterIntention
 */
class RouteActionParameterIntentionTest : SymfonyLightCodeInsightFixtureTestCase() {
    override fun setUp() {
        super.setUp()
        myFixture.configureFromExistingVirtualFile(myFixture.copyFileToProject("classes.php"))
    }

    override fun getTestDataPath(): String {
        return "src/test/kotlin/fr/adrienbrault/idea/symfony2plugin/tests/intentions/php/fixtures"
    }

    fun testIntentionIsAvailableForRouteActionWithAttribute() {
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
        )
    }

    fun testIntentionIsAvailableForRouteActionWithAnnotation() {
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
        )
    }

    fun testIntentionIsAvailableForInvokeWithClassLevelRoute() {
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
        )
    }

    fun testIntentionIsNotAvailableForPrivateMethod() {
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
        )

        assertFalse(myFixture.filterAvailableIntentions("Symfony: Add parameter to route action").firstOrNull() != null)
    }

    fun testIntentionIsNotAvailableForMethodWithoutRoute() {
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
        )

        assertFalse(myFixture.filterAvailableIntentions("Symfony: Add parameter to route action").firstOrNull() != null)
    }

    fun testGetAvailableParameterFqnsReturnsAllWhenEmpty() {
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
        )

        val phpClass = PsiTreeUtil.findChildOfType(myFixture.file, PhpClass::class.java)
        assertNotNull(phpClass)

        val indexMethod = phpClass!!.findOwnMethodByName("index")
        assertNotNull(indexMethod)

        val availableParams = getAvailableRouteActionParameterFqns(indexMethod!!)

        assertTrue(availableParams.contains("Symfony\\Component\\HttpFoundation\\Request"))
        assertTrue(availableParams.contains("Symfony\\Component\\Security\\Core\\User\\UserInterface"))
    }

    fun testGetAvailableParameterFqnsFiltersExisting() {
        myFixture.configureByText(
            PhpFileType.INSTANCE,
            "<?php\n" +
                "namespace App\\Controller;\n" +
                "use Symfony\\Component\\HttpFoundation\\Request;\n" +
                "\n" +
                "class TestController\n" +
                "{\n" +
                "    public function index(Request \$request): void\n" +
                "    {\n" +
                "    }\n" +
                "}\n"
        )

        val phpClass = PsiTreeUtil.findChildOfType(myFixture.file, PhpClass::class.java)
        assertNotNull(phpClass)

        val indexMethod = phpClass!!.findOwnMethodByName("index")
        assertNotNull(indexMethod)

        val availableParams = getAvailableRouteActionParameterFqns(indexMethod!!)

        assertFalse(availableParams.contains("Symfony\\Component\\HttpFoundation\\Request"))
        assertTrue(availableParams.contains("Symfony\\Component\\Security\\Core\\User\\UserInterface"))
    }
}
