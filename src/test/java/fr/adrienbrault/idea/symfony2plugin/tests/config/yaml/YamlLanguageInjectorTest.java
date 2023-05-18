package fr.adrienbrault.idea.symfony2plugin.tests.config.yaml;

import com.intellij.testFramework.fixtures.InjectionTestFixture;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import org.jetbrains.annotations.NotNull;

public class YamlLanguageInjectorTest extends SymfonyLightCodeInsightFixtureTestCase {

    private InjectionTestFixture injectionTestFixture;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        injectionTestFixture = new InjectionTestFixture(myFixture);
    }

    public void testLanguageInjections() {
        assertExpressionLanguageIsInjectedAtCaret(
            "services.yaml",
            "services:\n" +
            "  App\\Service\\ExampleService:\n" +
            "    arguments:\n" +
            "      $example: '@=service(<caret>'\n"
        );
        
        assertExpressionLanguageIsInjectedAtCaret(
            "services.yaml",
            "services:\n" +
            "  App\\Service\\ExampleService:\n" +
            "    calls:\n" +
            "      - example: ['@=service(<caret>']\n"
        );

        assertExpressionLanguageIsInjectedAtCaret(
            "services.yaml",
            "services:\n" +
            "  App\\Service\\ExampleService:\n" +
            "    properties:\n" +
            "      example: '@=service(<caret>']\n"
        );

        assertExpressionLanguageIsInjectedAtCaret(
            "services.yaml",
            "services:\n" +
            "  App\\Service\\ExampleService:\n" +
            "    configurator: ['@=service(<caret>', 'configure']\n"
        );

        assertExpressionLanguageIsInjectedAtCaret(
            "routing.yaml",
            "app.contact:\n" +
            "  path: /contact\n" +
            "  controller: 'App\\Controller\\DefaultController::contact'\n" +
            "  condition: 'context.getMethod() in <caret>'\n"
        );
    }

    private void assertExpressionLanguageIsInjectedAtCaret(@NotNull String fileName, @NotNull String text) {
        myFixture.configureByText(fileName, text);
        injectionTestFixture.assertInjectedLangAtCaret("Symfony Expression Language");
    }
}
