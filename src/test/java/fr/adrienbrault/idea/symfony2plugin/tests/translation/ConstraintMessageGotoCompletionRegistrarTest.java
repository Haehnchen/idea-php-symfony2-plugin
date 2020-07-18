package fr.adrienbrault.idea.symfony2plugin.tests.translation;

import com.intellij.patterns.PlatformPatterns;
import com.jetbrains.php.lang.PhpFileType;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.translation.ConstraintMessageGotoCompletionRegistrar
 */
public class ConstraintMessageGotoCompletionRegistrarTest extends SymfonyLightCodeInsightFixtureTestCase {
    public void setUp() throws Exception {
        super.setUp();
        myFixture.copyFileToProject("ConstraintMessageGotoCompletionRegistrar.php");
        myFixture.copyFileToProject("validators.de.yml", "Resources/translations/validators.de.yml");
    }

    protected String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/translation/fixtures";
    }

    public void testThatPropertyStartingWithMessageInsideConstraintImplementationProvidesMessageCompletion() {
        assertCompletionContains(
            PhpFileType.INSTANCE,
            "<?php\n" +
                "class FooConstraint extends \\Symfony\\Component\\Validator\\Constraint\n" +
                "{\n" +
                "    public $message = '<caret>';\n" +
                "}",
            "validator_message"
        );
    }

    public void testThatPropertyStartingWithMessageInsideConstraintImplementationProvidesMessageNavigation() {
        assertNavigationMatch(
            PhpFileType.INSTANCE,
            "<?php\n" +
                "class FooConstraint extends \\Symfony\\Component\\Validator\\Constraint\n" +
                "{\n" +
                "    public $message = 'validato<caret>r_message';\n" +
                "}",
            PlatformPatterns.psiElement()
        );
    }
}
