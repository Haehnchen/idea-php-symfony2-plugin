package fr.adrienbrault.idea.symfony2plugin.tests.templating.usages

import com.intellij.find.findUsages.FindUsagesOptions
import com.intellij.psi.PsiFile
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.usageView.UsageInfo
import com.jetbrains.php.lang.psi.elements.Method
import com.jetbrains.twig.elements.TwigFieldReference
import fr.adrienbrault.idea.symfony2plugin.templating.usages.TwigExtensionSymbolFindUsagesTarget
import fr.adrienbrault.idea.symfony2plugin.templating.usages.TwigExtensionUsageKind
import fr.adrienbrault.idea.symfony2plugin.templating.usages.TwigExtensionUsageSymbol
import fr.adrienbrault.idea.symfony2plugin.templating.usages.TwigPhpFindUsagesTarget
import fr.adrienbrault.idea.symfony2plugin.templating.usages.TwigFindUsagesHandler
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase

class TwigFindUsagesHandlerTest : SymfonyLightCodeInsightFixtureTestCase() {
    override fun getTestDataPath(): String {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/templating/util/fixtures"
    }

    fun testPrimaryElementsExposeResolvedPhpTargets() {
        val phpFile = configureByProjectPath(
            "src/Bar.php",
            """
            <?php
            namespace Foo;
            class Bar {
                public function getI<caret>d() {}
            }
            """.trimIndent()
        )

        val method = PsiTreeUtil.getParentOfType(phpFile.findElementAt(myFixture.caretOffset), Method::class.java, false)
        assertNotNull(method)

        val twigFile = configureByProjectPath("templates/index.html.twig", "{# @var test \\Foo\\Bar #} {{ test.i<caret>d }}")
        val twigElement = PsiTreeUtil.getParentOfType(twigFile.findElementAt(myFixture.caretOffset), TwigFieldReference::class.java, false)
        assertNotNull(twigElement)

        val handler = TwigFindUsagesHandler(TwigPhpFindUsagesTarget(twigElement!!, listOf(method!!)))

        assertEquals(1, handler.primaryElements.size)
        assertSame(method, handler.primaryElements[0])
    }

    fun testProcessElementUsagesDelegatesTwigElementToResolvedPhpMethod() {
        val phpFile = configureByProjectPath(
            "src/Bar.php",
            """
            <?php
            namespace Foo;
            class Bar {
                public function getI<caret>d() {}
            }
            """.trimIndent()
        )

        myFixture.addFileToProject("templates/secondary.html.twig", "{# @var test \\Foo\\Bar #} {{ test.getId() }}")

        val method = PsiTreeUtil.getParentOfType(phpFile.findElementAt(myFixture.caretOffset), Method::class.java, false)
        assertNotNull(method)

        val twigFile = configureByProjectPath("templates/index.html.twig", "{# @var test \\Foo\\Bar #} {{ test.i<caret>d }}")
        val twigElement = PsiTreeUtil.getParentOfType(twigFile.findElementAt(myFixture.caretOffset), TwigFieldReference::class.java, false)
        assertNotNull(twigElement)

        val handler = TwigFindUsagesHandler(TwigPhpFindUsagesTarget(twigElement!!, listOf(method!!)))
        val options: FindUsagesOptions = handler.findUsagesOptions
        options.searchScope = GlobalSearchScope.projectScope(project)
        options.isSearchForTextOccurrences = false
        options.isUsages = true

        val usages = mutableListOf<UsageInfo>()
        val completed = handler.processElementUsages(twigElement, { usages.add(it) }, options)
        assertTrue("Find usages processing was interrupted", completed)

        assertContainsUsageFile(usages, "templates/index.html.twig")
        assertContainsUsageFile(usages, "templates/secondary.html.twig")
    }

    fun testProcessElementUsagesSearchesTwigExtensionSymbolsByExactName() {
        myFixture.copyFileToProject("twig_extensions.php")

        val twigFile = configureByProjectPath("templates/index.html.twig", "{{ form_st<caret>art() }}")
        myFixture.addFileToProject("templates/secondary.html.twig", "{{ form_start() }}")
        myFixture.addFileToProject("templates/other.html.twig", "{{ form() }} {{ form_end() }}")

        val twigElement = twigFile.findElementAt(myFixture.caretOffset)
        assertNotNull(twigElement)

        val handler = TwigFindUsagesHandler(
            TwigExtensionSymbolFindUsagesTarget(
                primaryElement = twigElement!!,
                symbol = TwigExtensionUsageSymbol(TwigExtensionUsageKind.FUNCTION, "form_start"),
            )
        )
        val options: FindUsagesOptions = handler.findUsagesOptions
        options.searchScope = GlobalSearchScope.projectScope(project)
        options.isSearchForTextOccurrences = false
        options.isUsages = true

        val usages = mutableListOf<UsageInfo>()
        val completed = handler.processElementUsages(twigElement, { usages.add(it) }, options)
        assertTrue("Find usages processing was interrupted", completed)

        assertContainsUsageFile(usages, "templates/index.html.twig")
        assertContainsUsageFile(usages, "templates/secondary.html.twig")
        assertNotContainsUsageFile(usages, "templates/other.html.twig")
    }

    private fun configureByProjectPath(filePath: String, content: String): PsiFile {
        val caretOffset = content.indexOf("<caret>")
        assertTrue("Missing <caret> marker", caretOffset >= 0)

        myFixture.addFileToProject(filePath, content.replace("<caret>", ""))
        myFixture.configureFromExistingVirtualFile(myFixture.findFileInTempDir(filePath))
        myFixture.editor.caretModel.moveToOffset(caretOffset)

        return myFixture.file
    }

    private fun assertContainsUsageFile(usages: List<UsageInfo>, relativePath: String) {
        val actualUsages = usages.map { it.virtualFile?.path ?: "<no-path>" }
        if (usages.any { it.virtualFile?.path?.endsWith(relativePath) == true }) {
            return
        }

        fail("Expected usage in file: $relativePath; actual: $actualUsages")
    }

    private fun assertNotContainsUsageFile(usages: List<UsageInfo>, relativePath: String) {
        if (usages.any { it.virtualFile?.path?.endsWith(relativePath) == true }) {
            fail("Did not expect usage in file: $relativePath")
        }
    }
}
