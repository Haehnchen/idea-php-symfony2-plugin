package fr.adrienbrault.idea.symfonyplugin.tests.dic.container.suggestion;

import fr.adrienbrault.idea.symfonyplugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import org.jetbrains.yaml.YAMLFileType;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfonyplugin.dic.container.suggestion.YamlConstructServiceSuggestionCollector
 */
public class YamlConstructServiceSuggestionCollectorTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void setUp() throws Exception {
        super.setUp();
        myFixture.configureFromExistingVirtualFile(myFixture.copyFileToProject("services.xml"));
        myFixture.configureFromExistingVirtualFile(myFixture.copyFileToProject("classes.php"));
    }

    public String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfonyplugin/tests/dic/container/suggestion/fixtures";
    }

    public void testConstructorArguments() {
        assertCompletionLookupContainsPresentableItem(YAMLFileType.YML, "" +
                "services:\n" +
                "   foo:\n" +
                "       class: Foo\\Bar\\Car\n" +
                "       arguments: [@<caret>]\n",
            lookupElement -> "foo_bar_apple".equals(lookupElement.getItemText()) && lookupElement.isItemTextBold()
        );

        assertCompletionLookupContainsPresentableItem(YAMLFileType.YML, "" +
                "services:\n" +
                "   foo:\n" +
                "       class: Foo\\Bar\\Car\n" +
                "       arguments:\n" +
                "           - @<caret>",
            lookupElement -> "foo_bar_apple".equals(lookupElement.getItemText()) && lookupElement.isItemTextBold()
        );

        assertCompletionLookupContainsPresentableItem(YAMLFileType.YML, "" +
                "services:\n" +
                "   foo:\n" +
                "       class: Foo\\Bar\\Car\n" +
                "       arguments:\n" +
                "           - @?\n" +
                "           - @<caret>\n",
            lookupElement -> "foo_bar_car".equals(lookupElement.getItemText()) && lookupElement.isItemTextBold()
        );

        assertCompletionLookupContainsPresentableItem(YAMLFileType.YML, "" +
                "services:\n" +
                "   foo:\n" +
                "       class: Foo\\Bar\\Car\n" +
                "       arguments:\n" +
                "           - @?\n" +
                "           - @<caret>\n",
            lookupElement -> "foo_bar_car".equals(lookupElement.getItemText()) && lookupElement.isItemTextBold()
        );
    }

}
