package fr.adrienbrault.idea.symfony2plugin.tests.form.action.generator

import com.jetbrains.php.lang.PhpFileType
import fr.adrienbrault.idea.symfony2plugin.form.action.generator.FormBuilderFieldGeneratorAction
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see FormBuilderFieldGeneratorAction
 */
class FormBuilderFieldGeneratorActionTest : SymfonyLightCodeInsightFixtureTestCase() {
    override fun setUp() {
        super.setUp()
        myFixture.addFileToProject(
            "Form.php",
            """
            <?php
            namespace Symfony\Component\Form;
            interface FormBuilderInterface {}
            interface FormTypeInterface {
                public function buildForm(FormBuilderInterface ${'$'}builder, array ${'$'}options);
            }
            """.trimIndent()
        )
    }

    fun testActionAvailableInsideBuildFormMethod() {
        myFixture.configureByText(
            PhpFileType.INSTANCE,
            """
            <?php
            class ProductType implements \Symfony\Component\Form\FormTypeInterface {
                public function buildForm(\Symfony\Component\Form\FormBuilderInterface ${'$'}builder, array ${'$'}options) {
                    <caret>
                }
            }
            """.trimIndent()
        )

        assertTrue(myFixture.testAction(FormBuilderFieldGeneratorAction()).isEnabledAndVisible)
    }
}
