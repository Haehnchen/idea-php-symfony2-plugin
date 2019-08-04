package fr.adrienbrault.idea.symfony2plugin.tests.completion.yaml;

import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import org.jetbrains.yaml.YAMLFileType;

/**
 * @author Thomas Schulz <mail@king2500.net>
 *
 * @see fr.adrienbrault.idea.symfony2plugin.completion.yaml.YamlCompletionContributor
 */
public class YamlCompletionContributorTest extends SymfonyLightCodeInsightFixtureTestCase {

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
}
