package fr.adrienbrault.idea.symfony2plugin.tests.lang;

import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.psi.PsiElement;
import com.intellij.testFramework.fixtures.InjectionTestFixture;
import com.jetbrains.php.lang.PhpFileType;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import junit.framework.TestCase;
import org.junit.Ignore;

import static fr.adrienbrault.idea.symfony2plugin.lang.ParameterLanguageInjector.*;

@Deprecated
@Ignore("ParameterLanguageInjectorTest is deprecated")
public class ParameterLanguageInjectorTest extends SymfonyLightCodeInsightFixtureTestCase {

    private InjectionTestFixture injectionTestFixture;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        myFixture.copyFileToProject("classes.php");
        injectionTestFixture = new InjectionTestFixture(myFixture);
    }

    public String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/lang/fixtures";
    }

    public void skipTestCssLanguageInjections() {
        // skip as we dont have CSS module in >= 2020 test builds
        String base = "<?php $c = new \\Symfony\\Component\\DomCrawler\\Crawler();\n";
        assertInjectedLangAtCaret(PhpFileType.INSTANCE, base + "$c->filter('html > bo<caret>dy');", LANGUAGE_ID_CSS);
        assertInjectedLangAtCaret(PhpFileType.INSTANCE, base + "$c->filter('<caret>');", LANGUAGE_ID_CSS);
        assertInjectedLangAtCaret(PhpFileType.INSTANCE, base + "$c->children('html > bo<caret>dy');", LANGUAGE_ID_CSS);
        assertInjectedLangAtCaret(PhpFileType.INSTANCE, base + "$c->children('<caret>');", LANGUAGE_ID_CSS);

        base = "<?php $c = new \\Symfony\\Component\\CssSelector\\CssSelectorConverter();\n";
        assertInjectedLangAtCaret(PhpFileType.INSTANCE, base + "$c->toXPath('html > bo<caret>dy');", LANGUAGE_ID_CSS);
        assertInjectedLangAtCaret(PhpFileType.INSTANCE, base + "$c->toXPath('<caret>');", LANGUAGE_ID_CSS);
    }

    public void testXPathLanguageInjections() {
        String base = "<?php $c = new \\Symfony\\Component\\DomCrawler\\Crawler();\n";
        assertInjectedLangAtCaret(PhpFileType.INSTANCE, base + "$c->filterXPath('//dum<caret>my');", LANGUAGE_ID_XPATH);
        assertInjectedLangAtCaret(PhpFileType.INSTANCE, base + "$c->filterXPath('<caret>');", LANGUAGE_ID_XPATH);
        assertInjectedLangAtCaret(PhpFileType.INSTANCE, base + "$c->evaluate('//dum<caret>my');", LANGUAGE_ID_XPATH);
        assertInjectedLangAtCaret(PhpFileType.INSTANCE, base + "$c->evaluate('<caret>');", LANGUAGE_ID_XPATH);
    }

    public void testJsonLanguageInjections() {
        String base = "<?php \\Symfony\\Component\\HttpFoundation\\";
        assertInjectedLangAtCaret(PhpFileType.INSTANCE, base + "JsonResponse::fromJsonString('<caret>');", LANGUAGE_ID_JSON);
        assertInjectedLangAtCaret(PhpFileType.INSTANCE, base + "JsonResponse::fromJsonString('{\"foo\": <caret>}');", LANGUAGE_ID_JSON);

        base = "<?php $r = new \\Symfony\\Component\\HttpFoundation\\JsonResponse();\n";
        assertInjectedLangAtCaret(PhpFileType.INSTANCE, base + "$r->setJson('<caret>');", LANGUAGE_ID_JSON);
        assertInjectedLangAtCaret(PhpFileType.INSTANCE, base + "$r->setJson('{\"foo\": <caret>}');", LANGUAGE_ID_JSON);
    }

    public void testDqlLanguageInjections() {
        String base = "<?php $em = new \\Doctrine\\ORM\\EntityManager();\n";
        assertInjectedLangAtCaret(PhpFileType.INSTANCE, base + "$em->createQuery('SELECT b FR<caret>OM \\Foo\\Bar b');", LANGUAGE_ID_DQL);
        assertInjectedLangAtCaret(PhpFileType.INSTANCE, base + "$em->createQuery('<caret>');", LANGUAGE_ID_DQL);
        assertInjectedLangAtCaret(PhpFileType.INSTANCE, base + "$em->createQuery(<<caret><<AAA\n \nAAA\n);", null);
        assertInjectedFragmentText(PhpFileType.INSTANCE, base + "$em->createQuery(<<<AAA\nSELEC<caret>T\nAAA\n);", "SELECT");
        assertInjectedFragmentText(PhpFileType.INSTANCE, base + "$em->createQuery(<<<'AAA'\nSELEC<caret>T a\nAAA\n);", "SELECT a");
        base = "<?php $q = new \\Doctrine\\ORM\\Query();\n";
        assertInjectedLangAtCaret(PhpFileType.INSTANCE, base + "$q->setDQL('SELECT b FR<caret>OM \\Foo\\Bar b');", LANGUAGE_ID_DQL);
        assertInjectedLangAtCaret(PhpFileType.INSTANCE, base + "$q->setDQL('<caret>');", LANGUAGE_ID_DQL);

        assertInjectedLangAtCaret(PhpFileType.INSTANCE, "<?php $dql = \"SELECT b FR<caret>OM \\Foo\\Bar b\");", LANGUAGE_ID_DQL);
        assertInjectedLangAtCaret(PhpFileType.INSTANCE, "<?php $dql = \"<caret>\");", LANGUAGE_ID_DQL);
    }

    private void assertInjectedLangAtCaret(LanguageFileType fileType, String configureByText, String lang) {
        myFixture.configureByText(fileType, configureByText);
        injectionTestFixture.assertInjectedLangAtCaret(lang);
    }

    private void assertInjectedFragmentText(LanguageFileType fileType, String configureByText, String text) {
        myFixture.configureByText(fileType, configureByText);
        PsiElement injectedElement = injectionTestFixture.getInjectedElement();
        assertNotNull(injectedElement);
        TestCase.assertEquals(text, injectedElement.getContainingFile().getText());
    }
}