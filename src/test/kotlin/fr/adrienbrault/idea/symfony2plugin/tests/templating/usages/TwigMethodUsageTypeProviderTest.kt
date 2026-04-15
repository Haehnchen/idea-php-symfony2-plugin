package fr.adrienbrault.idea.symfony2plugin.tests.templating.usages

import com.intellij.find.findUsages.PsiElement2UsageTargetAdapter
import com.intellij.psi.PsiReference
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.usages.UsageTarget
import com.intellij.usages.impl.rules.UsageType
import com.jetbrains.php.lang.psi.elements.Field
import com.jetbrains.php.lang.psi.elements.Method
import fr.adrienbrault.idea.symfony2plugin.templating.usages.TwigMethodUsageTypeProvider
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase

class TwigMethodUsageTypeProviderTest : SymfonyLightCodeInsightFixtureTestCase() {
    fun testClassifiesTwigMethodUsages() {
        val method = getMethodUnderCaret(
            """
            <?php
            namespace Foo;
            class Bar {
                public function getFo<caret>o() {}
            }
            """.trimIndent()
        )

        myFixture.addFileToProject("templates/shortcut.html.twig", "{# @var bar \\Foo\\Bar #} {{ bar.foo }}")

        val references: Collection<PsiReference> = ReferencesSearch.search(method, GlobalSearchScope.projectScope(project)).findAll()
        val reference = references.firstOrNull { it.element.containingFile.virtualFile?.path?.endsWith("templates/shortcut.html.twig") == true }
        assertNotNull(reference)

        val usageType: UsageType? = TwigMethodUsageTypeProvider().getUsageType(reference!!.element, arrayOf<UsageTarget>(PsiElement2UsageTargetAdapter(method, true)))
        assertNotNull(usageType)
        assertEquals("Twig", usageType.toString())
    }

    fun testClassifiesTwigFieldUsages() {
        val field = getFieldUnderCaret(
            $$"""
            <?php
            namespace Foo;
            class Bar {
                public string $fo<caret>o;
            }
            """.trimIndent()
        )

        myFixture.addFileToProject("templates/shortcut.html.twig", "{# @var bar \\Foo\\Bar #} {{ bar.foo }}")

        val references: Collection<PsiReference> = ReferencesSearch.search(field, GlobalSearchScope.projectScope(project)).findAll()
        val reference = references.firstOrNull { it.element.containingFile.virtualFile?.path?.endsWith("templates/shortcut.html.twig") == true }
        assertNotNull(reference)

        val usageType: UsageType? = TwigMethodUsageTypeProvider().getUsageType(reference!!.element, arrayOf<UsageTarget>(PsiElement2UsageTargetAdapter(field, true)))
        assertNotNull(usageType)
        assertEquals("Twig", usageType.toString())
    }

    private fun getMethodUnderCaret(content: String): Method {
        val psiFile = myFixture.configureByText("Bar.php", content)
        val element = psiFile.findElementAt(myFixture.caretOffset)
        val method = PsiTreeUtil.getParentOfType(element, Method::class.java, false)
        assertNotNull(method)
        return method!!
    }

    private fun getFieldUnderCaret(content: String): Field {
        val psiFile = myFixture.configureByText("Bar.php", content)
        val element = psiFile.findElementAt(myFixture.caretOffset)
        val field = PsiTreeUtil.getParentOfType(element, Field::class.java, false)
        assertNotNull(field)
        return field!!
    }
}
