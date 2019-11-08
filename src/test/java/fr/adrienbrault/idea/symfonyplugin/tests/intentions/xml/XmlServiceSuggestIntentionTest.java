package fr.adrienbrault.idea.symfonyplugin.tests.intentions.xml;

import com.intellij.ide.highlighter.XmlFileType;
import fr.adrienbrault.idea.symfonyplugin.tests.SymfonyLightCodeInsightFixtureTestCase;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 *
 * @see fr.adrienbrault.idea.symfonyplugin.intentions.xml.XmlServiceSuggestIntention
 */
public class XmlServiceSuggestIntentionTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void testTagIntentionIsAvailable() {
        assertIntentionIsAvailable(
            XmlFileType.INSTANCE,
            "<service><argument>%foo_parame<caret>ter_class%</argument></service>",
            "Symfony: Suggest Service"
        );

        assertIntentionIsAvailable(
            XmlFileType.INSTANCE,
            "<service><argument <caret>type=\"service\"></service>",
            "Symfony: Suggest Service"
        );

        assertIntentionIsAvailable(
            XmlFileType.INSTANCE,
            "<service><arg<caret>ument/></service>",
            "Symfony: Suggest Service"
        );
    }
}
