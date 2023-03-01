package fr.adrienbrault.idea.symfony2plugin.tests.intentions.yaml.dic;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.util.Condition;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.containers.ContainerUtil;
import fr.adrienbrault.idea.symfony2plugin.intentions.yaml.dict.YamlUpdateArgumentServicesCallback;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.psi.YAMLFile;
import org.jetbrains.yaml.psi.YAMLKeyValue;

import java.util.Arrays;
import java.util.Collection;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.intentions.yaml.dict.YamlUpdateArgumentServicesCallback
 */
public class YamlUpdateArgumentServicesCallbackTest extends SymfonyLightCodeInsightFixtureTestCase {

    /**
     * @see YamlUpdateArgumentServicesCallback#insert(java.util.List)
     */
    public void testArgumentInsertOfArrayArguments() {
        YAMLFile yamlFile = (YAMLFile) myFixture.configureByText("test.yml","" +
            "services:\n" +
            "    foo:\n" +
            "        class: Foo\\Foo\n" +
            "        arguments: [ @service_container ]"
        );

        invokeInsert(yamlFile);

        assertEquals("" +
                "services:\n" +
                "    foo:\n" +
                "        class: Foo\\Foo\n" +
                "        arguments: [ @service_container, '@foo', '@bar' ]",
            yamlFile.getText()
        );
    }

    /**
     * @see YamlUpdateArgumentServicesCallback#insert(java.util.List)
     */
    public void testArgumentInsertOfSequenceArrayArguments() {
        YAMLFile yamlFile = (YAMLFile) myFixture.configureByText("test.yml","" +
            "services:\n" +
            "    foo:\n" +
            "        class: Foo\\Foo\n" +
            "        arguments:\n" +
            "           - @service_container"
        );

        invokeInsert(yamlFile);

        assertEquals("" +
                "services:\n" +
                "    foo:\n" +
                "        class: Foo\\Foo\n" +
                "        arguments:\n" +
                "           - @service_container\n" +
                "           - '@foo'\n" +
                "           - '@bar'",
            yamlFile.getText()
        );
    }

    private void invokeInsert(YAMLFile yamlFile) {
        Collection<YAMLKeyValue> yamlKeyValues = PsiTreeUtil.collectElementsOfType(yamlFile, YAMLKeyValue.class);

        final YamlUpdateArgumentServicesCallback callback = new YamlUpdateArgumentServicesCallback(
            getProject(),
            ContainerUtil.find(yamlKeyValues, new YAMLKeyValueCondition("arguments")),
            ContainerUtil.find(yamlKeyValues, new YAMLKeyValueCondition("foo"))
        );

        CommandProcessor.getInstance().executeCommand(
            getProject(),
            () -> ApplicationManager.getApplication().runWriteAction(() -> callback.insert(Arrays.asList("foo", "bar"))),
            null,
            null
        );
    }

    public static class YAMLKeyValueCondition implements Condition<YAMLKeyValue> {

        @NotNull
        private final String key;

        public YAMLKeyValueCondition(@NotNull String key) {
            this.key = key;
        }

        @Override
        public boolean value(YAMLKeyValue yamlKeyValue) {
            return key.equals(yamlKeyValue.getKeyText());
        }
    }
}
