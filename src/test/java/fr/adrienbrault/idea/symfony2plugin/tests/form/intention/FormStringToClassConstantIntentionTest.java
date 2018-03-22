package fr.adrienbrault.idea.symfony2plugin.tests.form.intention;

import com.jetbrains.php.lang.PhpFileType;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

import java.io.File;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 *
 * @see fr.adrienbrault.idea.symfony2plugin.form.intention.FormStringToClassConstantIntention
 */
public class FormStringToClassConstantIntentionTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void setUp() throws Exception {
        super.setUp();
        myFixture.configureFromExistingVirtualFile(myFixture.copyFileToProject("classes.php"));
    }

    protected String getTestDataPath() {
        return new File(this.getClass().getResource("fixtures").getFile()).getAbsolutePath();
    }

    public void testIntentionIsAvailable() {
        assertIntentionIsAvailable(PhpFileType.INSTANCE,
            "<?php\n /** @var $foo \\Symfony\\Component\\Form\\FormBuilderInterface */\n $foo->add('', 'hid<caret>den')",
            "Symfony: use FormType class constant"
        );
    }
}
