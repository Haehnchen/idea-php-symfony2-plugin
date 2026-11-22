package fr.adrienbrault.idea.symfony2plugin.tests.javascript;

import com.intellij.patterns.ElementPattern;
import com.intellij.patterns.PlatformPatterns;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import fr.adrienbrault.idea.symfony2plugin.translation.TranslationKeyTargetFakePsiNavigationItem;

/**
 * @see fr.adrienbrault.idea.symfony2plugin.javascript.UxTranslatorGotoDeclarationHandler
 */
public class UxTranslatorGotoDeclarationHandlerTest extends SymfonyLightCodeInsightFixtureTestCase {
    private static final ElementPattern<?> TRANSLATION_TARGET = PlatformPatterns.psiElement(TranslationKeyTargetFakePsiNavigationItem.class);

    public void setUp() throws Exception {
        super.setUp();

        myFixture.copyFileToProject("messages.de.yml", "translations/messages.de.yml");
        myFixture.copyFileToProject("admin.de.yml", "translations/admin.de.yml");
    }

    public String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/javascript/fixtures";
    }

    public void testNavigationForBareTransCall() {
        assertNavigationMatch("test.js", "" +
                "import { trans } from './translator';\n" +
                "trans('app.he<caret>llo');",
            TRANSLATION_TARGET
        );

        assertNavigationMatch("test.js", "trans(\"app.he<caret>llo\");",
            TRANSLATION_TARGET
        );
    }

    public void testNavigationForQualifiedTransCall() {
        assertNavigationMatch("test.ts", "" +
                "const translator = createTranslator({});\n" +
                "translator.trans('app.he<caret>llo', { count: 1 });",
            TRANSLATION_TARGET
        );
    }

    public void testNoNavigationForUnknownKey() {
        assertNavigationIsEmpty("test.js", "trans('app.unkn<caret>own');");
    }

    public void testNoNavigationForUnrelatedFunction() {
        assertNavigationIsEmpty("test.js", "foo('app.he<caret>llo');");
    }

    public void testNoNavigationWhenKeyIsNotFirstArgument() {
        assertNavigationIsEmpty("test.js", "foo.trans(x, 'app.he<caret>llo');");
    }

    public void testNavigationWithExplicitDomain() {
        assertNavigationMatch("test.js", "trans('admin.ti<caret>tle', {}, 'admin');",
            TRANSLATION_TARGET
        );

        assertNavigationMatch("test.ts", "translator.trans('admin.ti<caret>tle', {}, 'admin', 'fr');",
            TRANSLATION_TARGET
        );
    }

    public void testNoNavigationWhenKeyIsInAnotherDomain() {
        assertNavigationIsEmpty("test.js", "trans('admin.ti<caret>tle');");
        assertNavigationIsEmpty("test.js", "trans('app.he<caret>llo', {}, 'admin');");
    }

    public void testNonLiteralDomainFallsBackToMessages() {
        assertNavigationMatch("test.js", "trans('app.he<caret>llo', {}, domain);",
            TRANSLATION_TARGET
        );
    }
}
