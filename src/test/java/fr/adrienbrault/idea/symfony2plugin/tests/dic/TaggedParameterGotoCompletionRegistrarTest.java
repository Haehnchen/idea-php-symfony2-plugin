package fr.adrienbrault.idea.symfony2plugin.tests.dic;

import com.intellij.ide.highlighter.XmlFileType;
import com.intellij.patterns.PlatformPatterns;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import org.jetbrains.yaml.YAMLFileType;

import java.io.File;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.dic.TaggedParameterGotoCompletionRegistrar
 */
public class TaggedParameterGotoCompletionRegistrarTest extends SymfonyLightCodeInsightFixtureTestCase {
    public void setUp() throws Exception {
        super.setUp();
        myFixture.copyFileToProject("TaggedParameterGotoCompletionRegistrar.php");
        myFixture.copyFileToProject("TaggedParameterGotoCompletionRegistrar.yml");
    }

    public String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/dic/fixtures";
    }

    public void testThatYamlTaggedParameterProvidesNavigation() {
        assertNavigationMatch(
            YAMLFileType.YML,
            "arguments: [!tagged foo<caret>bar]",
            PlatformPatterns.psiElement()
        );
    }

    public void testThatYamlTaggedParameterProvidesCompletion() {
        assertCompletionContains(
            YAMLFileType.YML,
            "arguments: [!tagged <caret>]",
            "foobar"
        );
    }

    public void testThatXmlTaggedParameterProvidesNavigation() {
        assertNavigationMatch(
            XmlFileType.INSTANCE,
            "<services>\n" +
                "    <service>\n" +
                "        <argument type=\"tagged\" tag=\"foo<caret>bar\" />\n" +
                "    </service>\n" +
                "</services>",
            PlatformPatterns.psiElement()
        );
    }

    public void testThatXmlTaggedParameterProvidesCompletion() {
        assertCompletionContains(
            XmlFileType.INSTANCE,
            "<services>\n" +
                "    <service>\n" +
                "        <argument type=\"tagged\" tag=\"<caret>\" />\n" +
                "    </service>\n" +
                "</services>",
            "foobar"
        );
    }
}
