package fr.adrienbrault.idea.symfony2plugin.tests.form

import com.intellij.patterns.PlatformPatterns
import com.jetbrains.php.lang.PhpFileType
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.form.FormAttributeOptionGotoCompletionRegistrar
 */
class FormAttributeOptionGotoCompletionRegistrarTest : SymfonyLightCodeInsightFixtureTestCase() {
    override fun setUp() {
        super.setUp()
        myFixture.copyFileToProject("classes.php")
        myFixture.copyFileToProject("FormAttributeOptionGotoCompletionRegistrar.php")
    }

    override fun getTestDataPath() = "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/form/fixtures"

    fun testOptionCompletionAndNavigation() {
        assertCompletionContains(
            PhpFileType.INSTANCE, attributeCode("FormField(EmailType::class, options: ['<caret>' => true])"),
            "required", "email_option", "configure_options", "email_extension"
        )
        assertNavigationMatch(
            PhpFileType.INSTANCE, attributeCode("FormField(EmailType::class, options: ['required<caret>' => true])"),
            PlatformPatterns.psiElement(StringLiteralExpression::class.java).withText("'required'")
        )
        assertNavigationMatch(
            PhpFileType.INSTANCE, attributeCode("FormField(EmailType::class, options: ['configure_options<caret>' => true])"),
            PlatformPatterns.psiElement(StringLiteralExpression::class.java).withText("'configure_options'")
        )
        assertNavigationMatch(
            PhpFileType.INSTANCE, attributeCode("FormField(EmailType::class, options: ['email_extension<caret>' => true])"),
            PlatformPatterns.psiElement(StringLiteralExpression::class.java).withText("'email_extension'")
        )
    }

    fun testNamedAndPositionalArguments() {
        assertCompletionContains(
            PhpFileType.INSTANCE, attributeCode("FormField(options: ['<caret>' => true], name: 'email', type: EmailType::class)"),
            "email_option"
        )
        assertNavigationMatch(
            PhpFileType.INSTANCE, attributeCode("FormField(EmailType::class, ['email_option<caret>' => true])"),
            PlatformPatterns.psiElement(StringLiteralExpression::class.java).withText("'email_option'")
        )
    }

    fun testAliasedAndFullyQualifiedAttributes() {
        assertCompletionContains(
            PhpFileType.INSTANCE, attributeCode("Field(EmailType::class, options: ['<caret>'])"), "email_option"
        )
        assertNavigationMatch(
            PhpFileType.INSTANCE,
            attributeCode("\\Symfony\\Component\\Form\\Attribute\\FormField(\\Symfony\\Component\\Form\\Extension\\Core\\Type\\EmailType::class, options: ['email_option<caret>'])"),
            PlatformPatterns.psiElement(StringLiteralExpression::class.java).withText("'email_option'")
        )
    }

    fun testDefaultOptionsWithoutExplicitType() {
        assertCompletionContains(
            PhpFileType.INSTANCE, attributeCode("FormField(options: ['<caret>' => true])"), "configure_options"
        )
        assertCompletionNotContains(
            PhpFileType.INSTANCE, attributeCode("FormField(options: ['<caret>' => true])"), "email_option", "email_extension"
        )
        assertNavigationMatch(
            PhpFileType.INSTANCE, attributeCode("FormField(null, ['configure_options<caret>' => true])"),
            PlatformPatterns.psiElement(StringLiteralExpression::class.java).withText("'configure_options'")
        )
    }

    fun testOptionsAreOnlyAvailableForTopLevelKeys() {
        assertCompletionNotContains(
            PhpFileType.INSTANCE, attributeCode("FormField(EmailType::class, options: ['attr' => ['<caret>']])"),
            "email_option", "configure_options", "email_extension"
        )
        assertNavigationIsEmpty(
            PhpFileType.INSTANCE, attributeCode("FormField(EmailType::class, options: ['label' => 'email_option<caret>'])")
        )
        assertCompletionNotContains(
            PhpFileType.INSTANCE, attributeCode("OtherAttribute(EmailType::class, options: ['<caret>'])"), "email_option"
        )
        assertNavigationIsEmpty(
            PhpFileType.INSTANCE, attributeCode("FormField(EmailType::class, name: ['email_option<caret>'])")
        )
    }

    private fun attributeCode(attribute: String) = """
        <?php
        use Symfony\Component\Form\Attribute\FormField;
        use Symfony\Component\Form\Attribute\FormField as Field;
        use Symfony\Component\Form\Extension\Core\Type\EmailType;

        class AccountData {
            #[$attribute]
            public ?string ${'$'}email = null;
        }
    """.trimIndent()
}
