package fr.adrienbrault.idea.symfony2plugin.tests.templating.usages

import com.intellij.psi.PsiReference
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.twig.TwigFileType
import com.jetbrains.php.lang.psi.elements.Field
import com.jetbrains.php.lang.psi.elements.Method
import com.jetbrains.php.lang.psi.elements.PhpClass
import com.jetbrains.php.lang.psi.elements.PhpNamedElement
import fr.adrienbrault.idea.symfony2plugin.templating.usages.TwigMethodReferencesSearchExecutor
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase

class TwigMethodReferencesSearchExecutorTest : SymfonyLightCodeInsightFixtureTestCase() {
    override fun setUp() {
        super.setUp()
        myFixture.copyFileToProject("twig_extensions.php")
    }

    override fun getTestDataPath(): String {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/templating/util/fixtures"
    }

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

    fun testCollectTwigFilesVisitsEachTemplateOnlyOnceAcrossGetterSearchWords() {
        getMethodUnderCaret(
            """
            <?php
            namespace Foo;
            class Bar {
                public function getFo<caret>o() {}
            }
            """.trimIndent()
        )

        myFixture.addFileToProject("templates/dedup.html.twig", "{# @var bar \\Foo\\Bar #} {{ bar.foo }} {{ bar.getFoo() }}")
        myFixture.addFileToProject("templates/shortcut_only.html.twig", "{# @var bar \\Foo\\Bar #} {{ bar.foo }}")
        myFixture.addFileToProject("templates/method_only.html.twig", "{# @var bar \\Foo\\Bar #} {{ bar.getFoo() }}")

        val twigScope = GlobalSearchScope.getScopeRestrictedByFileTypes(
            GlobalSearchScope.projectScope(project),
            TwigFileType.INSTANCE,
        )

        val files = TwigMethodReferencesSearchExecutor().collectTwigFiles(project, twigScope, linkedSetOf("getFoo", "foo"))
        val dedupMatches = files.count { it.virtualFile.path.endsWith("templates/dedup.html.twig") }

        assertEquals(1, dedupMatches)
        assertEquals(3, files.size)
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

    fun testFindsTwigUsagesForClassConstants() {
        val target = getNamedElementUnderCaret(
            """
            <?php
            namespace Foo;
            class CardSuite {
                public const CL<caret>UBS = 'clubs';
                public const SPADES = 'spades';
            }
            """.trimIndent()
        )

        myFixture.addFileToProject("templates/constant.html.twig", "{{ constant('Foo\\\\CardSuite::CLUBS') }}")
        myFixture.addFileToProject("templates/other.html.twig", "{{ constant('Foo\\\\CardSuite::SPADES') }}")

        val references = getTwigUsageReferences(target)

        assertContainsSourceFile(references, "templates/constant.html.twig")
        assertNotContainsSourceFile(references, "templates/other.html.twig")
    }

    fun testFindsTwigUsagesForEnumCases() {
        val target = getNamedElementUnderCaret(
            """
            <?php
            namespace Foo;
            enum CardSuite {
                case CL<caret>UBS;
                case SPADES;
            }
            """.trimIndent()
        )

        myFixture.addFileToProject("templates/case.html.twig", "{{ constant('Foo\\\\CardSuite::CLUBS') }}")
        myFixture.addFileToProject("templates/other.html.twig", "{{ constant('Foo\\\\CardSuite::SPADES') }}")

        val references = getTwigUsageReferences(target)

        assertContainsSourceFile(references, "templates/case.html.twig")
        assertNotContainsSourceFile(references, "templates/other.html.twig")
    }

    fun testFindsTwigUsagesForEnumFunctionsAndVarComments() {
        val phpClass = getPhpClassUnderCaret(
            """
            <?php
            namespace Foo;
            enum Card<caret>Suite {
                case CLUBS;
                case SPADES;
            }
            """.trimIndent()
        )

        myFixture.addFileToProject("templates/enum.html.twig", "{{ enum('Foo\\\\CardSuite') }}")
        myFixture.addFileToProject(
            "templates/enum_cases.html.twig",
            """
            {% for case in enum_cases('Foo\\CardSuite') %}
                {{ case.value }}
            {% endfor %}
            """.trimIndent(),
        )
        myFixture.addFileToProject("templates/var_first.html.twig", "{# @var cardSuite \\Foo\\CardSuite #}")
        myFixture.addFileToProject("templates/class_first.html.twig", "{# @var \\Foo\\CardSuite cardSuite #}")
        myFixture.addFileToProject("templates/other.html.twig", "{{ enum('Foo\\\\OtherSuite') }}")

        myFixture.addFileToProject(
            "src/OtherSuite.php",
            """
            <?php
            namespace Foo;
            enum OtherSuite {
                case HEARTS;
            }
            """.trimIndent(),
        )

        val references = getTwigUsageReferences(phpClass)

        assertContainsSourceFile(references, "templates/enum.html.twig")
        assertContainsSourceFile(references, "templates/enum_cases.html.twig")
        assertContainsSourceFile(references, "templates/var_first.html.twig")
        assertContainsSourceFile(references, "templates/class_first.html.twig")
        assertNotContainsSourceFile(references, "templates/other.html.twig")
    }

    fun testFindsTwigUsagesForMultilineVarComments() {
        val phpClass = getPhpClassUnderCaret(
            """
            <?php
            namespace Foo;
            class Card<caret>Suite {}
            """.trimIndent()
        )

        myFixture.addFileToProject(
            "templates/multiline.html.twig",
            """
            {#
               @var otherSuite \Foo\OtherSuite
               @var \Foo\CardSuite cardSuite
               @var \Foo\CardSuite2 cardSuite2
            #}
            """.trimIndent(),
        )
        myFixture.addFileToProject(
            "templates/multiline_other.html.twig",
            """
            {#
               @var \Foo\OtherSuite otherSuite
            #}
            """.trimIndent(),
        )

        myFixture.addFileToProject(
            "src/OtherSuite.php",
            """
            <?php
            namespace Foo;
            class OtherSuite {}
            """.trimIndent(),
        )
        myFixture.addFileToProject(
            "src/CardSuite2.php",
            """
            <?php
            namespace Foo;
            class CardSuite2 {}
            """.trimIndent(),
        )

        val references = getTwigUsageReferences(phpClass)

        assertContainsSourceFile(references, "templates/multiline.html.twig")
        assertNotContainsSourceFile(references, "templates/multiline_other.html.twig")
    }

    fun testFindsTwigUsagesForAttributeTwigFunctionMethod() {
        val method = getMethodUnderCaret(
            $$"""
            <?php
            namespace App\Twig;
            class AppExtension
            {
                public function formatProductNumberFu<caret>nction(string $number): string
                {
                }
            }
            """.trimIndent()
        )

        myFixture.addFileToProject("templates/function.html.twig", "{{ product_number_function('123') }}")
        myFixture.addFileToProject("templates/other.html.twig", "{{ product_number_filter('123') }}")
        myFixture.addFileToProject("templates/unknown.html.twig", "{{ unknown_function('123') }}")

        val references = getTwigMethodUsageReferences(method)

        assertContainsSourceFile(references, "templates/function.html.twig")
        assertNotContainsSourceFile(references, "templates/other.html.twig")
        assertNotContainsSourceFile(references, "templates/unknown.html.twig")
    }

    fun testFindsTwigUsagesForClassicTwigFunctionMethod() {
        val method = getMethodUnderCaret(
            """
            <?php
            namespace Twig;
            class Extensions {
                public function foob<caret>ar()
                {
                }
            }
            """.trimIndent()
        )

        myFixture.addFileToProject("templates/function.html.twig", "{{ hwi_oauth_login_url() }}")
        myFixture.addFileToProject("templates/other.html.twig", "{{ product_number_function('123') }}")

        val references = getTwigMethodUsageReferences(method)

        assertContainsSourceFile(references, "templates/function.html.twig")
        assertNotContainsSourceFile(references, "templates/other.html.twig")
    }

    fun testFindsTwigUsagesForAttributeTwigFilterMethodAndApplyTag() {
        val method = getMethodUnderCaret(
            $$"""
            <?php
            namespace App\Twig;
            class AppExtension
            {
                public function formatProductNumberFi<caret>lter(string $number): string
                {
                }
            }
            """.trimIndent()
        )

        myFixture.addFileToProject("templates/filter.html.twig", "{{ price|product_number_filter }}")
        myFixture.addFileToProject("templates/apply.html.twig", "{% apply product_number_filter %}{{ price }}{% endapply %}")
        myFixture.addFileToProject("templates/other.html.twig", "{{ product_number_function('123') }}")

        val references = getTwigMethodUsageReferences(method)

        assertContainsSourceFile(references, "templates/filter.html.twig")
        assertContainsSourceFile(references, "templates/apply.html.twig")
        assertNotContainsSourceFile(references, "templates/other.html.twig")
    }

    fun testFindsTwigUsagesForClassicTwigFilterMethod() {
        val method = getMethodUnderCaret(
            """
            <?php
            namespace Twig;
            class Extensions {
                public function foob<caret>ar()
                {
                }
            }
            """.trimIndent()
        )

        myFixture.addFileToProject("templates/filter.html.twig", "{{ value|trans }}")
        myFixture.addFileToProject("templates/other.html.twig", "{{ value|product_number_filter }}")

        val references = getTwigMethodUsageReferences(method)

        assertContainsSourceFile(references, "templates/filter.html.twig")
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

    private fun getNamedElementUnderCaret(content: String): com.intellij.psi.PsiElement {
        val psiFile = myFixture.configureByText("Bar.php", content)
        val element = psiFile.findElementAt(myFixture.caretOffset)
        assertNotNull(element)

        return generateSequence(element) { it.parent }
            .firstOrNull { it is PhpNamedElement }
            ?: element!!
    }

    private fun getPhpClassUnderCaret(content: String): PhpClass {
        val psiFile = myFixture.configureByText("Bar.php", content)
        val element = psiFile.findElementAt(myFixture.caretOffset)
        val phpClass = PsiTreeUtil.getParentOfType(element, PhpClass::class.java, false)
        assertNotNull(phpClass)
        return phpClass!!
    }

    private fun getTwigMethodUsageReferences(member: com.intellij.psi.PsiElement): Collection<TwigMethodReferencesSearchExecutor.TwigMethodUsageReference> {
        val references: Collection<PsiReference> = ReferencesSearch.search(member, GlobalSearchScope.projectScope(project)).findAll()
        return references.filterIsInstance<TwigMethodReferencesSearchExecutor.TwigMethodUsageReference>()
    }

    private fun getTwigUsageReferences(target: com.intellij.psi.PsiElement): Collection<TwigMethodReferencesSearchExecutor.TwigMethodUsageReference> {
        val references: Collection<PsiReference> = ReferencesSearch.search(target, GlobalSearchScope.projectScope(project)).findAll()
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
