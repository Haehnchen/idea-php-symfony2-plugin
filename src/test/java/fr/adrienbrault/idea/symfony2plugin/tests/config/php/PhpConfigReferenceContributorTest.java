package fr.adrienbrault.idea.symfony2plugin.tests.config.php;

import com.jetbrains.php.lang.PhpFileType;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

import java.io.File;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.config.php.PhpConfigReferenceContributor
 */
public class PhpConfigReferenceContributorTest extends SymfonyLightCodeInsightFixtureTestCase {
    public void setUp() throws Exception {
        super.setUp();
        myFixture.copyFileToProject("classes.php");
        myFixture.copyFileToProject("tags.yml");
    }

    public String getTestDataPath() {
        return new File(this.getClass().getResource("fixtures").getFile()).getAbsolutePath();
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
