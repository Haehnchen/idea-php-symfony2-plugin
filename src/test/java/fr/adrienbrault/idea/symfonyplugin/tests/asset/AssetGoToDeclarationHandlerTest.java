package fr.adrienbrault.idea.symfonyplugin.tests.asset;

import com.jetbrains.twig.TwigFileType;
import fr.adrienbrault.idea.symfonyplugin.tests.SymfonyLightCodeInsightFixtureTestCase;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class AssetGoToDeclarationHandlerTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void setUp() throws Exception {
        super.setUp();

        createDummyFiles(
            "web/assets/foo.css",
            "web/assets/foo.js",
            "web/foo.js"
        );
    }

    /**
     * @see fr.adrienbrault.idea.symfonyplugin.asset.AssetGoToDeclarationHandler
     */
    public void testGotoDeclarationTargetsTag() {
        if(System.getenv("PHPSTORM_ENV") != null) return;

        assertNavigationContainsFile(TwigFileType.INSTANCE, "" +
                "{% javascripts\n" +
                "    'assets/foo<caret>.js'\n" +
                "%}",
            "foo.js"
        );

        assertNavigationContainsFile(TwigFileType.INSTANCE, "" +
                "{% javascripts\n" +
                "    \"assets/foo<caret>.js\"\n" +
                "%}",
            "foo.js"
        );

        assertNavigationContainsFile(TwigFileType.INSTANCE, "" +
                "{% stylesheets\n" +
                "    'assets/foo<caret>.css'\n" +
                "%}",
            "foo.css"
        );

        assertNavigationContainsFile(TwigFileType.INSTANCE, "" +
                "{% stylesheets\n" +
                "    \"assets/foo<caret>.css\"\n" +
                "%}",
            "foo.css"
        );
    }

    /**
     * @see fr.adrienbrault.idea.symfonyplugin.asset.AssetGoToDeclarationHandler
     */
    public void testGotoDeclarationTargetsAsset() {
        if(System.getenv("PHPSTORM_ENV") != null) return;

        assertNavigationContainsFile(TwigFileType.INSTANCE, "{{ asset('assets/foo<caret>.css') }}", "foo.css");
        assertNavigationContainsFile(TwigFileType.INSTANCE, "{{ asset('assets/foo<caret>.js') }}", "foo.js");

        assertNavigationContainsFile(TwigFileType.INSTANCE, "{{ asset(\"assets/foo<caret>.css\") }}", "foo.css");
        assertNavigationContainsFile(TwigFileType.INSTANCE, "{{ asset(\"assets/foo<caret>.js\") }}", "foo.js");

        assertNavigationContainsFile(TwigFileType.INSTANCE, "{{ absolute_url(\"assets/foo<caret>.css\") }}", "foo.css");
        assertNavigationContainsFile(TwigFileType.INSTANCE, "{{ absolute_url('assets/foo<caret>.js') }}", "foo.js");

        assertNavigationContainsFile(TwigFileType.INSTANCE, "{{ asset('foo<caret>.js') }}", "foo.js");
    }
    /**
     * @see fr.adrienbrault.idea.symfonyplugin.asset.AssetGoToDeclarationHandler
     */
    public void testGotoDeclarationTargetsAssetInRoot() {
        if (System.getenv("PHPSTORM_ENV") != null) return;

        assertNavigationContainsFile(TwigFileType.INSTANCE, "{{ asset('foo<caret>.js') }}", "foo.js");
    }
}
