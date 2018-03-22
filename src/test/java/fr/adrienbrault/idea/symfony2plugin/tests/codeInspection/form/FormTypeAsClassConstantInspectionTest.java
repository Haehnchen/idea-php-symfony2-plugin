package fr.adrienbrault.idea.symfony2plugin.tests.codeInspection.form;

import com.jetbrains.php.lang.PhpFileType;
import fr.adrienbrault.idea.symfony2plugin.codeInspection.form.FormTypeAsClassConstantInspection;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import org.jetbrains.annotations.NotNull;

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
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/codeInspection/form/fixtures";
    }

    public void testThatFormNamesAreInspectedAsDeprecated() {
        this.initVersion();

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

    public void testThatFormNamesAreNotDeprecatedForWrongSymfonyVersion() {
        this.initVersion("2.5");

        assertLocalInspectionNotContains("my_form.php", "<?php\n" +
                "/** @var $builder \\Symfony\\Component\\Form\\FormBuilderInterface */\n" +
                "$builder->add(null, 'fo<caret>o')",
            FormTypeAsClassConstantInspection.MESSAGE
        );
    }

    public void testThatFormNamesWithFqnAsStringNotDeprecated() {
        this.initVersion();

        assertLocalInspectionNotContains("my_form.php", "<?php\n" +
                "/** @var $builder \\Symfony\\Component\\Form\\FormBuilderInterface */\n" +
                "$builder->add(null, 'Foo\\B<caret>ar')",
            FormTypeAsClassConstantInspection.MESSAGE
        );
    }

    private void initVersion() {
        initVersion("2.8");
    }

    private void initVersion(@NotNull String version) {
        myFixture.configureByText(PhpFileType.INSTANCE, "<?php\n" +
            "namespace Symfony\\Component\\HttpKernel {\n" +
            "   class Kernel {\n" +
            "       const VERSION = '" + version + "';" +
            "   }" +
            "}"
        );
    }
}
