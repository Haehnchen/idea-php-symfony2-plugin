package fr.adrienbrault.idea.symfonyplugin.tests.action.generator;

import com.intellij.ide.highlighter.XmlFileType;
import fr.adrienbrault.idea.symfonyplugin.action.generator.ServiceArgumentGenerateAction;
import fr.adrienbrault.idea.symfonyplugin.tests.SymfonyLightCodeInsightFixtureTestCase;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfonyplugin.action.generator.ServiceArgumentGenerateAction
 */
public class ServiceArgumentGenerateActionTest extends SymfonyLightCodeInsightFixtureTestCase {
    public void testActionAvailableForFileScope() {
        myFixture.configureByText(XmlFileType.INSTANCE, "" +
            "<services>" +
            "<service id=\"<caret>\"/>" +
            "</services>"
        );

        assertTrue(myFixture.testAction(new ServiceArgumentGenerateAction()).isEnabledAndVisible());
    }
}
