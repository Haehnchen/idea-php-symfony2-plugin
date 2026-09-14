package fr.adrienbrault.idea.symfony2plugin.tests.templating.inspection

import fr.adrienbrault.idea.symfony2plugin.templating.inspection.TwigVariablePathInspection
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see TwigVariablePathInspection
 */
class TwigVariablePathInspectionTest : SymfonyLightCodeInsightFixtureTestCase() {
    override fun setUp() {
        super.setUp()
        myFixture.copyFileToProject("classes.php")
    }

    override fun getTestDataPath(): String {
        return "src/test/kotlin/fr/adrienbrault/idea/symfony2plugin/tests/templating/inspection/fixtures"
    }

    fun testThatUnknownBeforeLeafTypeShouldNotProvideHighlight() {
        assertLocalInspectionNotContains(TwigVariablePathInspection::class.java, "f.html.twig", "{# @var foo \\Foo\\Bar #} {{ bar.un<caret>known }}", "Field or method not found")
    }

    fun testThatOnlyVariablesWithPublicAccessLevelAreHighlighted() {
        assertLocalInspectionContains(TwigVariablePathInspection::class.java, "f.html.twig", "{# @var bar \\Foo\\Bar #} {{ bar.un<caret>known }}", "Field or method not found")
        assertLocalInspectionContains(TwigVariablePathInspection::class.java, "f.html.twig", "{# @var bar \\Foo\\Bar #} {{ bar.pri<caret>vate }}", "Field or method not found")
        assertLocalInspectionContains(TwigVariablePathInspection::class.java, "f.html.twig", "{# @var bar \\Foo\\Bar #} {{ bar.ap<caret>ple }}", "Field or method not found")
        assertLocalInspectionContains(TwigVariablePathInspection::class.java, "f.html.twig", "{# @var bar \\Foo\\Bar #} {{ bar.next.ap<caret>ple  }}", "Field or method not found")

        assertLocalInspectionNotContains(TwigVariablePathInspection::class.java, "f.html.twig", "{# @var bar \\Foo\\Bar #} {{ bar.c<caret>ar }}", "Field or method not found")
        assertLocalInspectionNotContains(TwigVariablePathInspection::class.java, "f.html.twig", "{# @var bar \\Foo\\Bar #} {{ bar.is<caret>Car }}", "Field or method not found")
        assertLocalInspectionNotContains(TwigVariablePathInspection::class.java, "f.html.twig", "{# @var bar \\Foo\\Bar #} {{ bar.has<caret>Hassers }}", "Field or method not found")
        assertLocalInspectionNotContains(TwigVariablePathInspection::class.java, "f.html.twig", "{# @var bar \\Foo\\Bar #} {{ bar.ha<caret>ssers }}", "Field or method not found")
        assertLocalInspectionNotContains(TwigVariablePathInspection::class.java, "f.html.twig", "{# @var bar \\Foo\\Bar #} {{ bar.pub<caret>lic }}", "Field or method not found")
        assertLocalInspectionNotContains(TwigVariablePathInspection::class.java, "f.html.twig", "{# @var bar \\Foo\\Bar #} {{ bar.next.pub<caret>lic }}", "Field or method not found")
        assertLocalInspectionNotContains(TwigVariablePathInspection::class.java, "f.html.twig", "{# @var bar \\Foo\\Bar #} {{ bar.getNext.pub<caret>lic }}", "Field or method not found")
        assertLocalInspectionNotContains(TwigVariablePathInspection::class.java, "f.html.twig", "{# @var bar \\Foo\\Bar #} {{ bar.FO<caret>O }}", "Field or method not found")
    }

    fun testThatNestedPathsHighlightFirstUnknownElement() {
        assertLocalInspectionContains(TwigVariablePathInspection::class.java, "f.html.twig", "{# @var bar \\Foo\\Bar #} {{ bar.un<caret>known.public }}", "Field or method not found")
        assertLocalInspectionContains(TwigVariablePathInspection::class.java, "f.html.twig", "{# @var bar \\Foo\\Bar #} {{ bar.next.un<caret>known.public }}", "Field or method not found")
        assertLocalInspectionContains(TwigVariablePathInspection::class.java, "f.html.twig", "{# @var bar \\Foo\\Bar #} {{ bar.ap<caret>ple.public }}", "Field or method not found")

        assertLocalInspectionNotContains(TwigVariablePathInspection::class.java, "f.html.twig", "{# @var bar \\Foo\\Bar #} {{ bar.next.pub<caret>lic.unknown }}", "Field or method not found")
        assertLocalInspectionNotContains(TwigVariablePathInspection::class.java, "f.html.twig", "{# @var foo \\Foo\\Bar #} {{ bar.un<caret>known.public }}", "Field or method not found")
    }

    fun testThatNestedPathsWithMethodCallsHighlightFirstUnknownElement() {
        assertLocalInspectionContains(TwigVariablePathInspection::class.java, "f.html.twig", "{# @var bar \\Foo\\Bar #} {{ bar.getNext().un<caret>known.public }}", "Field or method not found")
        assertLocalInspectionContains(TwigVariablePathInspection::class.java, "f.html.twig", "{# @var bar \\Foo\\Bar #} {{ bar.getNext().ap<caret>ple.public }}", "Field or method not found")
        assertLocalInspectionContains(TwigVariablePathInspection::class.java, "f.html.twig", "{# @var bar \\Foo\\Bar #} {{ bar.getNext().getNext().un<caret>known.public }}", "Field or method not found")
        assertLocalInspectionContains(TwigVariablePathInspection::class.java, "f.html.twig", "{# @var bar \\Foo\\Bar #} {{ bar.getNext().getNext().ap<caret>ple.public }}", "Field or method not found")

        assertLocalInspectionNotContains(TwigVariablePathInspection::class.java, "f.html.twig", "{# @var bar \\Foo\\Bar #} {{ bar.getNext().pub<caret>lic }}", "Field or method not found")
        assertLocalInspectionNotContains(TwigVariablePathInspection::class.java, "f.html.twig", "{# @var bar \\Foo\\Bar #} {{ bar.getNext().getNext().pub<caret>lic }}", "Field or method not found")
    }

    fun testTwigFilterAndFunctionReturnTypePathsAreInspected() {
        addTwigStringExtensionFixture()

        assertLocalInspectionNotContains(TwigVariablePathInspection::class.java, "f.html.twig", "{{ 'Symfony'|u.trun<caret>cate(8) }}", "Field or method not found")
        assertLocalInspectionContains(TwigVariablePathInspection::class.java, "f.html.twig", "{{ 'Symfony'|u.un<caret>known }}", "Field or method not found")

        assertLocalInspectionNotContains(TwigVariablePathInspection::class.java, "f.html.twig", "{{ ustring().trun<caret>cate(8) }}", "Field or method not found")
        assertLocalInspectionContains(TwigVariablePathInspection::class.java, "f.html.twig", "{{ ustring().un<caret>known }}", "Field or method not found")
        assertLocalInspectionNotContains(TwigVariablePathInspection::class.java, "f.html.twig", "{{ ustring(ass).trun<caret>cate(8) }}", "Field or method not found")
        assertLocalInspectionNotContains(TwigVariablePathInspection::class.java, "f.html.twig", "{{ ustring('aa').trun<caret>cate(8) }}", "Field or method not found")
        assertLocalInspectionNotContains(TwigVariablePathInspection::class.java, "f.html.twig", "{{ ustring('aaa').trun<caret>cate(8) }}", "Field or method not found")
        assertLocalInspectionNotContains(TwigVariablePathInspection::class.java, "f.html.twig", "{{ ustring(text: 'aaa').trun<caret>cate(8) }}", "Field or method not found")
        assertLocalInspectionContains(TwigVariablePathInspection::class.java, "f.html.twig", "{{ ustring(text: 'aaa').un<caret>known }}", "Field or method not found")
    }

    fun testUnionTypesDoNotHighlightKnownMembersFromEitherType() {
        addTwigUnionFixture()

        assertLocalInspectionNotContains(TwigVariablePathInspection::class.java, "f.html.twig", "{# @var account \\Foo\\Union\\User|\\Foo\\Union\\Admin #} {{ account.user<caret>name }}", "Field or method not found")
        assertLocalInspectionNotContains(TwigVariablePathInspection::class.java, "f.html.twig", "{# @var account \\Foo\\Union\\User|\\Foo\\Union\\Admin #} {{ account.role<caret>Name }}", "Field or method not found")
        assertLocalInspectionContains(TwigVariablePathInspection::class.java, "f.html.twig", "{# @var account \\Foo\\Union\\User|\\Foo\\Union\\Admin #} {{ account.un<caret>known }}", "Field or method not found")
    }

    fun testThatArrayAccessAndIteratorImplementationsDontHighlightAtAll() {
        assertLocalInspectionNotContains(TwigVariablePathInspection::class.java, "f.html.twig", "{# @var bar \\Foo\\BarArrayAccess #} {{ bar.c<caret>ar }}", "Field or method not found")
        assertLocalInspectionNotContains(TwigVariablePathInspection::class.java, "f.html.twig", "{# @var bar \\Foo\\BarIterator #} {{ bar.c<caret>ar }}", "Field or method not found")
        assertLocalInspectionNotContains(TwigVariablePathInspection::class.java, "f.html.twig", "{# @var bar \\Foo\\BarArrayIterator #} {{ bar.c<caret>ar }}", "Field or method not found")
    }

    fun testThatArrayReturnDontHighlight() {
        assertLocalInspectionNotContains(TwigVariablePathInspection::class.java, "f.html.twig", "{# @var bar \\Foo\\Bar #} {{ bar.array.f<caret>oo }}", "Field or method not found")

        // 2023.3.4: "Caching disabled due to recursion prevention, please get rid of cyclic dependencies"
        // assertLocalInspectionNotContains(TwigVariablePathInspection::class.java, "f.html.twig", "{# @var bar \\Foo\\Bar #} {{ bar.array.foo.fo<caret>o }}", "Field or method not found")
    }

    private fun addTwigStringExtensionFixture() {
        myFixture.addFileToProject(
            "src/Twig/StringExtension.php",
            "<?php\n" +
                "namespace {\n" +
                "    interface Twig_ExtensionInterface {}\n" +
                "    abstract class Twig_Extension implements Twig_ExtensionInterface {}\n" +
                "    class Twig_SimpleFilter {}\n" +
                "    class Twig_SimpleFunction {}\n" +
                "}\n" +
                "namespace Twig\\Extra\\String {\n" +
                "    use Symfony\\Component\\String\\UnicodeString;\n" +
                "    class StringExtension extends \\Twig_Extension {\n" +
                "        public function getFilters() { return [new \\Twig_SimpleFilter('u', [\$this, 'createUnicodeString'])]; }\n" +
                "        public function getFunctions() { return [new \\Twig_SimpleFunction('ustring', [\$this, 'createUnicodeString'])]; }\n" +
                "        public function createUnicodeString(?string \$text): UnicodeString { return new UnicodeString(\$text ?? ''); }\n" +
                "    }\n" +
                "}\n" +
                "namespace Symfony\\Component\\String {\n" +
                "    class UnicodeString {\n" +
                "        public function truncate(int \$length, string \$ellipsis = '', bool \$cut = true): static {}\n" +
                "    }\n" +
                "}\n"
        )
    }

    private fun addTwigUnionFixture() {
        myFixture.addFileToProject(
            "src/Foo/Union/User.php",
            "<?php\n" +
                "namespace Foo\\Union;\n" +
                "class User { public function getUsername(): string {} }\n"
        )

        myFixture.addFileToProject(
            "src/Foo/Union/Admin.php",
            "<?php\n" +
                "namespace Foo\\Union;\n" +
                "class Admin { public function getRoleName(): string {} }\n"
        )
    }
}
