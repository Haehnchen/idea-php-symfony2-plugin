package fr.adrienbrault.idea.symfony2plugin.tests.config.php;

import com.jetbrains.php.lang.PhpFileType;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.config.php.PhpConfigReferenceContributor
 */
public class PhpConfigReferenceContributorTest extends SymfonyLightCodeInsightFixtureTestCase {
    public void setUp() throws Exception {
        super.setUp();
        myFixture.copyFileToProject("classes.php");
        myFixture.copyFileToProject("tags.yml");
        myFixture.copyFileToProject("services.yml");
    }

    public String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/config/php/fixtures";
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

    public void testEventNameCompletionForAsEventListener() {
        assertCompletionContains(PhpFileType.INSTANCE, "<?php\n" +
                "namespace App\\EventListener;\n" +
                "\n" +
                "use Symfony\\Component\\EventDispatcher\\Attribute\\AsEventListener;\n" +
                "\n" +
                "#[AsEventListener(event: '<caret>')]\n" +
                "final class MyMultiListener implements \\Symfony\\Component\\EventDispatcher\\EventSubscriberInterface\n" +
                "{\n" +
                "\n" +
                "}",
            "yaml_event_2"
        );
    }
}
