package fr.adrienbrault.idea.symfony2plugin.tests.form.action.generator;

import com.jetbrains.php.lang.PhpFileType;
import fr.adrienbrault.idea.symfony2plugin.form.action.generator.FormTypeConstantMigrationAction;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.form.action.generator.FormTypeConstantMigrationAction
 */
public class FormTypeConstantMigrationActionTest extends SymfonyLightCodeInsightFixtureTestCase {
    public void setUp() throws Exception {
        super.setUp();
        myFixture.configureFromExistingVirtualFile(myFixture.copyFileToProject("classes.php"));
    }

    protected String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/form/action/generator/fixtures";
    }

    public void testActionAvailableForFileScope() {
        myFixture.configureByText(PhpFileType.INSTANCE, "" +
            "<?php\n" +
            "class Foo implements \\Symfony\\Component\\Form\\FormTypeInterface\n" +
            "{" +
            "  \n private $foo = 'te<caret>st';\n" +
            "} "
        );

        assertTrue(myFixture.testAction(new FormTypeConstantMigrationAction()).isEnabledAndVisible());
    }
}
