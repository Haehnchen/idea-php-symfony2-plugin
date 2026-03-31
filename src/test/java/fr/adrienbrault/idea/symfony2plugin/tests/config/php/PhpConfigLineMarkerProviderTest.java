package fr.adrienbrault.idea.symfony2plugin.tests.config.php;

import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiFile;
import com.jetbrains.php.lang.psi.PhpPsiElementFactory;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.config.php.PhpConfigLineMarkerProvider
 */
public class PhpConfigLineMarkerProviderTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void setUp() throws Exception {
        super.setUp();
        myFixture.copyFileToProject("ConfigLineMarkerProvider.php");
    }

    public String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/config/fixtures";
    }

    private PsiFile createConfigPhpFile(String content) {
        return PhpPsiElementFactory.createPsiFileFromText(getProject(), content);
    }

    public void testRootConfigKeyProvidesLineMarker() {
        assertLineMarker(createConfigPhpFile("<?php\n" +
            "return [\n" +
            "    'foobar_root' => ['foo' => 'bar'],\n" +
            "];"
        ), new LineMarker.ToolTipEqualsAssert("Navigate to configuration"));
    }

    public void testConditionalRootConfigKeyProvidesLineMarker() {
        assertLineMarker(createConfigPhpFile("<?php\n" +
            "return [\n" +
            "    'when@prod' => ['foobar_root' => ['foo' => 'bar']],\n" +
            "];"
        ), new LineMarker.ToolTipEqualsAssert("Navigate to configuration"));
    }

    public void testWhenKeyItselfDoesNotProvideConfigMarker() {
        // 'when@prod' key itself must not get a config marker
        assertLineMarkerIsEmpty(createConfigPhpFile("<?php\n" +
            "return [\n" +
            "    'when@prod' => [],\n" +
            "];"
        ));
    }

    public void testNestedNonRootConfigKeyDoesNotProvideMarker() {
        // 'unknown_nested_key' is not a known root config key — no marker
        assertLineMarkerIsEmpty(createConfigPhpFile("<?php\n" +
            "return [\n" +
            "    'unknown_root_xyz' => ['foo' => 'bar'],\n" +
            "];"
        ));
    }

    public void testResourceImportProvidesLineMarker() {
        myFixture.addFileToProject("config/packages/legacy_config.php", "<?php return [];");

        PsiFile configFile = myFixture.addFileToProject("config/packages/imports_resource_test.php", "<?php\n" +
            "return [\n" +
            "    'imports' => [\n" +
            "        ['resource' => 'legacy_config.php'],\n" +
            "    ],\n" +
            "];"
        );
        myFixture.configureFromExistingVirtualFile(configFile.getVirtualFile());

        assertLineMarker(myFixture.getFile(), new LineMarker.ToolTipEqualsAssert("Navigate to resource"));
        assertLineMarker(myFixture.getFile(), new LineMarker.TargetAcceptsPattern("Navigate to resource",
            PlatformPatterns.psiFile().withName("legacy_config.php")
        ));
    }

    public void testConditionalImportsResourceProvidesLineMarker() {
        myFixture.addFileToProject("config/packages/legacy_config2.php", "<?php return [];");

        PsiFile configFile = myFixture.addFileToProject("config/packages/imports_when_resource_test.php", "<?php\n" +
            "return [\n" +
            "    'when@prod' => [\n" +
            "        'imports' => [\n" +
            "            ['resource' => 'legacy_config2.php'],\n" +
            "        ],\n" +
            "    ],\n" +
            "];"
        );
        myFixture.configureFromExistingVirtualFile(configFile.getVirtualFile());

        assertLineMarker(myFixture.getFile(), new LineMarker.ToolTipEqualsAssert("Navigate to resource"));
    }

    public void testNamedImportsEntryResourceDoesNotProvideLineMarker() {
        myFixture.addFileToProject("config/packages/legacy_config3.php", "<?php return [];");

        PsiFile configFile = myFixture.addFileToProject("config/packages/imports_named_resource_test.php", "<?php\n" +
            "return [\n" +
            "    'imports' => [\n" +
            "        'named' => ['resource' => 'legacy_config3.php'],\n" +
            "    ],\n" +
            "];"
        );
        myFixture.configureFromExistingVirtualFile(configFile.getVirtualFile());

        assertLineMarkerIsEmpty(myFixture.getFile());
    }
}
