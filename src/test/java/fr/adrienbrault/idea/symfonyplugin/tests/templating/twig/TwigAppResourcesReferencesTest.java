package fr.adrienbrault.idea.symfonyplugin.tests.templating.twig;

import com.jetbrains.twig.TwigFileType;
import fr.adrienbrault.idea.symfonyplugin.tests.SymfonyLightCodeInsightFixtureTestCase;

public class TwigAppResourcesReferencesTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void setUp() throws Exception {
        super.setUp();

        createDummyFiles(
            "app/Resources/views/base.html.twig",
            "app/Resources/views/Default/layout.html.twig"
        );

    }

    /**
     * @see fr.adrienbrault.idea.symfonyplugin.templating.TwigTemplateGoToDeclarationHandler
     */
    public void testTwigTemplatesInsideTwigFileCompletion() {
        if(System.getenv("PHPSTORM_ENV") != null) return;

        assertCompletionContains(TwigFileType.INSTANCE, "{% extends '<caret>' %}", "::base.html.twig", ":Default:layout.html.twig");
        assertCompletionContains(TwigFileType.INSTANCE, "{% extends \"<caret>\" %}", "::base.html.twig", ":Default:layout.html.twig");

        assertCompletionContains(TwigFileType.INSTANCE, "{% extends foo ? '<caret>' %}", "::base.html.twig", ":Default:layout.html.twig");
        assertCompletionContains(TwigFileType.INSTANCE, "{% extends foo ? \"<caret>\" %}", "::base.html.twig", ":Default:layout.html.twig");

        assertCompletionContains(TwigFileType.INSTANCE, "{% extends foo ? '' : '<caret>' %}", "::base.html.twig", ":Default:layout.html.twig");
        assertCompletionContains(TwigFileType.INSTANCE, "{% extends foo ? '' : \"<caret>\" %}", "::base.html.twig", ":Default:layout.html.twig");

        assertCompletionContains(TwigFileType.INSTANCE, "{% include \"<caret>\" %}", "::base.html.twig", ":Default:layout.html.twig");
        assertCompletionContains(TwigFileType.INSTANCE, "{% include '<caret>' %}", "::base.html.twig", ":Default:layout.html.twig");

        assertCompletionContains(TwigFileType.INSTANCE, "{% embed \"<caret>\" %}", "::base.html.twig", ":Default:layout.html.twig");
        assertCompletionContains(TwigFileType.INSTANCE, "{% embed '<caret>' %}", "::base.html.twig", ":Default:layout.html.twig");

        assertCompletionContains(TwigFileType.INSTANCE, "{% import \"<caret>\" as forms %}", "::base.html.twig", ":Default:layout.html.twig");
        assertCompletionContains(TwigFileType.INSTANCE, "{% import '<caret>' as forms %}", "::base.html.twig", ":Default:layout.html.twig");

        assertCompletionContains(TwigFileType.INSTANCE, "{{ source('<caret>') }}", "::base.html.twig", ":Default:layout.html.twig");
        assertCompletionContains(TwigFileType.INSTANCE, "{{ source(\"<caret>\") }}", "::base.html.twig", ":Default:layout.html.twig");

        assertCompletionContains(TwigFileType.INSTANCE, "{{ include('<caret>') }}", "::base.html.twig", ":Default:layout.html.twig");
        assertCompletionContains(TwigFileType.INSTANCE, "{{ include(\"<caret>\") }}", "::base.html.twig", ":Default:layout.html.twig");

        assertCompletionContains(TwigFileType.INSTANCE, "{% include ['<caret>'] %}", "::base.html.twig", ":Default:layout.html.twig");
        assertCompletionContains(TwigFileType.INSTANCE, "{% include [\"<caret>\"] %}", "::base.html.twig", ":Default:layout.html.twig");
        assertCompletionContains(TwigFileType.INSTANCE, "{% include [, '', ~ , '<caret>'] %}", "::base.html.twig", ":Default:layout.html.twig");
        assertCompletionNotContains(TwigFileType.INSTANCE, "{% include [ ~ '<caret>'] %}", "::base.html.twig");

        assertCompletionContains(TwigFileType.INSTANCE, "{% include foo ? '<caret>' %}", "::base.html.twig");
        assertCompletionContains(TwigFileType.INSTANCE, "{% include foo ? \"<caret>\" %}", "::base.html.twig");
        assertCompletionNotContains(TwigFileType.INSTANCE, "{% include foo ? ~ '<caret>' %}", "::base.html.twig");

        assertCompletionContains(TwigFileType.INSTANCE, "{% include foo ? 'foo' : '<caret>' %}", "::base.html.twig");
        assertCompletionNotContains(TwigFileType.INSTANCE, "{% include foo ? 'foo' : ~ '<caret>' %}", "::base.html.twig");

    }

    /**
     * @see fr.adrienbrault.idea.symfonyplugin.templating.TwigTemplateCompletionContributor
     */
    public void testTwigTemplatesInsideTwigFileNavigation() {
        if(System.getenv("PHPSTORM_ENV") != null) return;

        assertNavigationContainsFile(TwigFileType.INSTANCE, "{% extends '<caret>::base.html.twig' %}", "app/Resources/views/base.html.twig");
        assertNavigationContainsFile(TwigFileType.INSTANCE, "{% extends \"<caret>::base.html.twig\" %}", "app/Resources/views/base.html.twig");

        assertNavigationContainsFile(TwigFileType.INSTANCE, "{% include \"<caret>::base.html.twig\" %}", "app/Resources/views/base.html.twig");
        assertNavigationContainsFile(TwigFileType.INSTANCE, "{% include '<caret>::base.html.twig' %}", "app/Resources/views/base.html.twig");

        assertNavigationContainsFile(TwigFileType.INSTANCE, "{% include [\"<caret>::base.html.twig\"] %}", "app/Resources/views/base.html.twig");
        assertNavigationContainsFile(TwigFileType.INSTANCE, "{% include ['<caret>::base.html.twig'] %}", "app/Resources/views/base.html.twig");

        assertNavigationContainsFile(TwigFileType.INSTANCE, "{% embed \"<caret>::base.html.twig\" %}", "app/Resources/views/base.html.twig");
        assertNavigationContainsFile(TwigFileType.INSTANCE, "{% embed '<caret>::base.html.twig' %}", "app/Resources/views/base.html.twig");

        assertNavigationContainsFile(TwigFileType.INSTANCE, "{% import \"<caret>::base.html.twig\" as forms %}", "app/Resources/views/base.html.twig");
        assertNavigationContainsFile(TwigFileType.INSTANCE, "{% import '<caret>::base.html.twig' as forms %}", "app/Resources/views/base.html.twig");

        assertNavigationContainsFile(TwigFileType.INSTANCE, "{{ source('<caret>::base.html.twig') }}", "app/Resources/views/base.html.twig");
        assertNavigationContainsFile(TwigFileType.INSTANCE, "{{ source(\"<caret>::base.html.twig\") }}", "app/Resources/views/base.html.twig");

        assertNavigationContainsFile(TwigFileType.INSTANCE, "{{ include('<caret>::base.html.twig') }}", "app/Resources/views/base.html.twig");
        assertNavigationContainsFile(TwigFileType.INSTANCE, "{{ include(\"<caret>::base.html.twig\") }}", "app/Resources/views/base.html.twig");

        assertNavigationContainsFile(TwigFileType.INSTANCE, "{% extends foo ? '<caret>::base.html.twig' %}", "app/Resources/views/base.html.twig");
        assertNavigationContainsFile(TwigFileType.INSTANCE, "{% extends foo ? '' : '<caret>::base.html.twig' %}", "app/Resources/views/base.html.twig");

        assertNavigationContainsFile(TwigFileType.INSTANCE, "{% include foo ? '<caret>::base.html.twig' %}", "app/Resources/views/base.html.twig");
        assertNavigationContainsFile(TwigFileType.INSTANCE, "{% include foo ? '' : '<caret>::base.html.twig' %}", "app/Resources/views/base.html.twig");
    }

}
