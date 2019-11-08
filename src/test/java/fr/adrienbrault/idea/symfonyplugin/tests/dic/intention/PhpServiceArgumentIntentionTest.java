package fr.adrienbrault.idea.symfonyplugin.tests.dic.intention;

import com.jetbrains.php.lang.PhpFileType;
import fr.adrienbrault.idea.symfonyplugin.tests.SymfonyLightCodeInsightFixtureTestCase;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfonyplugin.dic.intention.PhpServiceArgumentIntention
 */
public class PhpServiceArgumentIntentionTest extends SymfonyLightCodeInsightFixtureTestCase {
    public void setUp() throws Exception {
        super.setUp();
        myFixture.copyFileToProject("services.yml");
    }

    public String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfonyplugin/tests/dic/intention/fixtures";
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
