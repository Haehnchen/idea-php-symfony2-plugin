package fr.adrienbrault.idea.symfony2plugin.tests.templating.translation;

import com.intellij.patterns.PlatformPatterns;
import com.jetbrains.twig.TwigFileType;
import fr.adrienbrault.idea.symfony2plugin.templating.TwigPattern;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TwigTranslationNavigationTest extends TwigTranslationFixturesTestCase {

    /**
     * @see TwigPattern#getTransDefaultDomainPattern
     * @see fr.adrienbrault.idea.symfony2plugin.templating.TwigTemplateCompletionContributor
     */
    public void testTwigTransDefaultDomainDomainNavigation() {
        assertNavigationContainsFile(TwigFileType.INSTANCE, "{% trans_default_domain 'inter<caret>change' for _self %}", "interchange.en.xlf");
        assertNavigationContainsFile(TwigFileType.INSTANCE, "{% trans_default_domain 'inter<caret>change' %}", "interchange.en.xlf");
        assertNavigationContainsFile(TwigFileType.INSTANCE, "{% trans_default_domain \"inter<caret>change\" %}", "interchange.en.xlf");
        assertNavigationContainsFile(TwigFileType.INSTANCE, "{% trans_default_domain mess<caret>ages %}", "messages.de.yml");
    }

    public void testFileWideTransDefaultDomainKeyNavigation() {
        assertNavigationMatch(TwigFileType.INSTANCE, "{% trans_default_domain 'interchange' for _self %}{% embed 'card.html.twig' %}{{ 'xlf_weak.symfony.gr<caret>eat'|trans }}{% endembed %}", PlatformPatterns.psiElement().inFile(PlatformPatterns.psiFile().withName("interchange.en.xlf")));
    }

    /**
     * @see TwigPattern#getTranslationTokenTagFromPattern
     */
    public void testTranslationTokenTagFromCompletionNavigation() {
        assertNavigationContainsFile(TwigFileType.INSTANCE, "{% trans from \"inter<caret>change\" %}", "interchange.en.xlf");
        assertNavigationContainsFile(TwigFileType.INSTANCE, "{%    trans from \"inter<caret>change\" %}", "interchange.en.xlf");
        assertNavigationContainsFile(TwigFileType.INSTANCE, "{% \t   trans from \"inter<caret>change\" %}", "interchange.en.xlf");
        assertNavigationContainsFile(TwigFileType.INSTANCE, "{% transchoice from \"inter<caret>change\" %}", "interchange.en.xlf");
        assertNavigationContainsFile(TwigFileType.INSTANCE, "{% trans with {'%name%': 'Fabien'} from \"inter<caret>change\" %}", "interchange.en.xlf");
    }

}
