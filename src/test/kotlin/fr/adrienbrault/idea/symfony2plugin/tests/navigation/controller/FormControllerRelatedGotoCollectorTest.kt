package fr.adrienbrault.idea.symfony2plugin.tests.navigation.controller

import com.jetbrains.php.lang.PhpFileType
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 *
 * @see fr.adrienbrault.idea.symfony2plugin.navigation.controller.FormControllerRelatedGotoCollector
 */
class FormControllerRelatedGotoCollectorTest : SymfonyLightCodeInsightFixtureTestCase() {
    override fun setUp() {
        super.setUp()
        myFixture.configureFromExistingVirtualFile(myFixture.copyFileToProject("classes.php"))
    }

    override fun getTestDataPath(): String {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/navigation/controller/fixtures"
    }

    fun testThatFormFactoryCreateProvidesLineMarker() {
        assertLineMarker(
            myFixture.configureByText(
                PhpFileType.INSTANCE,
                "<?php\n" +
                    "class Bar {\n" +
                    "   function fooAction(\\Symfony\\Component\\Form\\FormFactoryInterface \$form)" +
                    "   {\n" +
                    "       \$form->create(\\App\\MyFormType::class);" +
                    "   }\n" +
                    "}",
            ),
            LineMarker.ToolTipEqualsAssert("App\\MyFormType"),
        )

        assertLineMarker(
            myFixture.configureByText(
                PhpFileType.INSTANCE,
                "<?php\n" +
                    "class Bar {\n" +
                    "   function fooAction(\\Symfony\\Component\\Form\\FormFactoryInterface \$form)" +
                    "   {\n" +
                    "       \$form->create(new \\App\\MyFormType()));" +
                    "   }\n" +
                    "}",
            ),
            LineMarker.ToolTipEqualsAssert("App\\MyFormType"),
        )
    }

    fun testThatFormFactoryNamedCreateProvidesLineMarker() {
        assertLineMarker(
            myFixture.configureByText(
                PhpFileType.INSTANCE,
                "<?php\n" +
                    "class Bar {\n" +
                    "   function fooAction(\\Symfony\\Component\\Form\\FormFactoryInterface \$form)" +
                    "   {\n" +
                    "       \$form->createNamed('Foobar', \\App\\MyFormType::class);" +
                    "   }\n" +
                    "}",
            ),
            LineMarker.ToolTipEqualsAssert("App\\MyFormType"),
        )
    }
}
