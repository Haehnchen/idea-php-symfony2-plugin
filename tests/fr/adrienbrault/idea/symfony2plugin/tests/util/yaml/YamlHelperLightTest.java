package fr.adrienbrault.idea.symfony2plugin.tests.util.yaml;


import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlHelper;
import fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlPsiElementFactory;
import fr.adrienbrault.idea.symfony2plugin.util.yaml.visitor.YamlTagVisitor;
import fr.adrienbrault.idea.symfony2plugin.util.yaml.visitor.YamlTagVisitorArguments;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.psi.YAMLKeyValue;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class YamlHelperLightTest extends SymfonyLightCodeInsightFixtureTestCase {

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlHelper#visitTagsOnServiceDefinition
     */
    public void testVisitTagsOnServiceDefinition() {

        YAMLKeyValue yamlKeyValue = YamlPsiElementFactory.createFromText(getProject(), YAMLKeyValue.class, "foo:\n" +
            "    tags:\n" +
            "       - { name: kernel.event_listener, event: eventName, method: methodName }\n" +
            "       - { name: kernel.event_listener2, event: eventName2, method: methodName2 }\n"
        );

        ListYamlTagVisitor visitor = new ListYamlTagVisitor();
        YamlHelper.visitTagsOnServiceDefinition(yamlKeyValue, visitor);

        assertEquals("kernel.event_listener", visitor.getItem(0).getName());
        assertEquals("eventName", visitor.getItem(0).getAttribute("event"));
        assertEquals("methodName", visitor.getItem(0).getAttribute("method"));

        assertEquals("kernel.event_listener2", visitor.getItem(1).getName());
        assertEquals("eventName2", visitor.getItem(1).getAttribute("event"));
        assertEquals("methodName2", visitor.getItem(1).getAttribute("method"));
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlHelper#visitTagsOnServiceDefinition
     */
    public void testVisitTagsOnServiceDefinitionWithQuote() {

        YAMLKeyValue yamlKeyValue = YamlPsiElementFactory.createFromText(getProject(), YAMLKeyValue.class, "foo:\n" +
            "    tags:\n" +
            "       - { name: 'kernel.event_listener', event: 'eventName', method: 'methodName' }\n"
        );

        ListYamlTagVisitor visitor = new ListYamlTagVisitor();
        YamlHelper.visitTagsOnServiceDefinition(yamlKeyValue, visitor);

        assertEquals("kernel.event_listener", visitor.getItem().getName());
        assertEquals("eventName", visitor.getItem().getAttribute("event"));
        assertEquals("methodName", visitor.getItem().getAttribute("method"));
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlHelper#visitTagsOnServiceDefinition
     */
    public void testVisitTagsOnServiceDefinitionWithDoubleQuote() {

        YAMLKeyValue yamlKeyValue = YamlPsiElementFactory.createFromText(getProject(), YAMLKeyValue.class, "foo:\n" +
            "    tags:\n" +
            "       - { name: \"kernel.event_listener\", event: \"eventName\", method: \"methodName\" }\n"
        );

        ListYamlTagVisitor visitor = new ListYamlTagVisitor();
        YamlHelper.visitTagsOnServiceDefinition(yamlKeyValue, visitor);

        assertEquals("kernel.event_listener", visitor.getItem().getName());
        assertEquals("eventName", visitor.getItem().getAttribute("event"));
        assertEquals("methodName", visitor.getItem().getAttribute("method"));
    }

    private static class ListYamlTagVisitor implements YamlTagVisitor {

        private List<YamlTagVisitorArguments> items = new ArrayList<YamlTagVisitorArguments>();

        @Override
        public void visit(@NotNull YamlTagVisitorArguments args) {
            items.add(args);
        }

        public YamlTagVisitorArguments getItem(int pos) {
            return items.get(pos);
        }

        public YamlTagVisitorArguments getItem() {
            return items.get(0);
        }
    }

}
