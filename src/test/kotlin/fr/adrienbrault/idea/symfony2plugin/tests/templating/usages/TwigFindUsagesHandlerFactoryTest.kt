package fr.adrienbrault.idea.symfony2plugin.tests.templating.usages

import com.intellij.find.findUsages.PsiElement2UsageTargetAdapter
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.php.lang.psi.elements.Field
import com.jetbrains.php.lang.psi.elements.Method
import com.jetbrains.php.lang.psi.elements.PhpClass
import com.jetbrains.php.lang.psi.elements.PhpEnumCase
import com.jetbrains.twig.elements.TwigFieldReference
import com.jetbrains.twig.elements.TwigVariableReference
import fr.adrienbrault.idea.symfony2plugin.templating.usages.TwigFindUsagesHandler
import fr.adrienbrault.idea.symfony2plugin.templating.usages.TwigFindUsagesHandlerFactory
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase

class TwigFindUsagesHandlerFactoryTest : SymfonyLightCodeInsightFixtureTestCase() {
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

        val handler = TwigFindUsagesHandlerFactory().createFindUsagesHandler(twigElement!!, false)
        assertInstanceOf(handler, TwigFindUsagesHandler::class.java)
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

        val handler = TwigFindUsagesHandlerFactory().createFindUsagesHandler(twigElement!!, false)
        assertInstanceOf(handler, TwigFindUsagesHandler::class.java)
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

        val handler = TwigFindUsagesHandlerFactory().createFindUsagesHandler(twigElement!!, false)
        assertInstanceOf(handler, TwigFindUsagesHandler::class.java)
        assertEquals(1, handler!!.primaryElements.size)
        assertInstanceOf(handler.primaryElements[0], PhpClass::class.java)
    }

    fun testCreateFindUsagesHandlerReturnsNullForNonTypeTwigContext() {
        val psiFile = configureByProjectPath("templates/index.html.twig", "{% set value = 'fo<caret>o' %}")
        val element = psiFile.findElementAt(myFixture.caretOffset)
        assertNotNull(element)

        val handler = TwigFindUsagesHandlerFactory().createFindUsagesHandler(element!!, false)
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

        val handler = TwigFindUsagesHandlerFactory().createFindUsagesHandler(fieldReferences[0], false)
        assertInstanceOf(handler, TwigFindUsagesHandler::class.java)
        assertEquals(1, handler!!.primaryElements.size)
        assertInstanceOf(handler.primaryElements[0], Field::class.java)
        assertEquals("secondValue", (handler.primaryElements[0] as Field).name)
    }

    fun testCreateFindUsagesHandlerOnTwigConstantReturnsPhpFieldPrimaryElement() {
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

        val psiFile = configureByProjectPath("templates/index.html.twig", "{{ constant('Foo\\\\FooConst::B<caret>AR') }}")
        val twigElement = psiFile.findElementAt(myFixture.caretOffset)
        assertNotNull(twigElement)

        val handler = TwigFindUsagesHandlerFactory().createFindUsagesHandler(twigElement!!, false)
        assertInstanceOf(handler, TwigFindUsagesHandler::class.java)
        assertEquals(1, handler!!.primaryElements.size)
        assertInstanceOf(handler.primaryElements[0], Field::class.java)
    }

    fun testCreateFindUsagesHandlerOnTwigEnumReturnsPhpClassPrimaryElement() {
        myFixture.addFileToProject(
            "src/CardSuite.php",
            """
            <?php
            namespace Foo;
            enum CardSuite {
                case CLUBS;
            }
            """.trimIndent(),
        )

        val psiFile = configureByProjectPath("templates/index.html.twig", "{{ enum('Foo\\\\Card<caret>Suite') }}")
        val twigElement = psiFile.findElementAt(myFixture.caretOffset)
        assertNotNull(twigElement)

        val handler = TwigFindUsagesHandlerFactory().createFindUsagesHandler(twigElement!!, false)
        assertInstanceOf(handler, TwigFindUsagesHandler::class.java)
        assertEquals(1, handler!!.primaryElements.size)
        assertInstanceOf(handler.primaryElements[0], PhpClass::class.java)
    }

    fun testCreateFindUsagesHandlerOnTwigVarCommentReturnsPhpClassPrimaryElement() {
        myFixture.addFileToProject(
            "src/CardSuite.php",
            """
            <?php
            namespace Foo;
            class CardSuite {}
            """.trimIndent(),
        )

        val psiFile = configureByProjectPath("templates/index.html.twig", "{# @var cardSuite \\Foo\\Card<caret>Suite #}")
        val twigElement = psiFile.findElementAt(myFixture.caretOffset)
        assertNotNull(twigElement)

        val handler = TwigFindUsagesHandlerFactory().createFindUsagesHandler(twigElement!!, false)
        assertInstanceOf(handler, TwigFindUsagesHandler::class.java)
        assertEquals(1, handler!!.primaryElements.size)
        assertInstanceOf(handler.primaryElements[0], PhpClass::class.java)
    }

    fun testGetTargetsOnTwigConstantEnumCaseReturnsPhpEnumCaseUsageTarget() {
        myFixture.addFileToProject(
            "src/BetriebAboStatus.php",
            """
            <?php
            namespace Delos\Core\Enums;
            enum BetriebAboStatus: string {
                case ACTIVE = 'active';
            }
            """.trimIndent(),
        )

        val psiFile = configureByProjectPath(
            "templates/index.html.twig",
            "{{ constant('Delos\\\\Core\\\\Enums\\\\BetriebAboStatus::AC<caret>TIVE') }}"
        )
        val twigElement = psiFile.findElementAt(myFixture.caretOffset)
        assertNotNull(twigElement)

        val usageTargets = TwigFindUsagesHandlerFactory().getTargets(twigElement!!)
        assertEquals(1, usageTargets.size)

        val primaryElement = (usageTargets[0] as PsiElement2UsageTargetAdapter).element
        assertInstanceOf(primaryElement, PhpEnumCase::class.java)
        assertEquals("ACTIVE", (primaryElement as PhpEnumCase).name)
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
