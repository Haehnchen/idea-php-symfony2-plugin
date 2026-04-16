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
    override fun setUp() {
        super.setUp()
        myFixture.copyFileToProject("twig_extensions.php")
    }

    override fun getTestDataPath(): String {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/templating/util/fixtures"
    }

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

    fun testClassifiesTwigFunctionSymbolUsages() {
        val twigFile = configureByProjectPath("templates/index.html.twig", "{{ form_st<caret>art() }}")
        val element = twigFile.findElementAt(myFixture.caretOffset)
        assertNotNull(element)

        val usageType: UsageType? = TwigMethodUsageTypeProvider().getUsageType(
            element!!,
            arrayOf<UsageTarget>(PsiElement2UsageTargetAdapter(element, true))
        )

        assertNotNull(usageType)
        assertEquals("Twig", usageType.toString())
    }

    fun testClassifiesTwigFilterSymbolUsages() {
        val twigFile = configureByProjectPath("templates/index.html.twig", "{{ value|product_number_fi<caret>lter }}")
        val element = twigFile.findElementAt(myFixture.caretOffset)
        assertNotNull(element)

        val usageType: UsageType? = TwigMethodUsageTypeProvider().getUsageType(
            element!!,
            arrayOf<UsageTarget>(PsiElement2UsageTargetAdapter(element, true))
        )

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

    private fun configureByProjectPath(filePath: String, content: String): com.intellij.psi.PsiFile {
        val caretOffset = content.indexOf("<caret>")
        assertTrue("Missing <caret> marker", caretOffset >= 0)

        myFixture.addFileToProject(filePath, content.replace("<caret>", ""))
        myFixture.configureFromExistingVirtualFile(myFixture.findFileInTempDir(filePath))
        myFixture.editor.caretModel.moveToOffset(caretOffset)

        return myFixture.file
    }
}
