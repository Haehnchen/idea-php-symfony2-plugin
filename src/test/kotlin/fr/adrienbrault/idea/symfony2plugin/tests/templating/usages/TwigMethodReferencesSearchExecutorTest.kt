package fr.adrienbrault.idea.symfony2plugin.tests.templating.usages

import com.intellij.psi.PsiReference
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.php.lang.psi.elements.Field
import com.jetbrains.php.lang.psi.elements.Method
import fr.adrienbrault.idea.symfony2plugin.templating.usages.TwigMethodReferencesSearchExecutor
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase

class TwigMethodReferencesSearchExecutorTest : SymfonyLightCodeInsightFixtureTestCase() {
    fun testFindsTwigUsagesForShortcutAndMethodName() {
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
        myFixture.addFileToProject("templates/method.html.twig", "{# @var bar \\Foo\\Bar #} {{ bar.getFoo() }}")
        myFixture.addFileToProject("templates/uppercase.html.twig", "{# @var bar \\Foo\\Bar #} {{ bar.FOO }}")
        myFixture.addFileToProject("templates/other_type.html.twig", "{# @var bar \\Foo\\Baz #} {{ bar.foo }}")
        myFixture.addFileToProject("templates/unknown_type.html.twig", "{# @var bar \\Foo\\Missing #} {{ bar.foo }}")

        myFixture.addFileToProject(
            "src/Baz.php",
            """
            <?php
            namespace Foo;
            class Baz {
                public function getFoo() {}
            }
            """.trimIndent()
        )

        val references = getTwigMethodUsageReferences(method)

        assertContainsSourceFile(references, "templates/shortcut.html.twig")
        assertContainsSourceFile(references, "templates/method.html.twig")
        assertContainsSourceFile(references, "templates/uppercase.html.twig")
        assertNotContainsSourceFile(references, "templates/other_type.html.twig")
        assertNotContainsSourceFile(references, "templates/unknown_type.html.twig")
    }

    fun testIgnoresInaccessibleMethods() {
        val method = getMethodUnderCaret(
            """
            <?php
            namespace Foo;
            class Bar {
                public function setFo<caret>o() {}
            }
            """.trimIndent()
        )

        myFixture.addFileToProject("templates/shortcut.html.twig", "{# @var bar \\Foo\\Bar #} {{ bar.foo }}")

        val references = getTwigMethodUsageReferences(method)
        assertEmpty(references)
    }

    fun testFindsTwigUsagesForExplicitGetterSyntax() {
        val method = getMethodUnderCaret(
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

        val references = getTwigMethodUsageReferences(method)

        assertContainsSourceFile(references, "templates/property_method.html.twig")
        assertContainsSourceFile(references, "templates/call_method.html.twig")
    }

    fun testFindsTwigUsagesForPublicField() {
        val field = getFieldUnderCaret(
            $$"""
            <?php
            namespace Foo;
            class Bar {
                public string $fo<caret>o;
            }
            """.trimIndent()
        )

        myFixture.addFileToProject("templates/field.html.twig", "{# @var bar \\Foo\\Bar #} {{ bar.foo }}")
        myFixture.addFileToProject("templates/method.html.twig", "{# @var bar \\Foo\\Bar #} {{ bar.getFoo() }}")
        myFixture.addFileToProject("templates/other_type.html.twig", "{# @var bar \\Foo\\Baz #} {{ bar.foo }}")

        myFixture.addFileToProject(
            "src/Baz.php",
            $$"""
            <?php
            namespace Foo;
            class Baz {
                public string $foo;
            }
            """.trimIndent()
        )

        val references = getTwigMethodUsageReferences(field)

        assertContainsSourceFile(references, "templates/field.html.twig")
        assertNotContainsSourceFile(references, "templates/method.html.twig")
        assertNotContainsSourceFile(references, "templates/other_type.html.twig")
    }

    fun testFindsTwigUsagesForPromotedPublicField() {
        val field = getFieldUnderCaret(
            $$"""
            <?php
            namespace Foo;
            class Bar {
                public function __construct(
                    public float $primary<caret>Value,
                    public float $secondaryValue,
                ) {}
            }
            """.trimIndent()
        )

        myFixture.addFileToProject("templates/promoted.html.twig", "{# @var bar \\Foo\\Bar #} {{ bar.primaryValue }}")
        myFixture.addFileToProject("templates/other.html.twig", "{# @var bar \\Foo\\Bar #} {{ bar.secondaryValue }}")

        val references = getTwigMethodUsageReferences(field)

        assertContainsSourceFile(references, "templates/promoted.html.twig")
        assertNotContainsSourceFile(references, "templates/other.html.twig")
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

    private fun getTwigMethodUsageReferences(member: Any): Collection<TwigMethodReferencesSearchExecutor.TwigMethodUsageReference> {
        val references: Collection<PsiReference> = ReferencesSearch.search(member as com.intellij.psi.PsiElement, GlobalSearchScope.projectScope(project)).findAll()
        return references.filterIsInstance<TwigMethodReferencesSearchExecutor.TwigMethodUsageReference>()
    }

    private fun assertContainsSourceFile(
        references: Collection<TwigMethodReferencesSearchExecutor.TwigMethodUsageReference>,
        relativePath: String,
    ) {
        for (reference in references) {
            if (reference.element.containingFile.virtualFile?.path?.endsWith(relativePath) == true) {
                return
            }
        }

        fail("Expected reference from file: $relativePath")
    }

    private fun assertNotContainsSourceFile(
        references: Collection<TwigMethodReferencesSearchExecutor.TwigMethodUsageReference>,
        relativePath: String,
    ) {
        for (reference in references) {
            if (reference.element.containingFile.virtualFile?.path?.endsWith(relativePath) == true) {
                fail("Did not expect reference from file: $relativePath")
            }
        }
    }
}
