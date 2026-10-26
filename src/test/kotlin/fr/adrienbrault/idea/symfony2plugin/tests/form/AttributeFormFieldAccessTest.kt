package fr.adrienbrault.idea.symfony2plugin.tests.form

import com.intellij.patterns.PlatformPatterns
import com.intellij.find.findUsages.PsiElement2UsageTargetAdapter
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.LocalSearchScope
import com.intellij.psi.search.SearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.refactoring.rename.RenameProcessor
import com.jetbrains.php.lang.PhpFileType
import com.jetbrains.php.lang.psi.elements.Field
import com.jetbrains.php.lang.psi.elements.MethodReference
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression
import fr.adrienbrault.idea.symfony2plugin.form.util.FormFieldNameReference
import fr.adrienbrault.idea.symfony2plugin.form.usages.FormFieldUsageTypeProvider
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil

class AttributeFormFieldAccessTest : SymfonyLightCodeInsightFixtureTestCase() {
    override fun getTestDataPath(): String =
        "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/templating/fixtures"

    override fun setUp() {
        super.setUp()
        myFixture.copyFileToProject("AttributeFormFields.php")
    }

    fun testGetAndHasCompletionIncludesAliasesAndInheritedFields() {
        assertCompletionContains(PhpFileType.INSTANCE, controller("\$form->get('<caret>');"), "email", "displayName", "identifier")
        assertCompletionResultsNotContain("internalName", "unannotated")

        assertCompletionContains(PhpFileType.INSTANCE, controller("\$form->has(name: '<caret>');"), "email", "displayName", "identifier")
    }

    fun testGetAndHasNavigateToDeclaringProperty() {
        assertReferenceMatchOnParent(
            PhpFileType.INSTANCE, controller("\$form->get('display<caret>Name');"),
            PlatformPatterns.psiElement(Field::class.java).withName("internalName")
        )
        assertReferenceMatchOnParent(
            PhpFileType.INSTANCE, controller("\$form->has(name: 'identi<caret>fier');"),
            PlatformPatterns.psiElement(Field::class.java).withName("identifier")
        )
    }

    fun testFactoryCallsSupportNamedArgumentsAndDirectAccess() {
        assertCompletionContains(PhpFileType.INSTANCE, """
            <?php
            function fields(\Symfony\Component\Form\FormFactoryInterface ${'$'}factory) {
                ${'$'}form = ${'$'}factory->createNamed(type: \App\Form\AccountData::class, name: 'account');
                ${'$'}form->get('<caret>');
            }
        """.trimIndent(), "email", "displayName")

        assertReferenceMatchOnParent(PhpFileType.INSTANCE, """
            <?php
            function fields(\Symfony\Component\Form\FormFactoryInterface ${'$'}factory) {
                ${'$'}factory->create(data: null, type: \App\Form\AccountData::class)->has('display<caret>Name');
            }
        """.trimIndent(), PlatformPatterns.psiElement(Field::class.java).withName("internalName"))
    }

    fun testFindUsagesIncludesPhpAccessesOnceAndRespectsScope() {
        val calls = myFixture.addFileToProject("calls.php", controller("""
            ${'$'}form->get('displayName');
            ${'$'}form->has(name: 'displayName');
            ${'$'}form->get('identifier');
            ${'$'}form->get('internalName');
        """.trimIndent()))
        myFixture.addFileToProject("unrelated.php", """
            <?php
            function unrelated(\Symfony\Component\Form\FormInterface ${'$'}form) {
                ${'$'}form->get('displayName');
            }
        """.trimIndent())
        val target = field("internalName")
        val references = usages(target)

        assertEquals(2, references.size)
        assertEquals(calls, references.first().element.containingFile)
        val usageType = FormFieldUsageTypeProvider().getUsageType(references.first().element, arrayOf(PsiElement2UsageTargetAdapter(target, true)))
        assertEquals("Symfony form field", usageType.toString())
        assertEquals(2, usages(target, GlobalSearchScope.fileScope(calls)).size)
        assertEmpty(usages(target, LocalSearchScope(target)))
        assertEquals(field("identifier"), usages(field("identifier")).single().resolve())
    }

    fun testPropertyRenamePreservesExplicitFormName() {
        val code = controller("\$form->get('displayName'); \$form->has('displayName');")
        val calls = myFixture.addFileToProject("calls.php", code)
        val target = field("internalName")
        assertEquals(2, usages(target).size)

        RenameProcessor(project, target, "updatedName", false, false).run()

        assertEquals(code, calls.text)
    }

    fun testClassicFormFieldsStillCompleteAndNavigate() {
        myFixture.addFileToProject("LegacyType.php", """
            <?php
            namespace Symfony\Component\Form {
                interface FormBuilderInterface { public function add(${'$'}name); }
            }
            namespace App\Form {
                class LegacyType implements \Symfony\Component\Form\FormTypeInterface {
                    public function buildForm(\Symfony\Component\Form\FormBuilderInterface ${'$'}builder, array ${'$'}options) {
                        ${'$'}builder->add('legacy');
                    }
                }
            }
        """.trimIndent())

        assertCompletionContains(PhpFileType.INSTANCE, controller("\$form->get('<caret>');", "LegacyType"), "legacy")
        myFixture.configureByText(PhpFileType.INSTANCE, controller("\$form->get('lega<caret>cy');", "LegacyType"))
        val literal = PsiTreeUtil.getParentOfType(myFixture.file.findElementAt(myFixture.caretOffset), StringLiteralExpression::class.java)!!
        val target = literal.references.filterIsInstance<FormFieldNameReference>().single().resolve()
        assertInstanceOf(target, MethodReference::class.java)
        assertEquals("add", (target as MethodReference).name)
    }

    private fun controller(body: String, type: String = "AccountData"): String = """
        <?php
        namespace App\Controller;
        class FieldAccessController extends \Symfony\Bundle\FrameworkBundle\Controller\AbstractController {
            public function index() {
                ${'$'}form = ${'$'}this->createForm(\App\Form\$type::class);
                $body
            }
        }
    """.trimIndent()

    private fun field(name: String): Field =
        PhpElementsUtil.getClass(project, "\\App\\Form\\AccountData")!!.findFieldByName(name, false)!!

    private fun usages(target: PsiElement, scope: SearchScope = GlobalSearchScope.projectScope(project)): List<FormFieldNameReference> =
        ReferencesSearch.search(target, scope).findAll().filterIsInstance<FormFieldNameReference>()
}
