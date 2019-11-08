package fr.adrienbrault.idea.symfonyplugin.tests.completion.yaml;

import com.intellij.codeInsight.completion.CompletionType;
import fr.adrienbrault.idea.symfonyplugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import org.jetbrains.yaml.YAMLFileType;

import java.util.Arrays;
import java.util.List;

/**
 * @author Thomas Schulz <mail@king2500.net>
 *
 * @see fr.adrienbrault.idea.symfonyplugin.completion.yaml.YamlCompletionContributor
 */
public class YamlCompletionContributorTest extends SymfonyLightCodeInsightFixtureTestCase {

    public String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfonyplugin/tests/completion/yaml/fixtures";
    }

    public void testTagsCompletionContainsStandardTags() {
        assertCompletionContains(YAMLFileType.YML, "" +
                "root:\n" +
                "    key: <caret>\n",
            "!!binary", "!!float", "!!str"
        );
        assertCompletionContains(YAMLFileType.YML, "" +
                "root:\n" +
                "    key: !<caret>\n",
            "!!binary", "!!float", "!!str"
        );
        assertCompletionContains(YAMLFileType.YML, "" +
                "root:\n" +
                "    key: !!<caret>\n",
            "!!binary", "!!float", "!!str"
        );
    }

    public void testTagsCompletionNotContainsCustomTags() {
        assertCompletionNotContains(YAMLFileType.YML, "" +
                "root:\n" +
                "    key: <caret>\n",
            "!php/const", "!php/object", "!tagged"
        );
    }

    public void testAllTagsCompletionAt3rdInvocation() {
        assertCompletion3rdInvocationContains("" +
                "root:\n" +
                "    key: <caret>\n",
            "!php/const", "!php/object", "!!binary", "!!float", "!!str"
        );
    }

    public void testTagsCompletionContainsPhpConstTagInsideConfigAndServices() {
        assertCompletionContains("config.yaml", "" +
                "root:\n" +
                "    key: <caret>\n",
            "!php/const"
        );
        assertCompletionContains("services.yaml", "" +
                "services:\n" +
                "    my_service:\n" +
                "        key: <caret>\n",
            "!php/const"
        );
    }

    public void testTagsCompletionNotContainsPhpObjectTagInsideServices() {
        assertCompletionNotContains("services.yaml", "" +
                "services:\n" +
                "    my_service:\n" +
                "        key: <caret>\n",
            "!php/object"
        );
        assertCompletion3rdInvocationNotContains("services.yaml", "" +
                "services:\n" +
                "    my_service:\n" +
                "        key: <caret>\n",
            "!php/object"
        );
    }

    public void testServiceArgumentCompletion() {
        assertCompletionContains("services.yaml", "" +
                "services:\n" +
                "    my_service:\n" +
                "        arguments: [<caret>]\n",
            "!tagged", "!tagged_locator", "!service", "!service_locator", "!iterator"
        );
    }

    public void testServiceArgumentCompletionForSymfony32() {
        myFixture.copyFileToProject("Symfony32.php");
        assertCompletionNotContains("services.yaml", "" +
                "services:\n" +
                "    my_service:\n" +
                "        arguments: [<caret>]\n",
            "!tagged", "!tagged_locator", "!tagged_iterator", "!service", "!service_locator", "!iterator"
        );
    }

    public void testServiceArgumentCompletionForSymfony33() {
        myFixture.copyFileToProject("Symfony33.php");
        assertCompletionContains("services.yaml", "" +
                "services:\n" +
                "    my_service:\n" +
                "        arguments: [<caret>]\n",
            "!iterator", "!service"
        );
        assertCompletionNotContains("services.yaml", "" +
                "services:\n" +
                "    my_service:\n" +
                "        arguments: [<caret>]\n",
            "!tagged", "!tagged_locator", "!tagged_iterator", "!service_locator"
        );
    }

    public void testServiceArgumentCompletionForSymfony34() {
        myFixture.copyFileToProject("Symfony34.php");
        assertCompletionContains("services.yaml", "" +
                "services:\n" +
                "    my_service:\n" +
                "        arguments: [<caret>]\n",
            "!tagged", "!iterator", "!service"
        );
        assertCompletionNotContains("services.yaml", "" +
                "services:\n" +
                "    my_service:\n" +
                "        arguments: [<caret>]\n",
            "!tagged_locator", "!tagged_iterator", "!service_locator"
        );
    }

    public void testServiceArgumentCompletionForSymfony41() {
        myFixture.copyFileToProject("Symfony41.php");
        assertCompletionNotContains("services.yaml", "" +
                "services:\n" +
                "    my_service:\n" +
                "        arguments: [<caret>]\n",
            "!service_locator"
        );
    }

    public void testServiceArgumentCompletionForSymfony42() {
        myFixture.copyFileToProject("Symfony42.php");
        assertCompletionContains("services.yaml", "" +
                "services:\n" +
                "    my_service:\n" +
                "        arguments: [<caret>]\n",
            "!service_locator", "!tagged", "!iterator", "!service"
        );
        assertCompletionNotContains("services.yaml", "" +
                "services:\n" +
                "    my_service:\n" +
                "        arguments: [<caret>]\n",
            "!tagged_locator", "!tagged_iterator"
        );
    }

    public void testServiceArgumentCompletionForSymfony43() {
        myFixture.copyFileToProject("Symfony43.php");
        assertCompletionContains("services.yaml", "" +
                "services:\n" +
                "    my_service:\n" +
                "        arguments: [<caret>]\n",
            "!tagged_locator", "!service_locator", "!tagged", "!iterator", "!service"
        );
        assertCompletionNotContains("services.yaml", "" +
                "services:\n" +
                "    my_service:\n" +
                "        arguments: [<caret>]\n",
            "!tagged_iterator"
        );
    }

    public void testServiceArgumentCompletionForSymfony44() {
        myFixture.copyFileToProject("Symfony44.php");
        assertCompletionContains("services.yaml", "" +
                "services:\n" +
                "    my_service:\n" +
                "        arguments: [<caret>]\n",
            "!tagged_iterator", "!tagged_locator", "!service_locator", "!iterator", "!service"
        );
        assertCompletionNotContains("services.yaml", "" +
                "services:\n" +
                "    my_service:\n" +
                "        arguments: [<caret>]\n",
            "!tagged"
        );
    }

    public void testServiceArgumentCompletionForSymfony50() {
        myFixture.copyFileToProject("Symfony50.php");
        assertCompletionContains("services.yaml", "" +
                "services:\n" +
                "    my_service:\n" +
                "        arguments: [<caret>]\n",
            "!tagged_iterator", "!tagged_locator", "!service_locator", "!iterator", "!service"
        );
        assertCompletionNotContains("services.yaml", "" +
                "services:\n" +
                "    my_service:\n" +
                "        arguments: [<caret>]\n",
            "!tagged"
        );
    }

    public void testKeywordsCompletion() {
        assertCompletionContains(YAMLFileType.YML, "" +
                "root:\n" +
                "    key: <caret>\n",
            "true", ".inf"
        );
        assertCompletionContains(YAMLFileType.YML, "" +
                "root:\n" +
                "    key: <caret>",
            "true", ".inf"
        );
        assertCompletionContains(YAMLFileType.YML, "" +
                "root:\n" +
                "    key: tr<caret>",
            "true"
        );
//        assertCompletionContains(YAMLFileType.YML, "" +
//                "root:\n" +
//                "    key: .i<caret>",
//            ".inf"
//        );
    }

    public void testKeywordsCompletionInsideArray() {
        assertCompletionContains(YAMLFileType.YML, "" +
                "root:\n" +
                "    key: [<caret>]\n",
            "true", ".inf"
        );
        assertCompletionContains(YAMLFileType.YML, "" +
                "root:\n" +
                "    key: [FOO, <caret>]\n",
            "true", ".inf"
        );
        assertCompletionContains(YAMLFileType.YML, "" +
                "root:\n" +
                "    key: [FOO, tr<caret>]\n",
            "true"
        );
    }

    public void testKeywordsCompletionInsideSequence() {
        assertCompletionContains(YAMLFileType.YML, "" +
                "root:\n" +
                "    key:\n" +
                "        - <caret>\n",
            "true", ".inf"
        );
        assertCompletionContains(YAMLFileType.YML, "" +
                "root:\n" +
                "    key:\n" +
                "        - tr<caret>\n",
            "true"
        );
    }

    public void testKeywordsCompletionInsideMapping() {
        assertCompletionContains(YAMLFileType.YML, "" +
                "root:\n" +
                "    key: { foo: <caret> }\n",
            "true", ".inf"
        );
        assertCompletionContains(YAMLFileType.YML, "" +
                "root:\n" +
                "    key: { foo: tr<caret> }\n",
            "true"
        );
    }

    public void testThatKeywordsAreNotCompletedAfterYamlTag() {
        assertCompletionNotContains(YAMLFileType.YML, "" +
                "root:\n" +
                "    key: !mytag <caret>\n" +
                "    foo: bar",
            "true"
        );
        assertCompletionNotContains(YAMLFileType.YML, "" +
                "root:\n" +
                "    key: !mytag <caret>",
            "true"
        );
    }

    public void testThatKeywordsAreNotCompletedInNewLine() {
        assertCompletionNotContains(YAMLFileType.YML, "" +
                "root:\n" +
                "    <caret>" +
                "    foo: bar\n",
            "true"
        );
        assertCompletionNotContains(YAMLFileType.YML, "" +
                "root:\n" +
                "    foo: bar\n" +
                "<caret>",
            "true"
        );
        assertCompletionNotContains(YAMLFileType.YML, "" +
                "root:\n" +
                "    foo: bar\n" +
                "    <caret>",
            "true"
        );
    }

    public void testThatKeywordsAreNotCompletedInsideStringLiteral() {
        assertCompletionNotContains(YAMLFileType.YML, "" +
                "root:\n" +
                "    foo: '<caret>'\n" ,
            "true"
        );
    }

    private void assertCompletion3rdInvocationContains(String configureByText, String... lookupStrings) {
        myFixture.configureByText(YAMLFileType.YML, configureByText);
        myFixture.complete(CompletionType.BASIC, 3);

        if(lookupStrings.length == 0) {
            fail("No lookup element given");
        }

        List<String> lookupElements = myFixture.getLookupElementStrings();
        if(lookupElements == null || lookupElements.size() == 0) {
            fail(String.format("failed that empty completion contains %s", Arrays.toString(lookupStrings)));
        }

        for (String s : lookupStrings) {
            if(!lookupElements.contains(s)) {
                fail(String.format("failed that completion contains %s in %s", s, lookupElements.toString()));
            }
        }
    }

    public void assertCompletion3rdInvocationNotContains(String filename, String configureByText, String... lookupStrings) {
        myFixture.configureByText(filename, configureByText);
        myFixture.complete(CompletionType.BASIC, 3);

        List<String> lookupElementStrings = myFixture.getLookupElementStrings();
        assertNotNull(lookupElementStrings);
        assertFalse(lookupElementStrings.containsAll(Arrays.asList(lookupStrings)));
    }
}
