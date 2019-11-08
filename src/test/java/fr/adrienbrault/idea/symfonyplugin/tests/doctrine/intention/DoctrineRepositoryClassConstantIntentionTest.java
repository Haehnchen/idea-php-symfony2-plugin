package fr.adrienbrault.idea.symfony2plugin.tests.doctrine.intention;

import com.jetbrains.php.lang.PhpFileType;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 *
 * @see fr.adrienbrault.idea.symfony2plugin.doctrine.intention.DoctrineRepositoryClassConstantIntention
 */
public class DoctrineRepositoryClassConstantIntentionTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void setUp() throws Exception {
        super.setUp();
        myFixture.configureFromExistingVirtualFile(myFixture.copyFileToProject("classes.php"));
    }

    public String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/doctrine/intention/fixtures";
    }

    public void testIntentionIsAvailable() {
        assertIntentionIsAvailable(PhpFileType.INSTANCE,
            "<?php\n /** @var $foo \\Doctrine\\Common\\Persistence\\ManagerRegistry */\n $foo->getRepository('foo<caret>')",
            "Doctrine: use class constant"
        );

        assertIntentionIsAvailable(PhpFileType.INSTANCE,
            "<?php\n /** @var $foo \\Doctrine\\Common\\Persistence\\ObjectManager */\n $foo->getRepository('foo<caret>')",
            "Doctrine: use class constant"
        );

        assertIntentionIsAvailable(PhpFileType.INSTANCE,
            "<?php\n /** @var $foo \\Doctrine\\Common\\Persistence\\ObjectManager */\n $foo->find('foo<caret>')",
            "Doctrine: use class constant"
        );
    }
}
