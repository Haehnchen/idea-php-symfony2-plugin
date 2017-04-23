package fr.adrienbrault.idea.symfony2plugin.tests.templating;

import com.intellij.patterns.PlatformPatterns;
import com.jetbrains.twig.TwigFileType;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

import java.io.File;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.templating.TranslationTagCompletionRegistrar
 */
public class TranslationTagCompletionRegistrarTest extends SymfonyLightCodeInsightFixtureTestCase {
    public void setUp() throws Exception {
        super.setUp();
        myFixture.copyFileToProject("app.de.yml", "Resources/translations/app.de.yml");
    }

    public String getTestDataPath() {
        return new File(this.getClass().getResource("fixtures").getFile()).getAbsolutePath();
    }

    public void testThatTransTagProvidesCompletionForTagValue() {
        assertCompletionContains(
            TwigFileType.INSTANCE,
            "{% trans from \"app\" %}<caret>{% endtrans %}",
            "symfony.great"
        );

        assertCompletionContains(
            TwigFileType.INSTANCE,
            "{% trans from \"app\" %}s<caret>{% endtrans %}",
            "symfony.great"
        );

        assertCompletionContains(
            TwigFileType.INSTANCE,
            "{% transchoice from \"app\" %}s<caret>{% endtrans %}",
            "symfony.great"
        );
    }

    public void testThatTransTagProvidesCompletionForTagValueWithMessagesScope() {
        assertCompletionContains(
            TwigFileType.INSTANCE,
            "{% trans_default_domain \"app\" %}{% trans %}<caret>{% endtrans %}",
            "symfony.great"
        );
    }

    public void testThatTransTagProvidesNavigationForTagValue() {
        assertNavigationMatch(
            TwigFileType.INSTANCE,
            "{% trans from \"app\" %}symfony<caret>.great{% endtrans %}",
            PlatformPatterns.psiElement()
        );
    }
}
