package fr.adrienbrault.idea.symfony2plugin.tests.translation;

import com.jetbrains.php.lang.PhpFileType;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

import java.io.File;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.translation.ValidatorTranslationGotoCompletionRegistrar
 */
public class ValidatorTranslationGotoCompletionRegistrarTest extends SymfonyLightCodeInsightFixtureTestCase {
    public void setUp() throws Exception {
        super.setUp();
        myFixture.copyFileToProject("classes.php");
        myFixture.copyFileToProject("validators.de.yml", "Resources/translations/validators.de.yml");
    }

    protected String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/translation/fixtures";
    }

    public void testThatMessageValueForConstraintProvideValidatorTranslations() {
        assertCompletionContains(PhpFileType.INSTANCE, "<?php\n" +
                "$f = new MyConstraintMessage(['message' => '<caret>'])",
            "foo_yaml.symfony.great"
        );

        assertNavigationMatch(PhpFileType.INSTANCE, "<?php\n" +
            "$f = new MyConstraintMessage(['message' => 'foo_yaml.symfony<caret>.great'])"
        );
    }

    public void testThatExecutionContextProvidesTranslation() {
        assertCompletionContains(PhpFileType.INSTANCE, "<?php\n" +
                "/** @var $f \\Symfony\\Component\\Validator\\Context\\ExecutionContextInterface */\n" +
                "$f->addViolation('<caret>');",
            "foo_yaml.symfony.great"
        );

        assertNavigationMatch(PhpFileType.INSTANCE, "<?php\n" +
            "/** @var $f \\Symfony\\Component\\Validator\\Context\\ExecutionContextInterface */\n" +
            "$f->addViolation('foo_yaml.sym<caret>fony.great');"
        );
    }

    public void testThatConstraintViolationBuilderProvidesSetTranslationDomain() {
        assertCompletionContains(PhpFileType.INSTANCE, "<?php\n" +
                "/** @var $f \\Symfony\\Component\\Validator\\Violation\\ConstraintViolationBuilderInterface */\n" +
                "$f->setTranslationDomain('<caret>');",
            "validators"
        );

        assertNavigationMatch(PhpFileType.INSTANCE, "<?php\n" +
            "/** @var $f \\Symfony\\Component\\Validator\\Violation\\ConstraintViolationBuilderInterface */\n" +
            "$f->setTranslationDomain('vali<caret>dators');"
        );
    }
}
