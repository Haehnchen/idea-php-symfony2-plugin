package fr.adrienbrault.idea.symfony2plugin.tests.templating.usages

import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.php.lang.psi.elements.Field
import com.jetbrains.php.lang.psi.elements.Method
import com.jetbrains.php.lang.psi.elements.PhpClass
import com.jetbrains.twig.elements.TwigFieldReference
import com.jetbrains.twig.elements.TwigVariableReference
import fr.adrienbrault.idea.symfony2plugin.templating.usages.TwigTypeFindUsagesHandler
import fr.adrienbrault.idea.symfony2plugin.templating.usages.TwigTypeFindUsagesHandlerFactory
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase

class TwigTypeFindUsagesHandlerFactoryTest : SymfonyLightCodeInsightFixtureTestCase() {
    fun testCreateFindUsagesHandlerOnTwigMethodUsageReturnsPhpMethodPrimaryElement() {
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

        val psiFile = configureByProjectPath("templates/index.html.twig", "{# @var test \\Foo\\Bar #} {{ test.i<caret>d }}")
        val twigElement = PsiTreeUtil.getParentOfType(psiFile.findElementAt(myFixture.caretOffset), TwigFieldReference::class.java, false)
        assertNotNull(twigElement)

        val handler = TwigTypeFindUsagesHandlerFactory().createFindUsagesHandler(twigElement!!, false)
        assertInstanceOf(handler, TwigTypeFindUsagesHandler::class.java)
        assertEquals(1, handler!!.primaryElements.size)
        assertInstanceOf(handler.primaryElements[0], Method::class.java)
    }

    fun testCreateFindUsagesHandlerOnTwigPropertyUsageReturnsPhpFieldPrimaryElement() {
        myFixture.addFileToProject(
            "src/Bar.php",
            $$"""
            <?php
            namespace Foo;
            class Bar {
                public function __construct(
                    public string $primaryValue,
                ) {}
            }
            """.trimIndent()
        )

        val psiFile = configureByProjectPath("templates/index.html.twig", "{# @var test \\Foo\\Bar #} {{ test.primary<caret>Value }}")
        val twigElement = PsiTreeUtil.getParentOfType(psiFile.findElementAt(myFixture.caretOffset), TwigFieldReference::class.java, false)
        assertNotNull(twigElement)

        val handler = TwigTypeFindUsagesHandlerFactory().createFindUsagesHandler(twigElement!!, false)
        assertInstanceOf(handler, TwigTypeFindUsagesHandler::class.java)
        assertEquals(1, handler!!.primaryElements.size)
        assertInstanceOf(handler.primaryElements[0], Field::class.java)
    }

    fun testCreateFindUsagesHandlerOnTwigBaseVariableReturnsPhpClassPrimaryElement() {
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

        val handler = TwigTypeFindUsagesHandlerFactory().createFindUsagesHandler(twigElement!!, false)
        assertInstanceOf(handler, TwigTypeFindUsagesHandler::class.java)
        assertEquals(1, handler!!.primaryElements.size)
        assertInstanceOf(handler.primaryElements[0], PhpClass::class.java)
    }

    fun testCreateFindUsagesHandlerReturnsNullForNonTypeTwigContext() {
        val psiFile = configureByProjectPath("templates/index.html.twig", "{% set value = 'fo<caret>o' %}")
        val element = psiFile.findElementAt(myFixture.caretOffset)
        assertNotNull(element)

        val handler = TwigTypeFindUsagesHandlerFactory().createFindUsagesHandler(element!!, false)
        assertNull(handler)
    }

    fun testCreateFindUsagesHandlerUsesCurrentCaretLeafWhenPlatformPassesAnotherTwigOccurrence() {
        myFixture.addFileToProject(
            "src/Bar.php",
            $$"""
            <?php
            namespace Foo;
            class Bar {
                public function __construct(
                    public string $firstValue,
                    public string $secondValue,
                ) {}
            }
            """.trimIndent()
        )

        val psiFile = configureByProjectPath(
            "templates/index.html.twig",
            "{# @var test \\Foo\\Bar #} {{ test.firstValue }} {{ test.second<caret>Value }}"
        )

        val fieldReferences = PsiTreeUtil.findChildrenOfType(psiFile, TwigFieldReference::class.java).toList()
        assertEquals(2, fieldReferences.size)

        val handler = TwigTypeFindUsagesHandlerFactory().createFindUsagesHandler(fieldReferences[0], false)
        assertInstanceOf(handler, TwigTypeFindUsagesHandler::class.java)
        assertEquals(1, handler!!.primaryElements.size)
        assertInstanceOf(handler.primaryElements[0], Field::class.java)
        assertEquals("secondValue", (handler.primaryElements[0] as Field).name)
    }

    private fun configureByProjectPath(filePath: String, content: String): PsiFile {
        val caretOffset = content.indexOf("<caret>")
        assertTrue("Missing <caret> marker", caretOffset >= 0)

        myFixture.addFileToProject(filePath, content.replace("<caret>", ""))
        myFixture.configureFromExistingVirtualFile(myFixture.findFileInTempDir(filePath))
        myFixture.editor.caretModel.moveToOffset(caretOffset)

        return myFixture.file
    }
}
