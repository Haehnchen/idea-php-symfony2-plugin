package fr.adrienbrault.idea.symfony2plugin.tests.asset;

import com.jetbrains.twig.TwigFileType;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyTempCodeInsightFixtureTestCase;

import java.io.File;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class AssetGoToDeclarationHandlerTest extends SymfonyTempCodeInsightFixtureTestCase {
    public void setUp() throws Exception {
        super.setUp();

        createFiles(
            "web/assets/foo.css",
            "web/assets/foo.js",
            "web/foo.js"
        );
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.asset.AssetGoToDeclarationHandler
     */
    public void testGotoDeclarationTargetsTag() {
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
     * @see fr.adrienbrault.idea.symfony2plugin.asset.AssetGoToDeclarationHandler
     */
    public void testGotoDeclarationTargetsAsset() {
        assertNavigationContainsFile(TwigFileType.INSTANCE, "{{ asset('assets/foo<caret>.css') }}", "foo.css");
        assertNavigationContainsFile(TwigFileType.INSTANCE, "{{ asset('assets/foo<caret>.js') }}", "foo.js");

        assertNavigationContainsFile(TwigFileType.INSTANCE, "{{ asset(\"assets/foo<caret>.css\") }}", "foo.css");
        assertNavigationContainsFile(TwigFileType.INSTANCE, "{{ asset(\"assets/foo<caret>.js\") }}", "foo.js");

        assertNavigationContainsFile(TwigFileType.INSTANCE, "{{ absolute_url(\"assets/foo<caret>.css\") }}", "foo.css");
        assertNavigationContainsFile(TwigFileType.INSTANCE, "{{ absolute_url('assets/foo<caret>.js') }}", "foo.js");

        assertNavigationContainsFile(TwigFileType.INSTANCE, "{{ asset('foo<caret>.js') }}", "foo.js");
    }
    /**
     * @see fr.adrienbrault.idea.symfony2plugin.asset.AssetGoToDeclarationHandler
     */
    public void testGotoDeclarationTargetsAssetInRoot() {
        assertNavigationContainsFile(TwigFileType.INSTANCE, "{{ asset('foo<caret>.js') }}", "foo.js");
    }
}
