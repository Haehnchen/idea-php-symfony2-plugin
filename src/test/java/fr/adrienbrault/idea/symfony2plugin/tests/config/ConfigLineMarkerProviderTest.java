package fr.adrienbrault.idea.symfony2plugin.tests.config;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlPsiElementFactory;
import org.jetbrains.yaml.YAMLFileType;

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

    public void testResourceImportRelativePathProvidesLineMarker() {
        myFixture.addFileToProject("config/packages/services.yml", "services:");

        PsiFile configFile = myFixture.addFileToProject("config/packages/config.yaml",
            "imports:\n" +
            "  - { resource: 'services.yml' }\n"
        );
        myFixture.configureFromExistingVirtualFile(configFile.getVirtualFile());

        assertLineMarker(myFixture.getFile(), new LineMarker.ToolTipEqualsAssert("Navigate to resource"));
    }

    public void testResourceImportRelativePathWithParentDirProvidesLineMarker() {
        myFixture.addFileToProject("config/services.yml", "services:");

        PsiFile configFile = myFixture.addFileToProject("config/packages/config.yaml",
            "imports:\n" +
            "  - { resource: '../services.yml' }\n"
        );
        myFixture.configureFromExistingVirtualFile(configFile.getVirtualFile());

        assertLineMarker(myFixture.getFile(), new LineMarker.ToolTipEqualsAssert("Navigate to resource"));
    }

    public void testResourceImportDoubleQuotedPathProvidesLineMarker() {
        myFixture.addFileToProject("config/packages/legacy.yaml", "");

        PsiFile configFile = myFixture.addFileToProject("config/packages/config.yaml",
            "imports:\n" +
            "  - { resource: \"legacy.yaml\" }\n"
        );
        myFixture.configureFromExistingVirtualFile(configFile.getVirtualFile());

        assertLineMarker(myFixture.getFile(), new LineMarker.ToolTipEqualsAssert("Navigate to resource"));
    }

    public void testResourceImportNonExistentTargetDoesNotProvideLineMarker() {
        PsiFile configFile = myFixture.addFileToProject("config/packages/config.yaml",
            "imports:\n" +
            "  - { resource: 'nonexistent.yaml' }\n"
        );
        myFixture.configureFromExistingVirtualFile(configFile.getVirtualFile());

        assertLineMarkerIsEmpty(myFixture.getFile());
    }
}
