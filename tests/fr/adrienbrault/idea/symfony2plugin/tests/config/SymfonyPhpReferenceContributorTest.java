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
        myFixture.copyFileToProject("SymfonyPhpReferenceContributor.php");
        myFixture.copyFileToProject("services.xml");
    }

    public String getTestDataPath() {
        return new File(this.getClass().getResource("fixtures").getFile()).getAbsolutePath();
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.doctrine.ModelFieldReference
     */
    public void testModelFieldReference() {
        for (String s : new String[]{"findBy", "findOneBy"}) {
            assertCompletionContains(PhpFileType.INSTANCE, "<?php" +
                    "/** @var $em \\Doctrine\\Common\\Persistence\\ObjectManager */\n" +
                    "$em->getRepository('Foo\\Bar')->" + s + "(['<caret>'])",
                "phonenumbers", "email"
            );

            assertCompletionContains(PhpFileType.INSTANCE, "<?php" +
                    "/** @var $em \\Doctrine\\Common\\Persistence\\ObjectManager */\n" +
                    "$em->getRepository('Foo\\Bar')->" + s + "(['foo', '<caret>' => 'foo'])",
                "phonenumbers", "email"
            );

            assertCompletionContains(PhpFileType.INSTANCE, "<?php" +
                    "/** @var $em \\Doctrine\\Common\\Persistence\\ObjectManager */\n" +
                    "$em->getRepository('Foo\\Bar')->" + s + "(['foo' => 'foo', '<caret>' => 'foo'])",
                "phonenumbers", "email"
            );

            // migrate: @TODO: fr.adrienbrault.idea.symfony2plugin.doctrine.ModelFieldReference.multiResolve()
            // add navigation testing
        }
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
