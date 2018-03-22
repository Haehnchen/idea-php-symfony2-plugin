package fr.adrienbrault.idea.symfony2plugin.tests.intentions.xml;

import com.intellij.ide.highlighter.XmlFileType;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

import java.io.File;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 *
 * @see fr.adrienbrault.idea.symfony2plugin.intentions.xml.XmlServiceTagIntention
 */
public class XmlServiceTagIntentionTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void setUp() throws Exception {
        super.setUp();
        myFixture.configureFromExistingVirtualFile(myFixture.copyFileToProject("classes.php"));
    }

    protected String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/intentions/xml/fixtures";
    }

    public void testTagIntentionIsAvailable() {
        assertIntentionIsAvailable(
            XmlFileType.INSTANCE,
            "<service><argument>%foo_parame<caret>ter_class%</argument></service>",
            "Symfony: Add Tags"
        );
    }
}
