package fr.adrienbrault.idea.symfony2plugin.tests.doctrine.completion;

import com.jetbrains.php.lang.PhpFileType;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 *
 * @see fr.adrienbrault.idea.symfony2plugin.doctrine.completion.DoctrineCompletionContributor
 */
public class DoctrineCompletionContributorTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void setUp() throws Exception {
        super.setUp();
        myFixture.configureFromExistingVirtualFile(myFixture.copyFileToProject("classes.php"));
    }

    public String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/doctrine/completion/fixtures";
    }

    public void testClassConstantsCompletionWithoutNamespace() {
        assertCompletionResultEquals(PhpFileType.INSTANCE,
            "<?php\n /** @var $f \\Doctrine\\Common\\Persistence\\ObjectManager */\n $f->getRepository(FooMod<caret>)",
            "<?php\n /** @var $f \\Doctrine\\Common\\Persistence\\ObjectManager */\n $f->getRepository(\\My\\Model\\FooModel::class)",
            new LookupElementInsert.Icon(Symfony2Icons.DOCTRINE)
        );
        assertCompletionResultEquals(PhpFileType.INSTANCE,
            "<?php\n /** @var $f \\Doctrine\\Common\\Persistence\\ManagerRegistry */\n $f->getRepository(FooMod<caret>)",
            "<?php\n /** @var $f \\Doctrine\\Common\\Persistence\\ManagerRegistry */\n $f->getRepository(\\My\\Model\\FooModel::class)",
            new LookupElementInsert.Icon(Symfony2Icons.DOCTRINE)
        );
    }

    public void testClassConstantsCompletionWithNamespaceShouldInsertUseAndStripNamespace() {
        assertCompletionResultEquals(PhpFileType.INSTANCE,
            "<?php\n" +
                "namespace Foo {\n" +
                "  /** @var $f \\Doctrine\\Common\\Persistence\\ObjectManager */\n" +
                "  $f->getRepository(FooMod<caret>);\n" +
                "};",
            "<?php\n" +
                "namespace Foo {\n\n" +
                "    use My\\Model\\FooModel;\n\n" +
                "    /** @var $f \\Doctrine\\Common\\Persistence\\ObjectManager */\n" +
                "  $f->getRepository(FooModel::class);\n" +
                "};",
            new LookupElementInsert.Icon(Symfony2Icons.DOCTRINE)
        );
    }
}
