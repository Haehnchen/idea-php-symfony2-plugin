package fr.adrienbrault.idea.symfony2plugin.tests.action.generator;

import com.intellij.ide.highlighter.XmlFileType;
import fr.adrienbrault.idea.symfony2plugin.action.generator.ServiceArgumentGenerateAction;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.action.generator.ServiceArgumentGenerateAction
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
