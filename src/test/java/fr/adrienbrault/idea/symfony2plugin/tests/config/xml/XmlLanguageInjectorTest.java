package fr.adrienbrault.idea.symfony2plugin.tests.config.xml;

import com.intellij.testFramework.fixtures.InjectionTestFixture;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import org.jetbrains.annotations.NotNull;

public class XmlLanguageInjectorTest extends SymfonyLightCodeInsightFixtureTestCase {

    private InjectionTestFixture injectionTestFixture;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        injectionTestFixture = new InjectionTestFixture(myFixture);
    }

    public void testLanguageInjections() {
        assertExpressionLanguageIsInjectedAtCaret(
            "services.xml",
            "<?xml version=\"1.0\"?>\n" +
            "<container>\n" +
            "    <services>\n" +
            "        <service id=\"App\\Service\\ExampleService\">\n" +
            "            <argument type=\"expression\">container.hasParameter('some_param') ?<caret></argument>\n" +
            "        </service>\n" +
            "    </services>\n" +
            "</container>"
        );

        assertExpressionLanguageIsInjectedAtCaret(
            "routing.xml",
            "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n" +
            "<routes>\n" +
            "    <route id=\"contact\" path=\"/contact\" controller=\"App\\Controller\\DefaultController::contact\">\n" +
            "        <condition>context.getMethod() in <caret></condition>\n" +
            "    </route>\n" +
            "</routes>"
        );
    }

    private void assertExpressionLanguageIsInjectedAtCaret(@NotNull String fileName, @NotNull String text) {
        myFixture.configureByText(fileName, text);
        injectionTestFixture.assertInjectedLangAtCaret("Symfony Expression Language");
    }
}
