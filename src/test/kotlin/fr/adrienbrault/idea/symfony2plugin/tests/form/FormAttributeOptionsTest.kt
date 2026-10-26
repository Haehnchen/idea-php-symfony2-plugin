package fr.adrienbrault.idea.symfony2plugin.tests.form

import com.intellij.patterns.PlatformPatterns
import com.jetbrains.php.lang.PhpFileType
import com.jetbrains.php.lang.psi.elements.Field
import com.jetbrains.php.lang.psi.elements.PhpClass
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression
import fr.adrienbrault.idea.symfony2plugin.form.PhpLineMarkerProvider
import fr.adrienbrault.idea.symfony2plugin.form.util.FormOptionsUtil
import fr.adrienbrault.idea.symfony2plugin.mcp.collector.SymfonyFormTypeOptionsCollector
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.FormDataClassStubIndex
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
class FormAttributeOptionsTest : SymfonyLightCodeInsightFixtureTestCase() {
    override fun setUp() {
        super.setUp()
        myFixture.copyFileToProject("AsFormTypeAttributeOptionGotoCompletionRegistrar.php")
        myFixture.copyFileToProject("AttributeFormOptions.php")
        myFixture.copyFileToProject("foo.de.yml", "Resources/translations/foo.de.yml")
    }

    override fun getTestDataPath() = "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/form/fixtures"

    fun testImplicitDataClass() {
        val dataClass = PhpElementsUtil.getClass(project, "App\\AccountData")!!
        assertEquals(dataClass, FormOptionsUtil.getFormPhpClassFromContext(dataClass.findMethodByName("configureOptions")!!))
        assertIndexContainsKeyWithValue(FormDataClassStubIndex.KEY, "\\App\\AccountData") { it.contains("\\App\\AccountData") }
        assertIndexNotContains(FormDataClassStubIndex.KEY, "\\App\\OtherData")
        val markers = mutableListOf<com.intellij.codeInsight.daemon.LineMarkerInfo<*>>()
        PhpLineMarkerProvider().collectSlowLineMarkers(listOf(dataClass.nameIdentifier!!), markers)
        assertEmpty(markers)
    }

    fun testOptionsAtControllerCall() {
        val code = """
            <?php
            class Controller extends \Symfony\Bundle\FrameworkBundle\Controller\AbstractController {
                public function index() {
                    ${'$'}this->createForm(\App\AccountData::class, null, ['<caret>' => null]);
                }
            }
        """.trimIndent()
        assertCompletionContains(PhpFileType.INSTANCE, code, "own_option", "ancestor_option", "label", "account_extension")
        assertCompletionNotContains(PhpFileType.INSTANCE, code, "unused_dto_option", "unused_method_option", "unattributed_extension")
        assertNavigationMatch(
            PhpFileType.INSTANCE, code.replace("<caret>", "ancestor_option<caret>"),
            PlatformPatterns.psiElement(StringLiteralExpression::class.java).withText("'ancestor_option'")
        )
    }

    fun testMcpUsesAttributeDefaultsAndParents() {
        val csv = SymfonyFormTypeOptionsCollector(project).collect("App\\AccountData")
        assertTrue(csv, csv.contains("own_option,DEFAULT,\\App\\AccountData"))
        assertTrue(csv, csv.contains("ancestor_option,DEFAULT,\\App\\ParentAccountData"))
        assertTrue(csv, csv.contains("parent_extension,DEFAULT,\\App\\ParentExtension"))
        assertTrue(csv, csv.contains("label,DEFAULT,\\Symfony\\Component\\Form\\Extension\\Core\\Type\\FormType"))
        assertFalse(csv, csv.contains("unused_dto_option"))
    }

    fun testPropertyPathCompletionAndNavigation() {
        assertCompletionContains(
            PhpFileType.INSTANCE, fieldCode("'property_path' => '<caret>'"), "email", "displayName"
        )
        assertReferenceMatchOnParent(
            PhpFileType.INSTANCE, fieldCode("'property_path' => 'em<caret>ail'"),
            PlatformPatterns.psiElement(Field::class.java).withName("email")
        )
    }

    fun testFormTypeOptionValue() {
        assertCompletionContains(PhpFileType.INSTANCE, fieldCode("'entry_type' => '<caret>'"), "App\\AccountData")
        assertReferenceMatchOnParent(
            PhpFileType.INSTANCE, fieldCode("'entry_type' => 'App\\AccountData<caret>'"),
            PlatformPatterns.psiElement(PhpClass::class.java).withName("AccountData")
        )
    }

    fun testEntityClassOptionValue() {
        assertReferenceMatchOnParent(
            PhpFileType.INSTANCE, fieldCode("'class' => 'App\\OtherData<caret>'"),
            PlatformPatterns.psiElement(PhpClass::class.java).withName("OtherData")
        )
    }

    fun testTranslationOptions() {
        assertCompletionContains(PhpFileType.INSTANCE, fieldCode("'translation_domain' => '<caret>'"), "foo")
        assertCompletionContains(PhpFileType.INSTANCE, fieldCode("'label' => '<caret>'"), "foo.symfony.great")
        assertNavigationMatch(
            PhpFileType.INSTANCE, fieldCode("'help' => 'foo.symfony.great<caret>'")
        )
        assertCompletionContains(
            PhpFileType.INSTANCE, fieldCode("'choice_translation_domain' => 'foo', 'choices' => ['<caret>' => 'value']"),
            "foo.symfony.great"
        )
    }

    private fun fieldCode(options: String) = """
        <?php
        use Symfony\Component\Form\Attribute\AsFormType;
        use Symfony\Component\Form\Attribute\FormField;

        #[AsFormType]
        class EditData extends \App\AccountData {
            #[FormField(options: [$options])]
            public string ${'$'}display;
        }
    """.trimIndent()
}
