package fr.adrienbrault.idea.symfonyplugin.tests.form.intention;

import com.jetbrains.php.lang.PhpFileType;
import fr.adrienbrault.idea.symfonyplugin.tests.SymfonyLightCodeInsightFixtureTestCase;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 *
 * @see fr.adrienbrault.idea.symfonyplugin.form.intention.FormStringToClassConstantIntention
 */
public class FormStringToClassConstantIntentionTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void setUp() throws Exception {
        super.setUp();
        myFixture.configureFromExistingVirtualFile(myFixture.copyFileToProject("classes.php"));
    }

    protected String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfonyplugin/tests/form/intention/fixtures";
    }

    public void testIntentionIsAvailable() {
        assertIntentionIsAvailable(PhpFileType.INSTANCE,
            "<?php\n /** @var $foo \\Symfony\\Component\\Form\\FormBuilderInterface */\n $foo->add('', 'hid<caret>den')",
            "Symfony: use FormType class constant"
        );
    }
}
