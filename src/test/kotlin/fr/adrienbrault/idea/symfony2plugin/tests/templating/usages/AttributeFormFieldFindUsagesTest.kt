package fr.adrienbrault.idea.symfony2plugin.tests.templating.usages

import com.intellij.find.FindManager
import com.intellij.find.impl.FindManagerImpl
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.LocalSearchScope
import com.intellij.psi.search.SearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.refactoring.rename.RenameProcessor
import com.intellij.usageView.UsageInfo
import com.jetbrains.php.lang.psi.elements.Field
import fr.adrienbrault.idea.symfony2plugin.templating.usages.TwigMethodReferencesSearchExecutor.TwigMethodUsageReference
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil

class AttributeFormFieldFindUsagesTest : SymfonyLightCodeInsightFixtureTestCase() {
    override fun getTestDataPath(): String =
        "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/templating/fixtures"

    override fun setUp() {
        super.setUp()
        myFixture.copyFileToProject("ide-twig.json")
        myFixture.copyFileToProject("AttributeFormFields.php")
    }

    fun testFindsPublicAndInheritedFormFieldsWithoutBuildForm() {
        val template = myFixture.addFileToProject("templates/form/attributes.html.twig", "{{ form.email }} {{ form.identifier }}")
        myFixture.addFileToProject("templates/unrelated.html.twig", "{{ form.email }} {{ form.identifier }}")

        assertEquals(template, usages(field("email")).single().element.containingFile)
        assertEquals(template, usages(field("identifier")).single().element.containingFile)
    }

    fun testPrivateAliasWorksFromPhpAndTwig() {
        val template = myFixture.addFileToProject("templates/form/attributes.html.twig", "{{ form.displayName }} {{ form.internalName }}")
        myFixture.addFileToProject("templates/unrelated.html.twig", "{{ form.displayName }}")
        val target = field("internalName")
        val manager = FindManager.getInstance(project) as FindManagerImpl
        val handler = manager.findUsagesManager.getFindUsagesHandler(target, false)!!
        val options = handler.findUsagesOptions
        options.searchScope = GlobalSearchScope.projectScope(project)
        options.isSearchForTextOccurrences = false
        options.isUsages = true
        val found = mutableListOf<UsageInfo>()

        assertTrue(handler.processElementUsages(target, { found.add(it) }, options))
        assertEquals(template.virtualFile, found.single().virtualFile)
        assertEquals("displayName", found.single().element!!.text)

        myFixture.configureFromExistingVirtualFile(template.virtualFile)
        myFixture.editor.caretModel.moveToOffset("{{ form.dis".length)
        val twigElement = template.findElementAt(myFixture.caretOffset)!!
        val twigHandler = manager.findUsagesManager.getFindUsagesHandler(twigElement, false)!!
        assertEquals(target, twigHandler.primaryElements.single())
    }

    fun testPrivateAliasRespectsSearchScope() {
        val template = myFixture.addFileToProject("templates/form/attributes.html.twig", "{{ form.displayName }}")
        val target = field("internalName")

        assertEquals(1, usages(target, GlobalSearchScope.fileScope(template)).size)
        assertEquals(1, usages(target, LocalSearchScope(template)).size)
        assertEmpty(usages(target, LocalSearchScope(target)))
    }

    fun testPropertyRenamePreservesExplicitFormName() {
        val template = myFixture.addFileToProject("templates/form/attributes.html.twig", "{{ form.displayName }}")
        val target = field("internalName")
        assertEquals(target, usages(target).single().resolve())

        RenameProcessor(project, target, "updatedName", false, false).run()

        assertEquals("{{ form.displayName }}", template.text)
        assertTrue(target.containingFile.text.contains("\$updatedName"))
    }

    fun testPrivateTraitAliasFindsUsagesAcrossConsumingForms() {
        val template = myFixture.addFileToProject("templates/form/traits.html.twig", "{{ form.displayName }} {{ form.traitEmail }}")
        myFixture.addFileToProject("templates/form/other_traits.html.twig", "{{ form.displayName }}")
        myFixture.addFileToProject("templates/form/shadowed_traits.html.twig", "{{ form.displayName }}")
        myFixture.addFileToProject("templates/unrelated.html.twig", "{{ form.displayName }}")
        val target = PhpElementsUtil.getClassInterface(project, "App\\Form\\ContactFields")!!.findFieldByName("traitName", false)!!

        assertEquals(2, usages(target).size)
        assertEquals(target, usages(target).first().resolve())
        assertEquals(1, usages(target, GlobalSearchScope.fileScope(template)).size)
        assertEquals(1, usages(target, LocalSearchScope(template)).size)
        assertEmpty(usages(target, LocalSearchScope(target)))
        val publicTarget = PhpElementsUtil.getClassInterface(project, "App\\Form\\ContactFields")!!.findFieldByName("traitEmail", false)!!
        assertEquals(1, usages(publicTarget, GlobalSearchScope.fileScope(template)).size)
    }

    private fun field(name: String): Field =
        PhpElementsUtil.getClass(project, "\\App\\Form\\AccountData")!!.findFieldByName(name, false)!!

    private fun usages(target: PsiElement, scope: SearchScope = GlobalSearchScope.projectScope(project)): List<TwigMethodUsageReference> =
        ReferencesSearch.search(target, scope).findAll().filterIsInstance<TwigMethodUsageReference>()
}
