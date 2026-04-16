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
import com.jetbrains.php.lang.psi.elements.PhpClass
import com.jetbrains.twig.elements.TwigFieldReference
import com.jetbrains.twig.elements.TwigVariableReference
import fr.adrienbrault.idea.symfony2plugin.templating.usages.TwigFindUsagesHandler
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase

class TwigFindUsagesHandlerIntegrationTest : SymfonyLightCodeInsightFixtureTestCase() {
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

    fun testPlatformFindUsagesTriggeredFromTwigMethodDelegatesToPhpMethod() {
        myFixture.addFileToProject(
            "src/Bar.php",
            """
            <?php
            namespace Foo;
            class Bar {
                public function getId() {}
            }
            """.trimIndent()
        )

        myFixture.addFileToProject("templates/secondary.html.twig", "{# @var test \\Foo\\Bar #} {{ test.getId() }}")

        val psiFile = configureByProjectPath("templates/index.html.twig", "{# @var test \\Foo\\Bar #} {{ test.i<caret>d }}")
        val twigElement = PsiTreeUtil.getParentOfType(psiFile.findElementAt(myFixture.caretOffset), TwigFieldReference::class.java, false)
        assertNotNull(twigElement)

        val handler = getPlatformFindUsagesHandler(twigElement!!)
        assertInstanceOf(handler, TwigFindUsagesHandler::class.java)

        val primaryElements = handler!!.primaryElements
        assertEquals(1, primaryElements.size)
        assertInstanceOf(primaryElements[0], Method::class.java)

        val usages = findUsagesFromPlatform(twigElement)

        assertContainsUsageFile(usages, "templates/index.html.twig")
        assertContainsUsageFile(usages, "templates/secondary.html.twig")
    }

    fun testPlatformFindUsagesTriggeredFromTwigPropertyDelegatesToPhpField() {
        myFixture.addFileToProject(
            "src/Bar.php",
            $$"""
            <?php
            namespace Foo;
            class Bar {
                public function __construct(
                    public string $primaryValue,
                    public string $secondaryValue,
                ) {}
            }
            """.trimIndent()
        )

        myFixture.addFileToProject("templates/secondary.html.twig", "{# @var test \\Foo\\Bar #} {{ test.primaryValue|upper }}")
        myFixture.addFileToProject("templates/other.html.twig", "{# @var test \\Foo\\Bar #} {{ test.secondaryValue }}")

        val psiFile = configureByProjectPath("templates/index.html.twig", "{# @var test \\Foo\\Bar #} {{ test.primary<caret>Value }}")
        val twigElement = PsiTreeUtil.getParentOfType(psiFile.findElementAt(myFixture.caretOffset), TwigFieldReference::class.java, false)
        assertNotNull(twigElement)

        val handler = getPlatformFindUsagesHandler(twigElement!!)
        assertInstanceOf(handler, TwigFindUsagesHandler::class.java)

        val primaryElements = handler!!.primaryElements
        assertEquals(1, primaryElements.size)
        assertInstanceOf(primaryElements[0], Field::class.java)

        val usages = findUsagesFromPlatform(twigElement)

        assertContainsUsageFile(usages, "templates/index.html.twig")
        assertContainsUsageFile(usages, "templates/secondary.html.twig")
        assertNotContainsUsageFile(usages, "templates/other.html.twig")
    }

    fun testPlatformFindUsagesTriggeredFromTwigBaseVariableDelegatesToPhpClass() {
        myFixture.addFileToProject(
            "src/Bar.php",
            """
            <?php
            namespace Foo;
            class Bar {
                public function getId() {}
            }
            """.trimIndent()
        )

        val psiFile = configureByProjectPath("templates/index.html.twig", "{# @var foo \\Foo\\Bar #} {{ fo<caret>o.id }}")
        val twigElement = PsiTreeUtil.getParentOfType(psiFile.findElementAt(myFixture.caretOffset), TwigVariableReference::class.java, false)
        assertNotNull(twigElement)

        val handler = getPlatformFindUsagesHandler(twigElement!!)
        assertInstanceOf(handler, TwigFindUsagesHandler::class.java)

        val primaryElements = handler!!.primaryElements
        assertEquals(1, primaryElements.size)
        assertInstanceOf(primaryElements[0], PhpClass::class.java)
    }

    fun testPlatformFindUsagesTriggeredFromTwigConstantDelegatesToPhpConstant() {
        myFixture.addFileToProject(
            "src/FooConst.php",
            """
            <?php
            namespace Foo;
            class FooConst {
                public const BAR = 'bar';
            }
            """.trimIndent(),
        )

        myFixture.addFileToProject("templates/secondary.html.twig", "{{ constant('Foo\\\\FooConst::BAR') }}")

        val psiFile = configureByProjectPath("templates/index.html.twig", "{{ constant('Foo\\\\FooConst::B<caret>AR') }}")
        val twigElement = psiFile.findElementAt(myFixture.caretOffset)
        assertNotNull(twigElement)

        val handler = getPlatformFindUsagesHandler(twigElement!!)
        assertInstanceOf(handler, TwigFindUsagesHandler::class.java)

        val primaryElements = handler!!.primaryElements
        assertEquals(1, primaryElements.size)
        assertInstanceOf(primaryElements[0], Field::class.java)

        val usages = findUsagesFromPlatform(twigElement)

        assertContainsUsageFile(usages, "templates/index.html.twig")
        assertContainsUsageFile(usages, "templates/secondary.html.twig")
    }

    fun testPlatformFindUsagesTriggeredFromTwigEnumCasesDelegatesToPhpEnumClass() {
        myFixture.addFileToProject(
            "src/CardSuite.php",
            """
            <?php
            namespace Foo;
            enum CardSuite {
                case CLUBS;
                case SPADES;
            }
            """.trimIndent(),
        )

        myFixture.addFileToProject(
            "templates/secondary.html.twig",
            """
            {% for case in enum_cases('Foo\\CardSuite') %}
                {{ case.value }}
            {% endfor %}
            """.trimIndent(),
        )

        val psiFile = configureByProjectPath("templates/index.html.twig", "{{ enum_cases('Foo\\\\Card<caret>Suite') }}")
        val twigElement = psiFile.findElementAt(myFixture.caretOffset)
        assertNotNull(twigElement)

        val handler = getPlatformFindUsagesHandler(twigElement!!)
        assertInstanceOf(handler, TwigFindUsagesHandler::class.java)

        val primaryElements = handler!!.primaryElements
        assertEquals(1, primaryElements.size)
        assertInstanceOf(primaryElements[0], PhpClass::class.java)

        val usages = findUsagesFromPlatform(twigElement)

        assertContainsUsageFile(usages, "templates/index.html.twig")
        assertContainsUsageFile(usages, "templates/secondary.html.twig")
    }

    fun testPlatformFindUsagesTriggeredFromTwigVarCommentDelegatesToPhpClass() {
        myFixture.addFileToProject(
            "src/CardSuite.php",
            """
            <?php
            namespace Foo;
            class CardSuite {}
            """.trimIndent(),
        )

        myFixture.addFileToProject("templates/secondary.html.twig", "{# @var cardSuite \\Foo\\CardSuite #}")

        val psiFile = configureByProjectPath("templates/index.html.twig", "{# @var \\Foo\\Card<caret>Suite cardSuite #}")
        val twigElement = psiFile.findElementAt(myFixture.caretOffset)
        assertNotNull(twigElement)

        val handler = getPlatformFindUsagesHandler(twigElement!!)
        assertInstanceOf(handler, TwigFindUsagesHandler::class.java)

        val primaryElements = handler!!.primaryElements
        assertEquals(1, primaryElements.size)
        assertInstanceOf(primaryElements[0], PhpClass::class.java)

        val usages = findUsagesFromPlatform(twigElement)

        assertContainsUsageFile(usages, "templates/index.html.twig")
        assertContainsUsageFile(usages, "templates/secondary.html.twig")
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
