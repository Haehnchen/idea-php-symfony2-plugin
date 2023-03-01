package fr.adrienbrault.idea.symfony2plugin.tests.intentions.yaml.dic;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.containers.ContainerUtil;
import fr.adrienbrault.idea.symfony2plugin.intentions.yaml.dict.YamlCreateServiceArgumentsCallback;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import org.jetbrains.yaml.psi.YAMLFile;
import org.jetbrains.yaml.psi.YAMLKeyValue;

import java.util.Arrays;
import java.util.Collection;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.intentions.yaml.dict.YamlCreateServiceArgumentsCallback
 */
public class YamlCreateServiceArgumentsCallbackTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void testYamlServiceArgumentCreation() {
        YAMLFile yamlFile = (YAMLFile) myFixture.configureByText("test.yml", """
            services:
                foo:
                    class: Foo\\Foo
            """
        );

        Collection<YAMLKeyValue> yamlKeyValues = PsiTreeUtil.collectElementsOfType(yamlFile, YAMLKeyValue.class);

        YAMLKeyValue services = ContainerUtil.find(yamlKeyValues, new YamlUpdateArgumentServicesCallbackTest.YAMLKeyValueCondition("foo"));
        assertNotNull(services);

        final YamlCreateServiceArgumentsCallback callback = new YamlCreateServiceArgumentsCallback(services);

        CommandProcessor.getInstance().executeCommand(
            getProject(),
            () -> ApplicationManager.getApplication().runWriteAction(() -> callback.insert(Arrays.asList("foo", null, ""))),
            null,
            null
        );

        assertEquals("""
                services:
                    foo:
                      class: Foo\\Foo
                      arguments:
                        [ '@foo', '@?', '@?' ]
                """,
            yamlFile.getText()
        );
    }
}
