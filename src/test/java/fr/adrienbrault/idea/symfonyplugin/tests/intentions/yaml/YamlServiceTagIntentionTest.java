package fr.adrienbrault.idea.symfonyplugin.tests.intentions.yaml;

import fr.adrienbrault.idea.symfonyplugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import org.jetbrains.yaml.YAMLFileType;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 *
 * @see fr.adrienbrault.idea.symfonyplugin.intentions.yaml.YamlServiceTagIntention
 */
public class YamlServiceTagIntentionTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void setUp() throws Exception {
        super.setUp();
        myFixture.configureFromExistingVirtualFile(myFixture.copyFileToProject("classes.php"));
    }

    protected String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfonyplugin/tests/intentions/yaml/fixtures";
    }

    public void testTagIntentionIsAvailable() {
        assertIntentionIsAvailable(
            YAMLFileType.YML,
            "services:\n" +
                "    foo:\n" +
                "        class: Foo<caret>\\Bar",
            "Symfony: Add Tags"
        );

        assertIntentionIsAvailable(
            YAMLFileType.YML,
            "services:\n" +
                "    foo:\n" +
                "        arguments: [Foo<caret>\\Bar] ",
            "Symfony: Add Tags"
        );
    }
}
