package fr.adrienbrault.idea.symfony2plugin.tests.codeInspection.form;

import fr.adrienbrault.idea.symfony2plugin.codeInspection.form.FormTypeAsClassConstantInspection;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

import java.io.File;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.codeInspection.form.FormTypeAsClassConstantInspection
 */
public class FormTypeAsClassConstantInspectionTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void setUp() throws Exception {
        super.setUp();
        myFixture.configureFromExistingVirtualFile(myFixture.copyFileToProject("form.php"));
    }

    public String getTestDataPath() {
        return new File(this.getClass().getResource("fixtures").getFile()).getAbsolutePath();
    }

    public void testThatFormNamesAreInspectedAsDeprecated() {
        assertLocalInspectionContains("my_form.php", "<?php\n" +
                "/** @var $builder \\Symfony\\Component\\Form\\FormBuilderInterface */\n" +
                "$builder->add(null, 'fo<caret>o')",
            FormTypeAsClassConstantInspection.MESSAGE
        );

        assertLocalInspectionContains("my_form.php", "<?php\n" +
                "/** @var $builder \\Symfony\\Component\\Form\\FormBuilderInterface */\n" +
                "$builder->create(null, 'fo<caret>o')",
            FormTypeAsClassConstantInspection.MESSAGE
        );
    }

    public void testThatFormNamesWithFqnAsStringNotDeprecated() {
        assertLocalInspectionNotContains("my_form.php", "<?php\n" +
                "/** @var $builder \\Symfony\\Component\\Form\\FormBuilderInterface */\n" +
                "$builder->add(null, 'Foo\\B<caret>ar')",
            FormTypeAsClassConstantInspection.MESSAGE
        );
    }
}
