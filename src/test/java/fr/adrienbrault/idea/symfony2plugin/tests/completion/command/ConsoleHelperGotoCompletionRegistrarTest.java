package fr.adrienbrault.idea.symfony2plugin.tests.completion.command;

import com.intellij.patterns.PlatformPatterns;
import com.jetbrains.php.lang.PhpFileType;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ConsoleHelperGotoCompletionRegistrarTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void setUp() throws Exception {
        super.setUp();
        myFixture.copyFileToProject("helper.php");
    }

    public String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/completion/command/fixtures";
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.completion.command.ConsoleHelperGotoCompletionRegistrar
     */
    public void testGetHelper() {
        assertCompletionContains(PhpFileType.INSTANCE, "<?php" +
                "/** @var \\Symfony\\Component\\Console\\Command\\Command $foo */\n" +
                "$foo->getHelper('<caret>');",
            "foo"
        );

        assertNavigationMatch(PhpFileType.INSTANCE, "<?php" +
                "/** @var \\Symfony\\Component\\Console\\Command\\Command $foo */\n" +
                "$foo->getHelper('fo<caret>o');",
            PlatformPatterns.psiElement(PhpClass.class)
        );
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.completion.command.ConsoleHelperGotoCompletionRegistrar
     */
    public void testHelperSet() {
        assertCompletionContains(PhpFileType.INSTANCE, "<?php" +
                "/** @var \\Symfony\\Component\\Console\\Helper\\HelperSet $foo */\n" +
                "$foo->has('<caret>');",
            "foo"
        );

        assertNavigationMatch(PhpFileType.INSTANCE, "<?php" +
                "/** @var \\Symfony\\Component\\Console\\Helper\\HelperSet $foo */\n" +
                "$foo->has('fo<caret>o');",
            PlatformPatterns.psiElement(PhpClass.class)
        );

        assertCompletionContains(PhpFileType.INSTANCE, "<?php" +
                "/** @var \\Symfony\\Component\\Console\\Helper\\HelperSet $foo */\n" +
                "$foo->get('<caret>');",
            "foo"
        );

        assertNavigationMatch(PhpFileType.INSTANCE, "<?php" +
                "/** @var \\Symfony\\Component\\Console\\Helper\\HelperSet $foo */\n" +
                "$foo->get('fo<caret>o');",
            PlatformPatterns.psiElement(PhpClass.class)
        );
    }
}
