package fr.adrienbrault.idea.symfony2plugin.tests.config.php;

import com.intellij.patterns.PlatformPatterns;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.config.php.PhpConfigGotoDeclarationHandler
 */
public class PhpConfigGotoDeclarationHandlerTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void setUp() throws Exception {
        super.setUp();
        myFixture.copyFileToProject("ConfigLineMarkerProvider.php");
    }

    public String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/config/fixtures";
    }

    public void testRootConfigKeyNavigatesToConfigurationClass() {
        assertNavigationMatch("test.php", "<?php\n" +
            "return [\n" +
            "    'foobar<caret>_root' => ['foo' => 'bar'],\n" +
            "];",
            PlatformPatterns.psiElement(StringLiteralExpression.class)
        );
    }

    public void testConditionalConfigKeyNavigatesToConfigurationClass() {
        assertNavigationMatch("test.php", "<?php\n" +
            "return [\n" +
            "    'when@prod' => ['foobar<caret>_root' => ['foo' => 'bar']],\n" +
            "];",
            PlatformPatterns.psiElement(StringLiteralExpression.class)
        );
    }

    public void testUnknownConfigKeyDoesNotNavigate() {
        assertNavigationIsEmpty("test.php", "<?php\n" +
            "return [\n" +
            "    'unknown_xyz<caret>' => ['foo' => 'bar'],\n" +
            "];"
        );
    }

    public void testWhenKeyItselfDoesNotNavigate() {
        assertNavigationIsEmpty("test.php", "<?php\n" +
            "return [\n" +
            "    'when@p<caret>rod' => ['foobar_root' => []],\n" +
            "];"
        );
    }
}
