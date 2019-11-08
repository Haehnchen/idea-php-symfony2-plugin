package fr.adrienbrault.idea.symfonyplugin.tests.config;

import com.jetbrains.php.lang.PhpFileType;
import fr.adrienbrault.idea.symfonyplugin.tests.SymfonyLightCodeInsightFixtureTestCase;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 *
 * @see fr.adrienbrault.idea.symfonyplugin.config.SymfonyPhpReferenceContributor
 */
public class SymfonyPhpReferenceContributorTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void setUp() throws Exception {
        super.setUp();
        myFixture.copyFileToProject("services.xml");
        myFixture.copyFileToProject("ServiceLineMarkerProvider.php");
    }

    public String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfonyplugin/tests/config/fixtures";
    }

    public void testThatPrivateServiceAreNotInCompletionListForContainerGet() {
        assertCompletionContains(PhpFileType.INSTANCE, "<?php" +
                "/** @var $c \\Symfony\\Component\\DependencyInjection\\ContainerInterface */\n" +
                "$c->get('<caret>');",
            "my.public.service"
        );

        assertCompletionNotContains(PhpFileType.INSTANCE, "<?php" +
                "/** @var $c \\Symfony\\Component\\DependencyInjection\\ContainerInterface */\n" +
                "$c->get('<caret>');",
            "my.private.service"
        );
    }
}
