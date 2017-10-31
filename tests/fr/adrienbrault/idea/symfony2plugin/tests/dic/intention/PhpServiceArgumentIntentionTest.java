package fr.adrienbrault.idea.symfony2plugin.tests.dic.intention;

import com.jetbrains.php.lang.PhpFileType;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

import java.io.File;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.dic.intention.PhpServiceArgumentIntention
 */
public class PhpServiceArgumentIntentionTest extends SymfonyLightCodeInsightFixtureTestCase {
    public void setUp() throws Exception {
        super.setUp();
        myFixture.copyFileToProject("services.yml");
    }

    public String getTestDataPath() {
        return new File(this.getClass().getResource("fixtures").getFile()).getAbsolutePath();
    }

    public void testIntentionIsAvailable() {
        assertIntentionIsAvailable(
            PhpFileType.INSTANCE,
            "<?php\n" +
                "" +
                "namespace Foo;\n" +
                "" +
                "class Foobar\n" +
                "{\n" +
                "<caret>" +
                "}\n",
            "Symfony: Update service arguments"
        );
    }
}
