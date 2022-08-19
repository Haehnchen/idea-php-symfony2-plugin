package fr.adrienbrault.idea.symfony2plugin.tests.navigation.controller;

import com.jetbrains.php.lang.PhpFileType;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 *
 * @see fr.adrienbrault.idea.symfony2plugin.navigation.controller.FormControllerRelatedGotoCollector
 */
public class FormControllerRelatedGotoCollectorTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void setUp() throws Exception {
        super.setUp();
        myFixture.configureFromExistingVirtualFile(myFixture.copyFileToProject("classes.php"));
    }

    protected String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/navigation/controller/fixtures";
    }

    public void testThatFormFactoryCreateProvidesLineMarker() {
        assertLineMarker(myFixture.configureByText(PhpFileType.INSTANCE, "<?php\n" +
            "class Bar {\n" +
            "   function fooAction(\\Symfony\\Component\\Form\\FormFactoryInterface $form)" +
            "   {\n" +
            "       $form->create(\\App\\MyFormType::class);" +
            "   }\n" +
            "}"
        ), new LineMarker.ToolTipEqualsAssert("App\\MyFormType"));

        assertLineMarker(myFixture.configureByText(PhpFileType.INSTANCE, "<?php\n" +
            "class Bar {\n" +
            "   function fooAction(\\Symfony\\Component\\Form\\FormFactoryInterface $form)" +
            "   {\n" +
            "       $form->create(new \\App\\MyFormType()));" +
            "   }\n" +
            "}"
        ), new LineMarker.ToolTipEqualsAssert("App\\MyFormType"));
    }

    public void testThatFormFactoryNamedCreateProvidesLineMarker() {
        assertLineMarker(myFixture.configureByText(PhpFileType.INSTANCE, "<?php\n" +
            "class Bar {\n" +
            "   function fooAction(\\Symfony\\Component\\Form\\FormFactoryInterface $form)" +
            "   {\n" +
            "       $form->createNamed('Foobar', \\App\\MyFormType::class);" +
            "   }\n" +
            "}"
        ), new LineMarker.ToolTipEqualsAssert("App\\MyFormType"));
    }
}
