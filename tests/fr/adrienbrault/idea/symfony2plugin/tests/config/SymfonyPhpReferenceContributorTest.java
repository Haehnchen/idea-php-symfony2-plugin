package fr.adrienbrault.idea.symfony2plugin.tests.config;

import com.jetbrains.php.lang.PhpFileType;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

import java.io.File;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 *
 * @see fr.adrienbrault.idea.symfony2plugin.config.SymfonyPhpReferenceContributor
 */
public class SymfonyPhpReferenceContributorTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void setUp() throws Exception {
        super.setUp();
        myFixture.copyFileToProject("services.xml");
        myFixture.copyFileToProject("ServiceLineMarkerProvider.php");
    }

    public String getTestDataPath() {
        return new File(this.getClass().getResource("fixtures").getFile()).getAbsolutePath();
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
