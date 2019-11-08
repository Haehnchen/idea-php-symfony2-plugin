package fr.adrienbrault.idea.symfony2plugin.tests.util.yaml;

import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.psi.PsiElement;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import com.jetbrains.php.lang.psi.elements.Parameter;
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
import org.jetbrains.yaml.psi.YAMLScalar;
import org.jetbrains.yaml.psi.impl.YAMLHashImpl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class YamlHelperLightTest extends SymfonyLightCodeInsightFixtureTestCase {
    public void setUp() throws Exception {
        super.setUp();
        myFixture.copyFileToProject("classes.php");
    }

    public String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/util/yaml/fixtures";
    }

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
    public void testVisitTagsOnServiceDefinitionForSymfony33TagsShortcut() {
        YAMLKeyValue yamlKeyValue = YamlPsiElementFactory.createFromText(getProject(), YAMLKeyValue.class, "foo:\n" +
            "    tags:\n" +
            "       - kernel.event_listener\n" +
            "       - kernel.event_listener2\n"
        );

        ListYamlTagVisitor visitor = new ListYamlTagVisitor();
        YamlHelper.visitTagsOnServiceDefinition(yamlKeyValue, visitor);

        assertEquals("kernel.event_listener", visitor.getItem(0).getName());
        assertEquals("kernel.event_listener", visitor.getItem(0).getAttribute("name"));
        assertEquals("kernel.event_listener2", visitor.getItem(1).getName());
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
     * @see fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlHelper#collectServiceTags
     */
    public void testCollectServiceTagsForSymfony33TagsShortcut() {

        YAMLKeyValue fromText = YamlPsiElementFactory.createFromText(getProject(), YAMLKeyValue.class, "" +
            "foo:\n" +
            "  tags:\n" +
            "    - routing.loader_tags_1\n" +
            "    - routing.loader_tags_2\n"
        );

        assertNotNull(fromText);
        Set<String> collection = YamlHelper.collectServiceTags(fromText);

        assertContainsElements(collection, "routing.loader_tags_1");
        assertContainsElements(collection, "routing.loader_tags_2");
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlHelper#collectServiceTags
     */
    public void testCollectServiceTagsForSymfony33TagsShortcutInline() {
        YAMLKeyValue fromText = YamlPsiElementFactory.createFromText(getProject(), YAMLKeyValue.class, "" +
            "foo:\n" +
            "  tags: [routing.loader_tags_3, routing.loader_tags_4]\n"
        );

        assertNotNull(fromText);
        Set<String> collection = YamlHelper.collectServiceTags(fromText);

        assertContainsElements(collection, "routing.loader_tags_3");
        assertContainsElements(collection, "routing.loader_tags_4");
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

        ApplicationInfo instance = ApplicationInfo.getInstance();
        String minorVersion = instance.getMinorVersionMainPart();
        if (instance.getMajorVersion().equals("2019") || (instance.getMajorVersion().equals("2018") && Integer.valueOf(minorVersion) >= 3)) {
            assertEquals("" +
                            "services:\n" +
                            "   foo:\n" +
                            "       car: test\n" +
                            "   my_service:\n" +
                            "     class: foo\n" +
                            "     tag:\n" +
                            "       - foo",
                    yamlFile.getText()
            );
        } else {
            assertEquals("" +
                            "services:\n" +
                            "   foo:\n" +
                            "       car: test\n" +
                            "   my_service:\n" +
                            "     class: foo\n" +
                            "     tag:\n" +
                            "     - foo",
                    yamlFile.getText()
            );
        }
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
                        "    class: foo\n" +
                        "    tag: foo",
                yamlFile.getText()
        );
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlHelper#visitServiceCall
     */
    public void testVisitServiceCall() {
        myFixture.configureByText(YAMLFileType.YML, "services:\n" +
            "    foobar:\n" +
            "       class: Foo\\Bar\n" +
            "       calls:\n" +
            "           - [ '<caret>' ]\n"
        );

        PsiElement psiElement = myFixture.getFile().findElementAt(myFixture.getCaretOffset());
        YAMLScalar parent = (YAMLScalar) psiElement.getParent();

        Collection<String> values = new ArrayList<>();
        YamlHelper.visitServiceCall(parent, values::add);

        assertContainsElements(values, "Foo\\Bar");
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlHelper#visitServiceCall
     */
    public void testVisitServiceCallForNamedServices() {
        myFixture.configureByText(YAMLFileType.YML, "services:\n" +
            "    Foo\\Bar:\n" +
            "       calls:\n" +
            "           - [ '<caret>' ]\n"
        );

        PsiElement psiElement = myFixture.getFile().findElementAt(myFixture.getCaretOffset());
        YAMLScalar parent = (YAMLScalar) psiElement.getParent();

        Collection<String> values = new ArrayList<>();
        YamlHelper.visitServiceCall(parent, values::add);

        assertContainsElements(values, "Foo\\Bar");
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlHelper#visitServiceCallArgument
     */
    public void testVisitServiceCallArgument() {
        myFixture.configureByText(YAMLFileType.YML, "services:\n" +
            "    foobar:\n" +
            "       class: Foo\\Bar\n" +
            "       calls:\n" +
            "           - [ 'setBar', [@f<caret>oo] ]\n"
        );

        PsiElement psiElement = myFixture.getFile().findElementAt(myFixture.getCaretOffset());
        YAMLScalar parent = (YAMLScalar) psiElement.getParent();

        Collection<String> values = new ArrayList<>();
        YamlHelper.visitServiceCallArgument(parent, parameterVisitor ->
            values.add(parameterVisitor.getClassName() + ":" + parameterVisitor.getMethod() + ":" + parameterVisitor.getParameterIndex())
        );

        assertContainsElements(values, "Foo\\Bar:setBar:0");
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlHelper#visitServiceCallArgument
     */
    public void testVisitServiceCallArgumentAsNamedService() {
        myFixture.configureByText(YAMLFileType.YML, "services:\n" +
            "    Foo\\Bar:\n" +
            "       calls:\n" +
            "           - [ 'setBar', [@f<caret>oo] ]\n"
        );

        PsiElement psiElement = myFixture.getFile().findElementAt(myFixture.getCaretOffset());
        YAMLScalar parent = (YAMLScalar) psiElement.getParent();

        Collection<String> values = new ArrayList<>();
        YamlHelper.visitServiceCallArgument(parent, parameterVisitor ->
            values.add(parameterVisitor.getClassName() + ":" + parameterVisitor.getMethod() + ":" + parameterVisitor.getParameterIndex())
        );

        assertContainsElements(values, "Foo\\Bar:setBar:0");
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlHelper#visitServiceCallArgumentMethodIndex
     */
    public void testVisitServiceCallArgumentMethodIndex() {
        myFixture.configureByText(YAMLFileType.YML, "services:\n" +
            "    foobar:\n" +
            "       class: Foo\\Bar\n" +
            "       calls:\n" +
            "           - [ 'setBar', [@f<caret>oo] ]\n"
        );

        PsiElement psiElement = myFixture.getFile().findElementAt(myFixture.getCaretOffset());
        YAMLScalar parent = (YAMLScalar) psiElement.getParent();

        Collection<Parameter> parameters = new ArrayList<>();
        YamlHelper.visitServiceCallArgumentMethodIndex(parent, parameters::add);

        assertNotNull(ContainerUtil.find(parameters, parameter -> "arg1".equals(parameter.getName())));
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlHelper#visitServiceCallArgumentMethodIndex
     */
    public void testVisitServiceCallArgumentMethodIndexForNamedServices() {
        myFixture.configureByText(YAMLFileType.YML, "services:\n" +
            "    Foo\\Bar:\n" +
            "       calls:\n" +
            "           - [ 'setBar', ['@foo', @f<caret>oo] ]\n"
        );

        PsiElement psiElement = myFixture.getFile().findElementAt(myFixture.getCaretOffset());
        YAMLScalar parent = (YAMLScalar) psiElement.getParent();

        Collection<Parameter> parameters = new ArrayList<>();
        YamlHelper.visitServiceCallArgumentMethodIndex(parent, parameters::add);

        assertNotNull(ContainerUtil.find(parameters, parameter -> "arg2".equals(parameter.getName())));
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlHelper#getIndentSpaceForFile
     */
    public void testGetIndentSpaceForFile() {
        assertEquals(2, getIndentForTextContent("parameters:\n  foo: ~"));
        assertEquals(4, getIndentForTextContent("parameters:\n    foo: ~"));
        assertEquals(4, getIndentForTextContent("parameters: ~"));

        assertEquals(4, getIndentForTextContent("parameters:\n" +
            "  # foobar" +
            "    foo: ~"
        ));
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlHelper#getServiceDefinitionClassFromTagMethod
     */
    public void testGetServiceDefinitionClassFromTagMethod() {
        myFixture.configureByText(YAMLFileType.YML, "" +
            "services:\n" +
            "   foobar:\n" +
            "       class: ClassName\\Foo\n" +
            "       tags:\n" +
            "           - { method: cross<caret>Hint }"
        );

        PsiElement psiElement = myFixture.getFile().findElementAt(myFixture.getCaretOffset());
        assertEquals("ClassName\\Foo", YamlHelper.getServiceDefinitionClassFromTagMethod(psiElement));

        myFixture.configureByText(YAMLFileType.YML, "" +
            "services:\n" +
            "   ClassName\\Foo:\n" +
            "       tags:\n" +
            "           - { method: cross<caret>Hint }"
        );

        psiElement = myFixture.getFile().findElementAt(myFixture.getCaretOffset());
        assertEquals("ClassName\\Foo", YamlHelper.getServiceDefinitionClassFromTagMethod(psiElement));
    }

    private int getIndentForTextContent(@NotNull String content) {
        return YamlHelper.getIndentSpaceForFile((YAMLFile) YamlPsiElementFactory.createDummyFile(
            getProject(),
            "foo.yml",
            content
        ));
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
