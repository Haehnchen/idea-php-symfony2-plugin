package fr.adrienbrault.idea.symfony2plugin.tests.util.yaml;

import com.intellij.psi.PsiElement;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlHelper;
import fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlPsiElementFactory;
import fr.adrienbrault.idea.symfony2plugin.util.yaml.visitor.YamlServiceTag;
import fr.adrienbrault.idea.symfony2plugin.util.yaml.visitor.YamlTagVisitor;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.YAMLFileType;
import org.jetbrains.yaml.psi.YAMLCompoundValue;
import org.jetbrains.yaml.psi.YAMLFile;
import org.jetbrains.yaml.psi.YAMLKeyValue;
import org.jetbrains.yaml.psi.impl.YAMLArrayImpl;
import org.jetbrains.yaml.psi.impl.YAMLHashImpl;

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

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlHelper#findServiceInContext
     */
    public void testFindServiceInContext() {
        assertEquals("foo", YamlHelper.findServiceInContext(myFixture.configureByText(YAMLFileType.YML, "" +
            "services:\n" +
            "  foo:\n" +
            "    tags:\n" +
            "      - { name: fo<caret>o}\n"
        ).findElementAt(myFixture.getCaretOffset())).getKeyText());

        assertEquals("foo", YamlHelper.findServiceInContext(myFixture.configureByText(YAMLFileType.YML, "" +
            "services:\n" +
            "  foo:\n" +
            "    class: fo<caret>o"
        ).findElementAt(myFixture.getCaretOffset())).getKeyText());
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlHelper#getYamlKeyValueAsString
     */
    public void testGetYamlKeyValueAsString() {

        String[] strings = {
            "{ name: routing.loader, method: foo }",
            "{ name: routing.loader, method: 'foo' }",
            "{ name: routing.loader, method: \"foo\" }",
        };

        for (String s : strings) {
            assertEquals("foo", YamlHelper.getYamlKeyValueAsString(
                YamlPsiElementFactory.createFromText(getProject(), YAMLHashImpl.class, s),
                "method"
            ));
        }
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlHelper#collectServiceTags
     */
    public void testCollectServiceTags() {

        YAMLKeyValue fromText = YamlPsiElementFactory.createFromText(getProject(), YAMLKeyValue.class, "" +
            "foo:\n" +
            "  tags:\n" +
            "    - { name: routing.loader, method: crossHint }\n" +
            "    - { name: routing.loader1, method: crossHint }\n"
        );

        assertNotNull(fromText);
        assertContainsElements(YamlHelper.collectServiceTags(fromText), "routing.loader", "routing.loader1");
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlHelper#getYamlArrayOnSequenceOrArrayElements
     */
    public void testGetYamlArrayOnSequenceOrArrayElements() {

        String[] strings = {
            "calls: [@foo, @bar] \n",
            "calls:\n  - @foo\n  - @bar\n",
        };

        for (String s : strings) {
            YAMLCompoundValue fromText = YamlPsiElementFactory.createFromText(getProject(), YAMLCompoundValue.class, s);
            assertNotNull(fromText);

            List<PsiElement> elements = YamlHelper.getYamlArrayOnSequenceOrArrayElements(fromText);
            assertNotNull(elements);

            String join = StringUtils.join(ContainerUtil.map(elements, new Function<PsiElement, String>() {
                @Override
                public String fun(PsiElement psiElement) {
                    return psiElement.getText();
                }
            }), ",");

            assertTrue(join.contains("foo"));
            assertTrue(join.contains("bar"));
        }
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlHelper#getYamlArrayValues
     */
    public void testGetYamlArrayValues() {
        YAMLArrayImpl fromText = YamlPsiElementFactory.createFromText(getProject(), YAMLArrayImpl.class, "['@twig', @twig, @twig]");
        assertEquals(3, YamlHelper.getYamlArrayValues(fromText).size());

        fromText = YamlPsiElementFactory.createFromText(getProject(), YAMLArrayImpl.class, "[@service, \"@service2\"]");
        assertEquals(2, YamlHelper.getYamlArrayValues(fromText).size());
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlHelper#getYamlArrayOnSequenceOrArrayElements
     */
    public void testGetYamlArrayOnSequenceOrArrayElementsForArray() {

        YAMLCompoundValue fromText = YamlPsiElementFactory.createFromText(getProject(), YAMLCompoundValue.class, "" +
            "calls: [@foo, @bar] \n"
        );

        assertNotNull(fromText);
        String join = StringUtils.join(ContainerUtil.map(YamlHelper.getYamlArrayOnSequenceOrArrayElements(fromText), new Function<PsiElement, String>() {
            @Override
            public String fun(PsiElement psiElement) {
                return psiElement.getText();
            }
        }), ",");

        assertTrue(join.contains("foo"));
        assertTrue(join.contains("bar"));
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlHelper#insertKeyIntoFile
     */
    public void testInsertKeyIntoFile() {
        YAMLFile yamlFile = (YAMLFile) myFixture.configureByText(YAMLFileType.YML, "" +
            "foo:\n" +
            "   bar:\n" +
            "       car: test"
        );

        YamlHelper.insertKeyIntoFile(yamlFile, "value", "foo", "bar", "apple");

        assertEquals("" +
            "foo:\n" +
            "   bar:\n" +
            "       car: test\n" +
            "       apple: value",
            yamlFile.getText()
        );
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlHelper#insertKeyIntoFile
     */
    public void testInsertKeyIntoFileOnRoot() {
        YAMLFile yamlFile = (YAMLFile) myFixture.configureByText(YAMLFileType.YML, "" +
            "foo:\n" +
            "   bar:\n" +
            "       car: test"
        );

        YamlHelper.insertKeyIntoFile(yamlFile, "value", "car", "bar", "apple");

        assertEquals("" +
                "foo:\n" +
                "   bar:\n" +
                "       car: test\n" +
                "car:\n" +
                "  bar:\n" +
                "    apple: value",
            yamlFile.getText()
        );
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlHelper#insertKeyIntoFile
     * TODO empty file
     */
    public void skipTestInsertKeyIntoEmptyFile() {
        YAMLFile yamlFile = (YAMLFile) myFixture.configureByText(YAMLFileType.YML, "");

        YamlHelper.insertKeyIntoFile(yamlFile, "value", "car", "bar", "apple");

        assertEquals("" +
                "foo:\n" +
                "   bar:\n" +
                "       car: test\n" +
                "car:\n" +
                "  bar:\n" +
                "    apple: value",
            yamlFile.getText()
        );
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlHelper#insertKeyIntoFile
     */
    public void testInsertKeyWithArrayValue() {
        YAMLFile yamlFile = (YAMLFile) myFixture.configureByText(YAMLFileType.YML, "" +
            "services:\n" +
            "   foo:\n" +
            "       car: test"
        );

        YAMLKeyValue yamlKeyValue = YamlPsiElementFactory.createFromText(getProject(), YAMLKeyValue.class, "" +
            "my_service:\n" +
            "   class: foo\n" +
            "   tag:\n" +
            "       - foo\n"
        );

        assertNotNull(yamlKeyValue);

        YamlHelper.insertKeyIntoFile(yamlFile, yamlKeyValue, "services");

        assertEquals("" +
                "services:\n" +
                "   foo:\n" +
                "       car: test\n" +
                "   my_service:\n" +
                "      class: foo\n" +
                "      tag:\n" +
                "          - foo",
            yamlFile.getText()
        );
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlHelper#insertKeyIntoFile
     */
    public void testInsertKeyValueWithMissingMainKeyInRoot() {
        YAMLFile yamlFile = (YAMLFile) myFixture.configureByText(YAMLFileType.YML, "foo: foo");

        YAMLKeyValue yamlKeyValue = YamlPsiElementFactory.createFromText(getProject(), YAMLKeyValue.class, "" +
            "my_service:\n" +
            "   class: foo\n" +
            "   tag: foo"
        );

        assertNotNull(yamlKeyValue);

        YamlHelper.insertKeyIntoFile(yamlFile, yamlKeyValue, "services");

        assertEquals("" +
                "foo: foo\n" +
                "services:\n" +
                "  my_service:\n" +
                "   class: foo\n" +
                "   tag: foo",
            yamlFile.getText()
        );
    }

    private static class ListYamlTagVisitor implements YamlTagVisitor {

        private List<YamlServiceTag> items = new ArrayList<YamlServiceTag>();

        @Override
        public void visit(@NotNull YamlServiceTag args) {
            items.add(args);
        }

        public YamlServiceTag getItem(int pos) {
            return items.get(pos);
        }

        public YamlServiceTag getItem() {
            return items.get(0);
        }
    }

}
