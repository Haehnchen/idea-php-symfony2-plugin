package fr.adrienbrault.idea.symfony2plugin.tests.config;

import com.intellij.psi.PsiElement;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlPsiElementFactory;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.config.ConfigLineMarkerProvider
 */
public class ConfigLineMarkerProviderTest extends SymfonyLightCodeInsightFixtureTestCase {
    public void setUp() throws Exception {
        super.setUp();
        myFixture.copyFileToProject("ConfigLineMarkerProvider.php");
    }

    public String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/config/fixtures";
    }

    public void testThatConfigRootProvidesLinemarker() {
        PsiElement yaml = YamlPsiElementFactory.createDummyFile(getProject(), "config.yml", "foobar_root:\n" +
            "    foo: ~"
        );

        assertLineMarker(yaml, new LineMarker.ToolTipEqualsAssert("Navigate to configuration"));
    }

    public void testThatConfigRootProvidesLinemarkerWithEnvironmentCheck() {
        PsiElement yaml = YamlPsiElementFactory.createDummyFile(getProject(), "config.yml", "" +
            "when@prod:\n" +
            "    foobar_root: ~\n"
        );

        assertLineMarker(yaml, new LineMarker.ToolTipEqualsAssert("Navigate to configuration"));
    }

    public void testThatNonConfigRootShouldNotProvideLinemarker() {
        PsiElement yaml = YamlPsiElementFactory.createDummyFile(getProject(), "foobar.yml", "foobar_root:\n" +
            "    foo: ~"
        );

        assertLineMarkerIsEmpty(yaml);
    }
}
