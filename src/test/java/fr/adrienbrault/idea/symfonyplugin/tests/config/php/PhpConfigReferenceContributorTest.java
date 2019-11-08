package fr.adrienbrault.idea.symfonyplugin.tests.config.php;

import com.jetbrains.php.lang.PhpFileType;
import fr.adrienbrault.idea.symfonyplugin.tests.SymfonyLightCodeInsightFixtureTestCase;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfonyplugin.config.php.PhpConfigReferenceContributor
 */
public class PhpConfigReferenceContributorTest extends SymfonyLightCodeInsightFixtureTestCase {
    public void setUp() throws Exception {
        super.setUp();
        myFixture.copyFileToProject("classes.php");
        myFixture.copyFileToProject("tags.yml");
    }

    public String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfonyplugin/tests/config/php/fixtures";
    }

    public void testTagReferences() {
        assertCompletionContains(PhpFileType.INSTANCE, "<?php\n" +
                "/** @var $x \\Symfony\\Component\\DependencyInjection\\Definition */\n" +
                "$x->addTag('<caret>')",
            "foobar"
        );

        assertCompletionContains(PhpFileType.INSTANCE, "<?php\n" +
                "/** @var $x \\Symfony\\Component\\DependencyInjection\\Definition */\n" +
                "$x->clearTag('<caret>')",
            "foobar"
        );

        assertCompletionContains(PhpFileType.INSTANCE, "<?php\n" +
                "/** @var $x \\Symfony\\Component\\DependencyInjection\\Definition */\n" +
                "$x->hasTag('<caret>')",
            "foobar"
        );
    }
}
