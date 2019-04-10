package fr.adrienbrault.idea.symfony2plugin.tests.lang;

import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.testFramework.fixtures.InjectionTestFixture;
import com.jetbrains.php.lang.PhpFileType;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

public class ParameterLanguageInjectorTest extends SymfonyLightCodeInsightFixtureTestCase {

    private InjectionTestFixture injectionTestFixture;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        injectionTestFixture = new InjectionTestFixture(myFixture);
        myFixture.copyFileToProject("classes.php");

    }

    public String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/lang/fixtures";
    }

    public void testCssLanguageInjections() {
        String base = "$c = new \\Symfony\\Component\\DomCrawler\\Crawler();\n";
        assertInjectedLangAtCaret(PhpFileType.INSTANCE, base + "$c->filter('html > bo<caret>dy');", "CSS");
        assertInjectedLangAtCaret(PhpFileType.INSTANCE, base + "$c->filter('<caret>');", "CSS");
        assertInjectedLangAtCaret(PhpFileType.INSTANCE, base + "$c->children('html > bo<caret>dy');", "CSS");
        assertInjectedLangAtCaret(PhpFileType.INSTANCE, base + "$c->children('<caret>');", "CSS");
    }

    public void testXPathLanguageInjections() {
        String base = "$c = new \\Symfony\\Component\\DomCrawler\\Crawler();\n";
        assertInjectedLangAtCaret(PhpFileType.INSTANCE, base + "$c->filterXPath('<caret>');", "XPath");
        assertInjectedLangAtCaret(PhpFileType.INSTANCE, base + "$c->evaluate('<caret>');", "XPath");

    }

    private void assertInjectedLangAtCaret(LanguageFileType fileType, String configureByText, String lang) {
        myFixture.configureByText(fileType, configureByText);
        injectionTestFixture.assertInjectedLangAtCaret(lang);
    }
}