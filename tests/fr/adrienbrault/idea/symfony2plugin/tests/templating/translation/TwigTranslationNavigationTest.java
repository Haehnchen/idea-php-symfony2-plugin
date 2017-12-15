package fr.adrienbrault.idea.symfony2plugin.tests.templating.translation;

import com.jetbrains.twig.TwigFileType;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigHelper;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TwigTranslationNavigationTest extends TwigTranslationFixturesTestCase {

    /**
     * @see TwigHelper#getTransDefaultDomainPattern
     * @see fr.adrienbrault.idea.symfony2plugin.templating.TwigTemplateCompletionContributor
     */
    public void testTwigTransDefaultDomainDomainNavigation() {
        assertNavigationContainsFile(TwigFileType.INSTANCE, "{% trans_default_domain 'inter<caret>change' %}", "interchange.en.xlf");
        assertNavigationContainsFile(TwigFileType.INSTANCE, "{% trans_default_domain \"inter<caret>change\" %}", "interchange.en.xlf");
        assertNavigationContainsFile(TwigFileType.INSTANCE, "{% trans_default_domain mess<caret>ages %}", "messages.de.yml");
    }

    /**
     * @see TwigHelper#getTranslationTokenTagFromPattern
     */
    public void testTranslationTokenTagFromCompletionNavigation() {
        assertNavigationContainsFile(TwigFileType.INSTANCE, "{% trans from \"inter<caret>change\" %}", "interchange.en.xlf");
        assertNavigationContainsFile(TwigFileType.INSTANCE, "{%    trans from \"inter<caret>change\" %}", "interchange.en.xlf");
        assertNavigationContainsFile(TwigFileType.INSTANCE, "{% \t   trans from \"inter<caret>change\" %}", "interchange.en.xlf");
        assertNavigationContainsFile(TwigFileType.INSTANCE, "{% transchoice from \"inter<caret>change\" %}", "interchange.en.xlf");
        assertNavigationContainsFile(TwigFileType.INSTANCE, "{% trans with {'%name%': 'Fabien'} from \"inter<caret>change\" %}", "interchange.en.xlf");
    }

}
