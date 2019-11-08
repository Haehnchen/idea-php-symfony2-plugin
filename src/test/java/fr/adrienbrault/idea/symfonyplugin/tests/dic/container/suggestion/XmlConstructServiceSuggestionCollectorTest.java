package fr.adrienbrault.idea.symfony2plugin.tests.dic.container.suggestion;

import com.intellij.ide.highlighter.XmlFileType;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.dic.container.suggestion.XmlCallServiceSuggestionCollector
 */
public class XmlConstructServiceSuggestionCollectorTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void setUp() throws Exception {
        super.setUp();
        myFixture.configureFromExistingVirtualFile(myFixture.copyFileToProject("services.xml"));
        myFixture.configureFromExistingVirtualFile(myFixture.copyFileToProject("classes.php"));
    }

    public String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/dic/container/suggestion/fixtures";
    }

    public void testServiceSuggestionForCallArguments() {
        assertCompletionLookupContainsPresentableItem(XmlFileType.INSTANCE, "" +
                "<services>" +
                "  <service class=\"Foo\\Bar\\Car\">\n" +
                "    <argument type=\"service\" id=\"<caret>\" />\n" +
                "  </service>" +
                "</services>",
            lookupElement -> "foo_bar_apple".equals(lookupElement.getItemText()) && lookupElement.isItemTextBold()
        );

        assertCompletionLookupContainsPresentableItem(XmlFileType.INSTANCE, "" +
                "<services>" +
                "  <service class=\"Foo\\Bar\\Car\">\n" +
                "    <argument/>\n" +
                "    <argument type=\"service\" id=\"<caret>\" />\n" +
                "  </service>" +
                "</services>",
            lookupElement -> "foo_bar_car".equals(lookupElement.getItemText()) && lookupElement.isItemTextBold()
        );

        assertCompletionLookupContainsPresentableItem(XmlFileType.INSTANCE, "" +
                "<services>" +
                "  <service id=\"Foo\\Bar\\Car\">\n" +
                "    <argument type=\"service\" id=\"<caret>\" />\n" +
                "  </service>" +
                "</services>",
            lookupElement -> "foo_bar_apple".equals(lookupElement.getItemText()) && lookupElement.isItemTextBold()
        );
    }
}
