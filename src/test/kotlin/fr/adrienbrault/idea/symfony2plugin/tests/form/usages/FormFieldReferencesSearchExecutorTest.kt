package fr.adrienbrault.idea.symfony2plugin.tests.form.usages

import com.intellij.find.findUsages.PsiElement2UsageTargetAdapter
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.LocalSearchScope
import com.intellij.psi.search.SearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.usages.UsageTarget
import com.jetbrains.php.lang.psi.elements.Field
import com.jetbrains.php.lang.psi.elements.PhpClass
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression
import fr.adrienbrault.idea.symfony2plugin.form.usages.FormFieldReferencesSearchExecutor
import fr.adrienbrault.idea.symfony2plugin.form.usages.FormFieldUsageReference
import fr.adrienbrault.idea.symfony2plugin.form.usages.FormFieldUsageTypeProvider
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase

abstract class FormFieldUsageTestCase : SymfonyLightCodeInsightFixtureTestCase() {
    override fun setUp() {
        super.setUp()

        myFixture.copyFileToProject("FormFieldReferencesSearchExecutorTest.php")
    }

    override fun getTestDataPath(): String =
        "src/test/kotlin/fr/adrienbrault/idea/symfony2plugin/tests/form/usages/fixtures"

    protected fun dataClass(members: String = "public ?string \$title = null;", name: String = "ProfileData"): PhpClass {
        val file = myFixture.addFileToProject("$name.php", "<?php namespace App; class $name { $members }")

        return PsiTreeUtil.findChildOfType(file, PhpClass::class.java)!!
    }

    protected fun form(body: String = "\$builder->add('title');", name: String = "ProfileType", data: String = "ProfileData", defaults: String = "\$resolver->setDefaults(['data_class' => $data::class]);", extra: String = ""): PsiFile =
        myFixture.addFileToProject("$name.php", """
            <?php namespace App;
            use Symfony\Component\Form\FormBuilderInterface;
            use Symfony\Component\OptionsResolver\OptionsResolver;
            class $name {
                public function buildForm(FormBuilderInterface ${'$'}builder, array ${'$'}options) { $body }
                public function configureOptions(OptionsResolver ${'$'}resolver) { $defaults }
                $extra
            }
        """.trimIndent())

    protected fun usages(target: PsiElement, scope: SearchScope = GlobalSearchScope.projectScope(project)): List<FormFieldUsageReference> =
        ReferencesSearch.search(target, scope).findAll().filterIsInstance<FormFieldUsageReference>()
}

class FormFieldReferencesSearchExecutorTest : FormFieldUsageTestCase() {
    fun testPublicPropertyFindsExactFormLiteralAndNativeNamesDoNotInterfere() {
        val clazz = dataClass("public \$title; public function getTitle() {} public function setTitle(\$value) {} public function isTitle() {} public function hasTitle() {}")
        val target = clazz.findFieldByName("title", false)!!
        form("\$builder->add('title'); \$builder->add('other');")

        val refs = usages(target)

        assertEquals(1, refs.size)
        assertEquals("title", refs.single().element.contents)
        assertEquals("title", refs.single().rangeInElement.substring(refs.single().element.text))
        assertEquals(target, refs.single().resolve())
        assertEquals(1, usages(clazz.findMethodByName("getTitle")!!).size)
        assertEquals(1, usages(clazz.findMethodByName("setTitle")!!).size)
        assertEmpty(usages(clazz.findMethodByName("isTitle")!!))
        assertEmpty(usages(clazz.findMethodByName("hasTitle")!!))
    }

    fun testPromotedPropertyAndCamelCaseNormalization() {
        val target = dataClass("public function __construct(public string \$fooBar) {}").findFieldByName("fooBar", false)!!
        form("\$builder->add('fooBar')->add('foo_bar'); \$builder->create('FOO_BAR');")

        assertEquals(setOf("fooBar", "foo_bar", "FOO_BAR"), usages(target).map { it.element.contents }.toSet())
    }

    fun testUnsupportedProperties() {
        val clazz = dataClass("private \$title; protected \$hidden; public static \$shared; public const LABEL = 'x';")
        form("\$builder->add('title'); \$builder->add('hidden'); \$builder->add('shared'); \$builder->add('LABEL');")

        clazz.fields.forEach { assertEmpty(usages(it)) }
    }

    fun testMultipleFormsAndDifferentDataClassAndSetDefault() {
        val target = dataClass().findFieldByName("title", false)!!
        dataClass(name = "OtherDTO")
        form()
        form(name = "SecondType", defaults = "\$resolver->setDefault('data_class', ProfileData::class);")
        form(name = "OtherType", data = "OtherDTO")

        assertEquals(setOf("ProfileType.php", "SecondType.php"), usages(target).map { it.element.containingFile.name }.toSet())
    }

    fun testAmbiguousDataClassDefaultsAreExcluded() {
        val target = dataClass().findFieldByName("title", false)!!
        dataClass(name = "OtherDTO")
        form(defaults = "\$resolver->setDefaults(['data_class' => ProfileData::class]); \$resolver->setDefault('data_class', OtherDTO::class);")

        assertEmpty(usages(target))
    }

    fun testDisabledSupportDoesNotProduceUsages() {
        val target = dataClass().findFieldByName("title", false)!!
        form()

        fr.adrienbrault.idea.symfony2plugin.Settings.getInstance(project).pluginEnabled = false

        assertEmpty(usages(target))
    }

    fun testLegacySetDefaultOptions() {
        val target = dataClass().findFieldByName("title", false)!!
        myFixture.addFileToProject("Legacy.php", """
            <?php namespace App;
            class Legacy {
                public function buildForm(\Symfony\Component\Form\FormBuilderInterface ${'$'}builder) { ${'$'}builder->add('title'); }
                public function setDefaultOptions(\Symfony\Component\OptionsResolver\OptionsResolverInterface ${'$'}resolver) {
                    ${'$'}resolver->setDefaults(['data_class' => ProfileData::class]);
                }
            }
        """.trimIndent())

        assertEquals(1, usages(target).size)
    }

    fun testDynamicAndUnmappedAndChildBuildersAreExcluded() {
        val target = dataClass().findFieldByName("title", false)!!
        form("""
            ${'$'}builder->add(${ '$' }name);
            ${'$'}builder->add("ti${'$'}suffix");
            ${'$'}builder->add('title', null, ['mapped' => false]);
            ${'$'}builder->add('title', null, ['property_path' => 'other']);
            ${'$'}builder->add('title', null, ['property_path' => ${'$'}path]);
            ${'$'}builder->add('title', null, ${'$'}options);
            ${'$'}builder->get('child')->add('title');
            ${'$'}builder->create('child')->add('title');
            ${'$'}child = ${'$'}builder->create('child');
            ${'$'}child->add('title');
            ${'$'}builder->add('title', null, ['mapped' => true, 'property_path' => 'title']);
        """.trimIndent())

        assertEquals(1, usages(target).size)
    }

    fun testHelperMethodsAndNoDuplicateLiterals() {
        val target = dataClass().findFieldByName("title", false)!!
        form("\$this->fields(\$builder); \$this->fields(\$builder);", extra = """
            private function fields(FormBuilderInterface ${'$'}root) { ${'$'}root->add('title'); }
        """.trimIndent())

        assertEquals(1, usages(target).size)
    }

    fun testUnrelatedHelperParameterAndMixedChildCallsAreExcluded() {
        val target = dataClass().findFieldByName("title", false)!!
        form("\$this->fields(\$builder, \$builder->get('child')); \$this->mixed(\$builder); \$this->mixed(\$builder->get('child'));", extra = """
            private function fields(FormBuilderInterface ${'$'}root, FormBuilderInterface ${'$'}child) { ${'$'}child->add('title'); }
            private function mixed(FormBuilderInterface ${'$'}root) { ${'$'}root->add('title'); }
        """.trimIndent())

        assertEmpty(usages(target))
    }

    fun testGlobalAndLocalScopes() {
        val target = dataClass().findFieldByName("title", false)!!
        val first = form()
        form(name = "SecondType")

        assertEquals(1, usages(target, GlobalSearchScope.fileScope(first)).size)

        val literal = PsiTreeUtil.findChildrenOfType(first, StringLiteralExpression::class.java).first { it.contents == "title" }

        assertEquals(1, usages(target, LocalSearchScope(literal)).size)
        assertEmpty(usages(target, LocalSearchScope(target)))
    }

    fun testCrossFileHelperScopeIsAppliedToLiteral() {
        val target = dataClass().findFieldByName("title", false)!!
        val helper = myFixture.addFileToProject("Helper.php", """
            <?php namespace App;
            class Helper {
                public static function fields(\Symfony\Component\Form\FormBuilderInterface ${'$'}root) { ${'$'}root->add('title'); }
            }
        """.trimIndent())
        val form = form("Helper::fields(\$builder);")

        assertEquals(1, usages(target, GlobalSearchScope.fileScope(helper)).size)
        assertEquals(1, usages(target, LocalSearchScope(helper)).size)
        assertEmpty(usages(target, GlobalSearchScope.fileScope(form)))
    }

    fun testConsumerStopsImmediately() {
        val target = dataClass().findFieldByName("title", false)!!
        form("\$builder->add('title'); \$builder->create('title');")

        var count = 0
        val completed = FormFieldReferencesSearchExecutor().execute(
            ReferencesSearch.SearchParameters(target, GlobalSearchScope.projectScope(project), false)
        ) { count++; false }

        assertFalse(completed)
        assertEquals(1, count)
    }

    fun testUsageTypeIsTargetAwareAndRenameReferenceIsInert() {
        val clazz = dataClass("public \$title; public function setTitle(\$value) {}")
        val target = clazz.findFieldByName("title", false)!!
        form()

        val reference = usages(target).single()
        val provider = FormFieldUsageTypeProvider()
        val targets = arrayOf<UsageTarget>(PsiElement2UsageTargetAdapter(target, true))

        assertEquals("Symfony form field", provider.getUsageType(reference.element, targets).toString())
        assertNull(provider.getUsageType(reference.element))
        assertEquals("Symfony form field", provider.getUsageType(reference.element, arrayOf(PsiElement2UsageTargetAdapter(clazz.findMethodByName("setTitle")!!, true))).toString())

        val unrelated = myFixture.addFileToProject("string.php", "<?php \$value = 'title';")

        assertNull(provider.getUsageType(PsiTreeUtil.findChildOfType(unrelated, StringLiteralExpression::class.java)!!, targets))
        assertEquals(reference.element, reference.handleElementRename("renamed"))
        assertEquals("title", reference.element.contents)
    }
}
