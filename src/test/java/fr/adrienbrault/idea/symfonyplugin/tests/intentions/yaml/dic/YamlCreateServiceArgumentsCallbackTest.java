package fr.adrienbrault.idea.symfonyplugin.tests.intentions.yaml.dic;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.containers.ContainerUtil;
import fr.adrienbrault.idea.symfonyplugin.intentions.yaml.dict.YamlCreateServiceArgumentsCallback;
import fr.adrienbrault.idea.symfonyplugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import org.jetbrains.yaml.YAMLElementGenerator;
import org.jetbrains.yaml.psi.YAMLFile;
import org.jetbrains.yaml.psi.YAMLKeyValue;

import java.util.Arrays;
import java.util.Collection;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfonyplugin.intentions.yaml.dict.YamlCreateServiceArgumentsCallback
 */
public class YamlCreateServiceArgumentsCallbackTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void testYamlServiceArgumentCreation() {
        YAMLFile yamlFile = YAMLElementGenerator.getInstance(getProject()).createDummyYamlWithText("" +
            "services:\n" +
            "    foo:\n" +
            "        class: Foo\\Foo\n"
        );

        Collection<YAMLKeyValue> yamlKeyValues = PsiTreeUtil.collectElementsOfType(yamlFile, YAMLKeyValue.class);

        YAMLKeyValue services = ContainerUtil.find(yamlKeyValues, new YamlUpdateArgumentServicesCallbackTest.YAMLKeyValueCondition("foo"));
        assertNotNull(services);

        final YamlCreateServiceArgumentsCallback callback = new YamlCreateServiceArgumentsCallback(services);

        CommandProcessor.getInstance().executeCommand(getProject(), new Runnable() {
            @Override
            public void run() {
                ApplicationManager.getApplication().runWriteAction(new Runnable() {
                    @Override
                    public void run() {
                        callback.insert(Arrays.asList("foo", null, ""));
                    }
                });

            }
        }, null, null);

        assertEquals("" +
                        "services:\n" +
                        "    foo:\n" +
                        "        class: Foo\\Foo\n" +
                        "        arguments:\n" +
                        "          ['@foo', '@?', '@?']\n",
                yamlFile.getText()
        );
    }
}
