package fr.adrienbrault.idea.symfony2plugin.tests.templating.translation;

import com.intellij.patterns.PlatformPatterns;
import com.jetbrains.twig.TwigFileType;
import fr.adrienbrault.idea.symfony2plugin.templating.TwigPattern;
import fr.adrienbrault.idea.symfony2plugin.translation.TranslationKeyTargetFakePsiNavigationItem;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TwigTranslationNavigationTest extends TwigTranslationFixturesTestCase {

    /**
     * @see TwigPattern#getTransDefaultDomainPattern
     * @see fr.adrienbrault.idea.symfony2plugin.templating.TwigTemplateCompletionContributor
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

    public void testTwigTransHtmlNavigation() {
        assertNavigationMatch(TwigFileType.INSTANCE, "{{ 'auth_pro<caret>mpt'|trans_html({}, {}, 'auth') }}", PlatformPatterns.psiElement(TranslationKeyTargetFakePsiNavigationItem.class));
        assertNavigationContainsFile(TwigFileType.INSTANCE, "{{ 'auth_prompt'|trans_html({}, {}, 'au<caret>th') }}", "auth+intl-icu.en.yaml");
        assertNavigationMatch(TwigFileType.INSTANCE, "{{ 'auth_pro<caret>mpt'|trans_html(domain: 'auth', tags: {}) }}", PlatformPatterns.psiElement(TranslationKeyTargetFakePsiNavigationItem.class));
        assertNavigationContainsFile(TwigFileType.INSTANCE, "{{ 'auth_prompt'|trans_html(domain: 'au<caret>th', tags: {}) }}", "auth+intl-icu.en.yaml");
    }
}
