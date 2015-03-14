package fr.adrienbrault.idea.symfony2plugin.tests.templating.translation;

import com.jetbrains.twig.TwigFileType;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TwigTranslationNavigationTest extends TwigTranslationFixturesTest {

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.TwigHelper#getTransDefaultDomainPattern
     * @see fr.adrienbrault.idea.symfony2plugin.templating.TwigTemplateCompletionContributor
     */
    public void testTwigTransDefaultDomainDomainNavigation() {
        assertNavigationContainsFile(TwigFileType.INSTANCE, "{% trans_default_domain 'inter<caret>change' %}", "interchange.en.xlf");
        assertNavigationContainsFile(TwigFileType.INSTANCE, "{% trans_default_domain mess<caret>ages %}", "messages.de.yml");
    }

}
