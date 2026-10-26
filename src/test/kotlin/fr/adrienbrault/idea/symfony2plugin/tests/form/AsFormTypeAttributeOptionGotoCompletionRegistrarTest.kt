package fr.adrienbrault.idea.symfony2plugin.tests.form

import com.intellij.patterns.PlatformPatterns
import com.jetbrains.php.lang.PhpFileType
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.form.AsFormTypeAttributeOptionGotoCompletionRegistrar
 */
class AsFormTypeAttributeOptionGotoCompletionRegistrarTest : SymfonyLightCodeInsightFixtureTestCase() {
    override fun setUp() {
        super.setUp()
        myFixture.copyFileToProject("AsFormTypeAttributeOptionGotoCompletionRegistrar.php")
    }

    override fun getTestDataPath() = "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/form/fixtures"

    fun testOptionCompletionAndNavigation() {
        assertCompletionContains(
            PhpFileType.INSTANCE, attributeCode("AsFormType(options: ['<caret>' => 'Account'])"),
            "label", "required", "core_extension", "account_extension"
        )
        assertNavigationMatch(
            PhpFileType.INSTANCE, attributeCode("AsFormType(options: ['label<caret>' => 'Account'])"),
            PlatformPatterns.psiElement(StringLiteralExpression::class.java).withText("'label'")
        )
        assertNavigationMatch(
            PhpFileType.INSTANCE, attributeCode("AsFormType(options: ['core_extension<caret>' => true])"),
            PlatformPatterns.psiElement(StringLiteralExpression::class.java).withText("'core_extension'")
        )
        assertNavigationMatch(
            PhpFileType.INSTANCE, attributeCode("AsFormType(options: ['account_extension<caret>' => true])"),
            PlatformPatterns.psiElement(StringLiteralExpression::class.java).withText("'account_extension'")
        )
    }

    fun testArgumentAndAttributeForms() {
        assertCompletionContains(
            PhpFileType.INSTANCE, attributeCode("AsFormType(['<caret>'])"), "label", "account_extension"
        )
        assertNavigationMatch(
            PhpFileType.INSTANCE, attributeCode("Form(options: ['label<caret>' => 'Account'])"),
            PlatformPatterns.psiElement(StringLiteralExpression::class.java).withText("'label'")
        )
        assertCompletionContains(
            PhpFileType.INSTANCE,
            attributeCode("\\Symfony\\Component\\Form\\Attribute\\AsFormType(options: ['<caret>' => 'Account'])"), "label"
        )
    }

    fun testInheritedAndOwnDefaultsAndExtensions() {
        assertCompletionContains(
            PhpFileType.INSTANCE,
            attributeCode("AsFormType(options: ['own_option' => true, '<caret>' => null])", "extends IntermediateAccountData"),
            "own_option", "ancestor_option", "parent_extension", "account_extension", "core_extension"
        )
        assertNavigationMatch(
            PhpFileType.INSTANCE, attributeCode("AsFormType(options: ['ancestor_option<caret>' => true])", "extends IntermediateAccountData"),
            PlatformPatterns.psiElement(StringLiteralExpression::class.java).withText("'ancestor_option'")
        )
        assertNavigationMatch(
            PhpFileType.INSTANCE, attributeCode("AsFormType(options: ['parent_extension<caret>' => true])", "extends IntermediateAccountData"),
            PlatformPatterns.psiElement(StringLiteralExpression::class.java).withText("'parent_extension'")
        )
        assertCompletionNotContains(
            PhpFileType.INSTANCE, attributeCode("AsFormType(options: ['<caret>' => true])", "extends IntermediateAccountData"),
            "unused_method_option", "unattributed_extension"
        )
    }

    fun testDataClassCannotBeConfigured() {
        assertCompletionNotContains(
            PhpFileType.INSTANCE, attributeCode("AsFormType(options: ['<caret>' => null])"), "data_class"
        )
        assertNavigationIsEmpty(
            PhpFileType.INSTANCE, attributeCode("AsFormType(options: ['data_class<caret>' => null])")
        )
    }

    fun testOptionsAreOnlyAvailableForTopLevelKeys() {
        assertCompletionNotContains(
            PhpFileType.INSTANCE, attributeCode("AsFormType(options: ['attr' => ['<caret>']])"), "label", "account_extension"
        )
        assertNavigationIsEmpty(
            PhpFileType.INSTANCE, attributeCode("AsFormType(options: ['label' => 'account_extension<caret>'])")
        )
        assertCompletionNotContains(
            PhpFileType.INSTANCE, attributeCode("AsFormType(settings: ['<caret>'])"), "label", "account_extension"
        )
        assertCompletionNotContains(
            PhpFileType.INSTANCE,
            attributeCode("\\Symfony\\Component\\Form\\Attribute\\FormField(options: ['<caret>'])"), "account_extension"
        )
    }

    fun testAttributeMustBelongToAClass() {
        assertCompletionNotContains(
            PhpFileType.INSTANCE,
            """
                <?php
                use Symfony\Component\Form\Attribute\AsFormType;
                class AccountData {
                    #[AsFormType(options: ['<caret>'])]
                    public string ${'$'}name;
                }
            """.trimIndent(),
            "label", "core_extension"
        )
    }

    private fun attributeCode(attribute: String, parent: String = "") = """
        <?php
        namespace App;
        use Symfony\Component\Form\Attribute\AsFormType;
        use Symfony\Component\Form\Attribute\AsFormType as Form;

        #[$attribute]
        class AccountData $parent {}
    """.trimIndent()
}
