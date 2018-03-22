package fr.adrienbrault.idea.symfony2plugin.tests.templating.twig;

import com.jetbrains.twig.TwigFileType;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

public class TwigAppResourcesReferences extends SymfonyLightCodeInsightFixtureTestCase {

    public void setUp() throws Exception {
        super.setUp();

        createDummyFiles(
            "app/Resources/views/base.html.twig",
            "app/Resources/views/Default/layout.html.twig"
        );

    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.templating.TwigTemplateGoToLocalDeclarationHandler
     */
    public void testTwigTemplatesInsideTwigFileCompletion() {
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
     * @see fr.adrienbrault.idea.symfony2plugin.templating.TwigTemplateCompletionContributor
     */
    public void testTwigTemplatesInsideTwigFileNavigation() {
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
