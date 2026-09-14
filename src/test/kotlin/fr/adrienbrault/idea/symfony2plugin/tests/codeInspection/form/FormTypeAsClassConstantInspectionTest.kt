package fr.adrienbrault.idea.symfony2plugin.tests.codeInspection.form

import com.jetbrains.php.lang.PhpFileType
import fr.adrienbrault.idea.symfony2plugin.codeInspection.form.FormTypeAsClassConstantInspection
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.codeInspection.form.FormTypeAsClassConstantInspection
 */
class FormTypeAsClassConstantInspectionTest : SymfonyLightCodeInsightFixtureTestCase() {
    override fun setUp() {
        super.setUp()
        myFixture.configureFromExistingVirtualFile(myFixture.copyFileToProject("form.php"))
    }

    override fun getTestDataPath(): String {
        return "src/test/kotlin/fr/adrienbrault/idea/symfony2plugin/tests/codeInspection/form/fixtures"
    }

    fun testThatFormNamesAreInspectedAsDeprecated() {
        initVersion()

        assertLocalInspectionContains(FormTypeAsClassConstantInspection::class.java, "my_form.php", "<?php\n" +
                "/** @var \$builder \\Symfony\\Component\\Form\\FormBuilderInterface */\n" +
                "\$builder->add(null, 'fo<caret>o')",
            FormTypeAsClassConstantInspection.MESSAGE
        )

        assertLocalInspectionContains(FormTypeAsClassConstantInspection::class.java, "my_form.php", "<?php\n" +
                "/** @var \$builder \\Symfony\\Component\\Form\\FormBuilderInterface */\n" +
                "\$builder->create(null, 'fo<caret>o')",
            FormTypeAsClassConstantInspection.MESSAGE
        )
    }

    fun testThatFormNamesAreNotDeprecatedForWrongSymfonyVersion() {
        initVersion("2.5")

        assertLocalInspectionNotContains(FormTypeAsClassConstantInspection::class.java, "my_form.php", "<?php\n" +
                "/** @var \$builder \\Symfony\\Component\\Form\\FormBuilderInterface */\n" +
                "\$builder->add(null, 'fo<caret>o')",
            FormTypeAsClassConstantInspection.MESSAGE
        )
    }

    fun testThatFormNamesWithFqnAsStringNotDeprecated() {
        initVersion()

        assertLocalInspectionNotContains(FormTypeAsClassConstantInspection::class.java, "my_form.php", "<?php\n" +
                "/** @var \$builder \\Symfony\\Component\\Form\\FormBuilderInterface */\n" +
                "\$builder->add(null, 'Foo\\B<caret>ar')",
            FormTypeAsClassConstantInspection.MESSAGE
        )
    }

    private fun initVersion() {
        initVersion("2.8")
    }

    private fun initVersion(version: String) {
        myFixture.configureByText(PhpFileType.INSTANCE, "<?php\n" +
            "namespace Symfony\\Component\\HttpKernel {\n" +
            "   class Kernel {\n" +
            "       const VERSION = '$version';" +
            "   }" +
            "}"
        )
    }
}
