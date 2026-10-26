package fr.adrienbrault.idea.symfony2plugin.tests.templating

import com.intellij.patterns.PlatformPatterns
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import com.jetbrains.php.lang.psi.elements.Field
import fr.adrienbrault.idea.symfony2plugin.templating.usages.TwigMethodReferencesSearchExecutor.TwigMethodUsageReference
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil

class NestedFormFieldTest : SymfonyLightCodeInsightFixtureTestCase() {
    override fun getTestDataPath(): String =
        "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/templating/fixtures"

    override fun setUp() {
        super.setUp()
        myFixture.copyFileToProject("ide-twig.json")
        myFixture.copyFileToProject("AttributeFormFields.php")
        myFixture.copyFileToProject("NestedFormFields.php")
    }

    fun testNestedFieldsCompleteAndNavigate() {
        assertContainsElements(complete("{{ form.address.<caret> }}"), "street", "postalCode")
        configure("{{ form.address.postal<caret>Code }}")
        assertNavigationMatch(PlatformPatterns.psiElement(Field::class.java).withName("zip"))
    }

    fun testCollectionEntriesRetainTheirFormTypeInLoops() {
        assertContainsElements(complete("{% for address in form.addresses %}{{ address.<caret> }}{% endfor %}"), "street", "postalCode")
        configure("{% for address in form.addresses.children %}{{ address.str<caret>eet }}{% endfor %}")
        assertNavigationMatch(PlatformPatterns.psiElement(Field::class.java).withName("street"))
    }

    fun testCollectionPrototypeResolvesToEntryFields() {
        assertContainsElements(complete("{{ form.addresses.vars.prototype.<caret> }}"), "street", "postalCode")
        configure("{{ form.addresses.vars.prototype.postal<caret>Code }}")
        assertNavigationMatch(PlatformPatterns.psiElement(Field::class.java).withName("zip"))
    }

    fun testDisabledPrototypeIsNotSuggested() {
        assertFalse(complete("{{ form.fixedAddresses.vars.<caret> }}").contains("prototype"))
    }

    fun testRepeatedFieldsUseConfiguredNamesAndType() {
        assertContainsElements(complete("{{ form.repeated.<caret> }}"), "original", "confirmation")
        assertContainsElements(complete("{{ form.repeated.original.<caret> }}"), "street", "postalCode")
        assertContainsElements(complete("{{ form.password.<caret> }}"), "first", "second")
    }

    fun testNestedCollectionsPreserveEntryOptions() {
        assertContainsElements(complete("{% for group in form.groups %}{% for address in group %}{{ address.<caret> }}{% endfor %}{% endfor %}"), "street", "postalCode")
        assertContainsElements(complete("{{ form.groups.vars.prototype.vars.prototype.<caret> }}"), "street", "postalCode")
    }

    fun testClassicBuilderFieldsUseTheSameNestedMetadata() {
        assertContainsElements(complete("{{ form.address.<caret> }}", "form/nested_classic.html.twig"), "street", "postalCode")
        assertContainsElements(complete("{{ form.addresses.vars.prototype.<caret> }}", "form/nested_classic.html.twig"), "street", "postalCode")
    }

    fun testNestedFieldsAreIncludedInFindUsages() {
        configure("{{ form.address.street }}")
        val target = PhpElementsUtil.getClass(project, "\\App\\Form\\AddressData")!!.findFieldByName("street", false)!!
        val references = ReferencesSearch.search(target, GlobalSearchScope.projectScope(project)).findAll().filterIsInstance<TwigMethodUsageReference>()

        assertEquals(myFixture.file, references.single().element.containingFile)
    }

    private fun complete(code: String, template: String = "form/nested.html.twig"): List<String> {
        configure(code, template)
        myFixture.completeBasic()
        return myFixture.lookupElementStrings.orEmpty()
    }

    private fun configure(code: String, template: String = "form/nested.html.twig") {
        val existing = myFixture.tempDirFixture.getFile("templates/$template")
        if (existing == null) {
            myFixture.addFileToProject("templates/$template", code)
        } else {
            WriteCommandAction.runWriteCommandAction(project) { VfsUtil.saveText(existing, code) }
        }
        myFixture.configureFromTempProjectFile("templates/$template")
    }
}
