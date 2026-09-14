package fr.adrienbrault.idea.symfony2plugin.tests.templating.inspection

import fr.adrienbrault.idea.symfony2plugin.templating.inspection.TwigVariableDeprecatedInspection
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.templating.inspection.TwigVariableDeprecatedInspection
 */
class TwigVariableDeprecatedInspectionTest : SymfonyLightCodeInsightFixtureTestCase() {
    override fun setUp() {
        super.setUp()
        myFixture.copyFileToProject("classes.php")
    }

    override fun getTestDataPath(): String {
        return "src/test/kotlin/fr/adrienbrault/idea/symfony2plugin/tests/templating/inspection/fixtures"
    }

    fun testThatDeprecatedMethodIsHighlighted() {
        assertLocalInspectionContains(TwigVariableDeprecatedInspection::class.java, "f.html.twig", "{# @var bar \\Foo\\Bar #} {{ bar.next.dep<caret>recated  }}", "Method 'Bar::getDeprecated' is deprecated")
    }

    fun testThatDeprecatedFieldIsHighlighted() {
        assertLocalInspectionContains(TwigVariableDeprecatedInspection::class.java, "f.html.twig", "{# @var bar \\Foo\\Bar #} {{ bar.deprecated<caret>Property  }}", "Field 'Bar::\$deprecatedProperty' is deprecated")
    }

    fun testThatAttributeDeprecatedMethodIsHighlighted() {
        assertLocalInspectionContains(TwigVariableDeprecatedInspection::class.java, "f.html.twig", "{# @var bar \\Foo\\Bar #} {{ bar.attribute<caret>Deprecated  }}", "Method 'Bar::getAttributeDeprecated' is deprecated")
    }

    fun testThatAttributeDeprecatedFieldIsHighlighted() {
        assertLocalInspectionContains(TwigVariableDeprecatedInspection::class.java, "f.html.twig", "{# @var bar \\Foo\\Bar #} {{ bar.deprecatedAttribute<caret>Property  }}", "Field 'Bar::\$deprecatedAttributeProperty' is deprecated")
    }

    fun testThatDeprecatedTwigFilterAndFunctionReturnTypeMethodsAreHighlighted() {
        addTwigStringExtensionFixture()

        assertLocalInspectionContains(TwigVariableDeprecatedInspection::class.java, "f.html.twig", "{{ 'Symfony'|u.dep<caret>recated }}", "Method 'UnicodeString::getDeprecated' is deprecated")
        assertLocalInspectionContains(TwigVariableDeprecatedInspection::class.java, "f.html.twig", "{{ ustring().dep<caret>recated }}", "Method 'UnicodeString::getDeprecated' is deprecated")
        assertLocalInspectionContains(TwigVariableDeprecatedInspection::class.java, "f.html.twig", "{{ ustring(ass).dep<caret>recated }}", "Method 'UnicodeString::getDeprecated' is deprecated")
        assertLocalInspectionContains(TwigVariableDeprecatedInspection::class.java, "f.html.twig", "{{ ustring('aa').dep<caret>recated }}", "Method 'UnicodeString::getDeprecated' is deprecated")
        assertLocalInspectionContains(TwigVariableDeprecatedInspection::class.java, "f.html.twig", "{{ ustring('aaa').dep<caret>recated }}", "Method 'UnicodeString::getDeprecated' is deprecated")
        assertLocalInspectionContains(TwigVariableDeprecatedInspection::class.java, "f.html.twig", "{{ ustring(text: 'aaa').dep<caret>recated }}", "Method 'UnicodeString::getDeprecated' is deprecated")
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
                "        /** @deprecated */\n" +
                "        public function getDeprecated(): static {}\n" +
                "    }\n" +
                "}\n"
        )
    }
}
