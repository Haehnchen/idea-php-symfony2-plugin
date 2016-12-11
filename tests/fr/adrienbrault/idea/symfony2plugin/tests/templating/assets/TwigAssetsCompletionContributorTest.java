package fr.adrienbrault.idea.symfony2plugin.tests.templating.assets;

import com.jetbrains.twig.TwigFileType;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see com.jetbrains.twig.completion.TwigCompletionContributor
 */
public class TwigAssetsCompletionContributorTest extends SymfonyLightCodeInsightFixtureTestCase {
    
    public void setUp() throws Exception {
        super.setUp();
        if (System.getenv("PHPSTORM_ENV") != null) return;
        
        createDummyFiles(
            "web/assets/foo.css",
            "web/assets/foo.less",
            "web/assets/foo.sass",
            "web/assets/foo.scss",
            "web/assets/foo.js",
            "web/assets/foo.dart",
            "web/assets/foo.coffee",
            "web/assets/foo.jpg",
            "web/assets/foo.png",
            "web/assets/foo.gif"
        );

    }

    public void testTwigAssetFunctionCompletion() {
        if (System.getenv("PHPSTORM_ENV") != null) return;

        assertCompletionContains(TwigFileType.INSTANCE, "{{ asset('<caret>') }}", "assets/foo.css", "assets/foo.js", "assets/foo.less", "assets/foo.coffee");
        assertCompletionResultEquals(TwigFileType.INSTANCE, "<script src=\"assets/foo.coffee<caret>\"></script>", "<script src=\"{{ asset('assets/foo.coffee') }}\"></script>");
    }

    public void testTwigAssetsTagCompletion() {
        if (System.getenv("PHPSTORM_ENV") != null) return;

        assertCompletionContains(TwigFileType.INSTANCE, "{% stylesheets '<caret>' %}{% endstylesheets %}", "assets/foo.css", "assets/foo.less", "assets/foo.sass", "assets/foo.scss");
        assertCompletionNotContains(TwigFileType.INSTANCE, "{% stylesheets '<caret>' %}{% endstylesheets %}", "assets/foo.js", "assets/foo.dart", "assets/foo.coffee");

        assertCompletionContains(TwigFileType.INSTANCE, "{% javascripts '<caret>' %}{% endjavascripts %}", "assets/foo.js", "assets/foo.dart", "assets/foo.coffee");
        assertCompletionNotContains(TwigFileType.INSTANCE, "{% javascripts '<caret>' %}{% endjavascripts %}", "assets/foo.css", "assets/foo.less", "assets/foo.sass", "assets/foo.scss");
    }

    public void testTwigAssetImageFunctionCompletion() {
        if (System.getenv("PHPSTORM_ENV") != null) return;

        assertCompletionResultEquals(TwigFileType.INSTANCE, "<img src=\"assets/foo.pn<caret>\">", "<img src=\"{{ asset('assets/foo.png') }}\">");
    }

    public void testTwigAbsoluteUrlFunctionCompletion() {
        if (System.getenv("PHPSTORM_ENV") != null) return;

        assertCompletionContains(TwigFileType.INSTANCE, "{{ absolute_url('<caret>') }}", "assets/foo.css", "assets/foo.js", "assets/foo.less", "assets/foo.coffee");
    }
}
