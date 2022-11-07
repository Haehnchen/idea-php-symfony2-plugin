package fr.adrienbrault.idea.symfony2plugin.tests.completion;

import com.jetbrains.php.lang.PhpFileType;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

/**
 * Check git submodules and libraryRoot for PhpStorm metadata files
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class LibraryMetaRootCompletionTest extends SymfonyLightCodeInsightFixtureTestCase {
    public void setUp() throws Exception {
        super.setUp();
        myFixture.copyFileToProject("classes.php");
    }

    public String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/completion/fixtures";
    }

    public void testThatSymfonyMetaRootIsProvidedAndIndexed() {
        assertCompletionContains(PhpFileType.INSTANCE, "<?php\n" +
                "/** @var \\Symfony\\Component\\HttpFoundation\\Request $request */\n" +
                "if ($request->getMethod() === '<caret>') {}",
            "GET"
        );
    }

    public void testThatDoctrineMetaRootIsProvidedAndIndexed() {
        assertCompletionContains(PhpFileType.INSTANCE, "<?php\n" +
                "/** @var \\Doctrine\\ORM\\QueryBuilder $qb */\n" +
                "$qb->add('<caret>');",
            "from"
        );
    }
}
