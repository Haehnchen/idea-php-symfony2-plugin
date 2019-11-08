package fr.adrienbrault.idea.symfonyplugin.tests.eventDispatcher;

import com.intellij.ide.highlighter.XmlFileType;
import com.jetbrains.php.lang.PhpFileType;
import fr.adrienbrault.idea.symfonyplugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import org.jetbrains.yaml.YAMLFileType;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class KernelEventListenerReferencesTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void setUp() throws Exception {
        super.setUp();
        myFixture.copyFileToProject("classes.php");
        myFixture.copyFileToProject("services.xml");
        myFixture.copyFileToProject("services.yml");
    }

    public String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfonyplugin/tests/eventDispatcher/fixtures";
    }

    /**
     * @see fr.adrienbrault.idea.symfonyplugin.util.completion.EventCompletionProvider
     * @see fr.adrienbrault.idea.symfonyplugin.config.dic.EventDispatcherEventReference
     */
    public void testKernelEventListenerTagOnIndexCompletion() {
        if(System.getenv("PHPSTORM_ENV") != null) return;

        assertCompletionContains(XmlFileType.INSTANCE, "<container>\n" +
                "  <services>\n" +
                "      <service>\n" +
                "          <tag name=\"kernel.event_listener\" event=\"<caret>\"/>\n" +
                "      </service>\n" +
                "  </services>\n" +
                "</container>",
            "xml_event", "yaml_event_1", "yaml_event_2", "yaml_event_3"
        );

        assertCompletionContains(YAMLFileType.YML, "services:\n" +
                "    foo_service:\n" +
                "        class: Foo\n" +
                "        tags:\n" +
                "            - { name: kernel.event_listener, event: <caret> }",
            "xml_event", "yaml_event_1", "yaml_event_2", "yaml_event_3"
        );

        assertCompletionContains(YAMLFileType.YML, "services:\n" +
                "    foo_service:\n" +
                "        class: Foo\n" +
                "        tags:\n" +
                "            - { name: kernel.event_listener, event: '<caret>' }",
            "xml_event", "yaml_event_1", "yaml_event_2", "yaml_event_3"
        );

        assertCompletionContains(YAMLFileType.YML, "services:\n" +
                "    foo_service:\n" +
                "        class: Foo\n" +
                "        tags:\n" +
                "            - { name: kernel.event_listener, event: \"<caret>\" }",
            "xml_event", "yaml_event_1", "yaml_event_2", "yaml_event_3"
        );

        assertCompletionNotContains(YAMLFileType.YML, "services:\n" +
                "    foo_service:\n" +
                "        class: Foo\n" +
                "        tags:\n" +
                "            - { name: kernel.event_listener, event: \"<caret>\" }",
            "xml_event_non"
        );

        assertCompletionContains(PhpFileType.INSTANCE, "<?php " +
                "/** @var $dispatcher \\Symfony\\Component\\EventDispatcher\\EventDispatcherInterface */\n" +
                "$dispatcher->dispatch('<caret>');" +
                "xml_event"
        );
    }
}
