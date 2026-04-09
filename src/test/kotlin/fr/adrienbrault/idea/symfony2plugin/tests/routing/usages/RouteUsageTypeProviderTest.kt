package fr.adrienbrault.idea.symfony2plugin.tests.routing.usages

import com.intellij.find.findUsages.FindUsagesHandler
import com.intellij.find.findUsages.PsiElement2UsageTargetAdapter
import com.intellij.navigation.ItemPresentation
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiReference
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.usages.PsiElementUsageTarget
import com.intellij.usages.UsageTarget
import com.intellij.usages.impl.rules.UsageType
import fr.adrienbrault.idea.symfony2plugin.routing.usages.RouteFindUsagesHandlerFactory
import fr.adrienbrault.idea.symfony2plugin.routing.usages.RouteUsageTypeProvider
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase

class RouteUsageTypeProviderTest : SymfonyLightCodeInsightFixtureTestCase() {
    fun testClassifiesTwigRouteUsages() {
        val target = myFixture.configureByText(
            "controller.php",
            """
            <?php
            namespace App\Controller;
            use Symfony\Component\Routing\Attribute\Route;
            class MyController {
                #[Route('/foo', name: 'app_report_submitter<caret>')]
                public function __invoke() {}
            }
            """.trimIndent()
        ).findElementAt(myFixture.caretOffset)

        myFixture.addFileToProject("templates/path.html.twig", "{{ path('app_report_submitter') }}")
        myFixture.addFileToProject("templates/url.html.twig", "{{ url('app_report_submitter') }}")
        myFixture.addFileToProject("templates/compare.html.twig", "{% if app.request.attributes.get('_route') == 'app_report_submitter' %}{% endif %}")
        myFixture.addFileToProject("templates/same_as.html.twig", "{% if app.request.attributes.get('_route') is same as('app_report_submitter') %}{% endif %}")
        myFixture.addFileToProject("templates/in_array.html.twig", "{% if app.request.attributes.get('_route') in ['app_report_submitter', 'other_route'] %}{% endif %}")

        val references: Collection<PsiReference> = ReferencesSearch.search(target!!, GlobalSearchScope.projectScope(project)).findAll()
        val targets = arrayOf<UsageTarget>(TestUsageTarget(target))
        val typeProvider = RouteUsageTypeProvider()

        val expectedBySourceFile = linkedMapOf(
            "templates/path.html.twig" to "Twig",
            "templates/url.html.twig" to "Twig",
            "templates/compare.html.twig" to "Twig",
            "templates/same_as.html.twig" to "Twig",
            "templates/in_array.html.twig" to "Twig",
        )

        for ((path, expectedType) in expectedBySourceFile) {
            val reference = findReferenceBySourceFile(references, path)
            assertNotNull("Expected reference from file: $path", reference)

            val usageType = typeProvider.getUsageType(reference!!.element, targets)
            assertNotNull("Usage type should be detected for: $path", usageType)
            assertEquals(expectedType, usageType.toString())
        }
    }

    fun testClassifiesTwigUsageForCustomFindUsagesTarget() {
        val target = myFixture.configureByText(
            "controller.php",
            """
            <?php
            namespace App\Controller;
            use Symfony\Component\Routing\Attribute\Route;
            class MyController {
                #[Route('/foo', name: 'app_report_submitter<caret>')]
                public function __invoke() {}
            }
            """.trimIndent()
        ).findElementAt(myFixture.caretOffset)

        myFixture.addFileToProject("templates/path.html.twig", "{{ path('app_report_submitter') }}")

        val references: Collection<PsiReference> = ReferencesSearch.search(target!!, GlobalSearchScope.projectScope(project)).findAll()
        val reference = findReferenceBySourceFile(references, "templates/path.html.twig")
        assertNotNull(reference)

        val handler: FindUsagesHandler? = RouteFindUsagesHandlerFactory().createFindUsagesHandler(target, false)
        assertNotNull(handler)

        val targets = arrayOf<UsageTarget>(PsiElement2UsageTargetAdapter(handler!!.primaryElements[0], true))
        val usageType: UsageType? = RouteUsageTypeProvider().getUsageType(reference!!.element, targets)
        assertNotNull(usageType)
        assertEquals("Twig", usageType.toString())
    }

    private fun findReferenceBySourceFile(references: Collection<PsiReference>, relativePath: String): PsiReference? {
        for (reference in references) {
            if (reference.element.containingFile.virtualFile?.path?.endsWith(relativePath) == true) {
                return reference
            }
        }

        return null
    }

    private class TestUsageTarget(private val element: PsiElement) : PsiElementUsageTarget {
        override fun getElement(): PsiElement = element

        override fun isValid(): Boolean = element.isValid

        override fun findUsages() {
        }

        override fun findUsagesInEditor(editor: FileEditor) {
        }

        override fun highlightUsages(file: PsiFile, editor: Editor, clearHighlights: Boolean) {
        }

        override fun isReadOnly(): Boolean = false

        override fun getFiles(): Array<VirtualFile> = VirtualFile.EMPTY_ARRAY

        override fun update() {
        }

        override fun getName(): String = element.text

        override fun getPresentation(): ItemPresentation? = null

        override fun canNavigate(): Boolean = false

        override fun canNavigateToSource(): Boolean = false

        override fun navigate(requestFocus: Boolean) {
        }
    }
}
