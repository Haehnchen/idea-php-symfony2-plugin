package fr.adrienbrault.idea.symfonyplugin.tests.templating.translation;

import com.jetbrains.twig.TwigFileType;
import fr.adrienbrault.idea.symfonyplugin.templating.TwigPattern;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TwigTranslationNavigationTest extends TwigTranslationFixturesTestCase {

    /**
     * @see TwigPattern#getTransDefaultDomainPattern
     * @see fr.adrienbrault.idea.symfonyplugin.templating.TwigTemplateCompletionContributor
     */
    public void testTwigTransDefaultDomainDomainNavigation() {
        assertNavigationContainsFile(TwigFileType.INSTANCE, "{% trans_default_domain 'inter<caret>change' %}", "interchange.en.xlf");
        assertNavigationContainsFile(TwigFileType.INSTANCE, "{% trans_default_domain \"inter<caret>change\" %}", "interchange.en.xlf");
        assertNavigationContainsFile(TwigFileType.INSTANCE, "{% trans_default_domain mess<caret>ages %}", "messages.de.yml");
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
