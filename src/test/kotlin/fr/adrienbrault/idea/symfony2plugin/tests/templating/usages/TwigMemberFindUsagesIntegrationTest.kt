package fr.adrienbrault.idea.symfony2plugin.tests.templating.usages

import com.intellij.find.FindManager
import com.intellij.find.findUsages.FindUsagesHandler
import com.intellij.find.findUsages.FindUsagesOptions
import com.intellij.find.impl.FindManagerImpl
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.usageView.UsageInfo
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.php.lang.psi.elements.Field
import com.jetbrains.php.lang.psi.elements.Method
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase

class TwigMemberFindUsagesIntegrationTest : SymfonyLightCodeInsightFixtureTestCase() {
    fun testPlatformFindUsagesForMethodIncludesTwigGetterUsages() {
        val psiFile = configureByProjectPath(
            "src/Bar.php",
            """
            <?php
            namespace Foo;
            class Bar {
                public function getI<caret>d() {}
            }
            """.trimIndent()
        )

        myFixture.addFileToProject("templates/property_method.html.twig", "{# @var test \\Foo\\Bar #} {{ test.getId }}")
        myFixture.addFileToProject("templates/call_method.html.twig", "{# @var test \\Foo\\Bar #} {{ test.getId() }}")
        myFixture.addFileToProject("templates/shortcut.html.twig", "{# @var test \\Foo\\Bar #} {{ test.id }}")

        val element = psiFile.findElementAt(myFixture.caretOffset)
        val method = PsiTreeUtil.getParentOfType(element, Method::class.java, false)
        assertNotNull(method)

        val usages = findUsagesFromPlatform(method!!)

        assertContainsUsageFile(usages, "templates/property_method.html.twig")
        assertContainsUsageFile(usages, "templates/call_method.html.twig")
        assertContainsUsageFile(usages, "templates/shortcut.html.twig")
    }

    fun testPlatformFindUsagesForPromotedFieldIncludesTwigPropertyUsage() {
        val psiFile = configureByProjectPath(
            "src/Bar.php",
            $$"""
            <?php
            namespace Foo;
            class Bar {
                public function __construct(
                    public string $primary<caret>Value,
                    public string $secondaryValue,
                ) {}
            }
            """.trimIndent()
        )

        myFixture.addFileToProject("templates/field.html.twig", "{# @var test \\Foo\\Bar #} {{ test.primaryValue }}")
        myFixture.addFileToProject("templates/other.html.twig", "{# @var test \\Foo\\Bar #} {{ test.secondaryValue }}")

        val element = psiFile.findElementAt(myFixture.caretOffset)
        val field = PsiTreeUtil.getParentOfType(element, Field::class.java, false)
        assertNotNull(field)

        val usages = findUsagesFromPlatform(field!!)

        assertContainsUsageFile(usages, "templates/field.html.twig")
        assertNotContainsUsageFile(usages, "templates/other.html.twig")
    }

    private fun findUsagesFromPlatform(targetElement: PsiElement): List<UsageInfo> {
        val handler = getPlatformFindUsagesHandler(targetElement)
        assertNotNull(handler)

        val options: FindUsagesOptions = handler!!.findUsagesOptions
        options.searchScope = GlobalSearchScope.projectScope(project)
        options.isSearchForTextOccurrences = false
        options.isUsages = true

        val usages = mutableListOf<UsageInfo>()
        val completed = handler.processElementUsages(targetElement, { usages.add(it) }, options)
        assertTrue("Find usages processing was interrupted", completed)

        return usages
    }

    private fun getPlatformFindUsagesHandler(targetElement: PsiElement): FindUsagesHandler? {
        val findManager = FindManager.getInstance(project) as FindManagerImpl
        return findManager.findUsagesManager.getFindUsagesHandler(targetElement, false)
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
