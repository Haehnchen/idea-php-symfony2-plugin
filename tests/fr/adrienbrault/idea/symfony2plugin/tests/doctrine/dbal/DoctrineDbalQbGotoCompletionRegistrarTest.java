package fr.adrienbrault.idea.symfony2plugin.tests.doctrine.dbal;

import com.jetbrains.php.lang.PhpFileType;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

import java.io.File;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.doctrine.querybuilder.dbal.DoctrineDbalQbGotoCompletionRegistrar
 */
public class DoctrineDbalQbGotoCompletionRegistrarTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void setUp() throws Exception {
        super.setUp();
        myFixture.configureFromExistingVirtualFile(myFixture.copyFileToProject("classes.php"));
        myFixture.configureFromExistingVirtualFile(myFixture.copyFileToProject("doctrine.orm.xml"));
    }

    public String getTestDataPath() {
        return new File(this.getClass().getResource("fixtures").getFile()).getAbsolutePath();
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.doctrine.querybuilder.dbal.DoctrineDbalQbGotoCompletionRegistrar
     */
    public void testDBALTableNameCompletion() {

        for (String s : new String[]{"update", "insert", "from"}) {
            assertCompletionContains(PhpFileType.INSTANCE, "<?php" +
                    "/** @var $foo \\Doctrine\\DBAL\\Query\\QueryBuilder */\n" +
                    "$foo->" + s + "('<caret>');",
                "cms_users"
            );
        }

        for (String s : new String[]{"innerJoin", "leftJoin", "join", "rightJoin"}) {
            assertCompletionContains(PhpFileType.INSTANCE, "<?php" +
                    "/** @var $foo \\Doctrine\\DBAL\\Query\\QueryBuilder */\n" +
                    "$foo->" + s + "('', '<caret>');",
                "cms_users"
            );
        }
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.doctrine.querybuilder.dbal.DoctrineDbalQbGotoCompletionRegistrar
     */
    public void testNotDBALTableNameCompletion() {
        assertCompletionNotContains(PhpFileType.INSTANCE, "<?php" +
                "/** @var $foo \\Doctrine\\DBAL\\Query\\QueryBuilder */\n" +
                "$foo->delete('<caret>');",
            "cms_users"
        );
    }
}
