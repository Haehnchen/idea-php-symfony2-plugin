package fr.adrienbrault.idea.symfony2plugin.tests.stubs;

import com.intellij.openapi.util.Condition;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiManager;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.containers.ContainerUtil;
import fr.adrienbrault.idea.symfony2plugin.dic.ContainerService;
import fr.adrienbrault.idea.symfony2plugin.stubs.ServiceIndexUtil;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.psi.YAMLKeyValue;

import java.util.Collection;
import java.util.Map;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.stubs.ServiceIndexUtil
 */
public class ServiceIndexUtilTest extends SymfonyLightCodeInsightFixtureTestCase {
    private VirtualFile ymlVirtualFile;
    private VirtualFile xmlVirtualFile;

    public void setUp() throws Exception {
        super.setUp();

        myFixture.copyFileToProject("classes.php");

        ymlVirtualFile = myFixture.copyFileToProject("services.yml");
        xmlVirtualFile = myFixture.copyFileToProject("services.xml");
    }

    public String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/stubs/fixtures";
    }

    public void testFindServiceDefinitionsForStringInsideYaml() {
        assertNotNull(ContainerUtil.find(
            ServiceIndexUtil.findServiceDefinitions(getProject(), "foo.yml_id"),
            new MyYamlKeyValueCondition("foo.yml_id")
        ));
    }

    public void testFindServiceDefinitionsForStringInsideXml() {
        assertNotNull(ContainerUtil.find(
            ServiceIndexUtil.findServiceDefinitions(getProject(), "foo.xml_id"),
            new MyXmlTagCondition("foo.xml_id")
        ));
    }

    public void testFindServiceDefinitionsForStringInsideXmlUpperCase() {
        assertNotNull(ContainerUtil.find(
            ServiceIndexUtil.findServiceDefinitions(getProject(), "foo.xml_id.upper"),
            new MyXmlTagCondition("foo.xml_id.UPPER")
        ));
    }

    public void testFindServiceDefinitionsForPhpClassInsideYaml() {
        assertNotNull(ContainerUtil.find(
            ServiceIndexUtil.findServiceDefinitions(PhpElementsUtil.getClass(getProject(), "My\\Foo\\Service\\Targets")),
            new MyYamlKeyValueCondition("foo.yml_id")
        ));
    }

    public void testFindServiceDefinitionsForPhpClassInsideXml() {
        assertNotNull(ContainerUtil.find(
            ServiceIndexUtil.findServiceDefinitions(PhpElementsUtil.getClass(getProject(), "My\\Foo\\Service\\Targets")),
            new MyXmlTagCondition("foo.xml_id")
        ));
    }

    public void testFindServiceDefinitionsForPhpClassAsLazyInsideYml() {
        assertNotNull(ContainerUtil.find(
            ServiceIndexUtil.findServiceDefinitionsLazy(PhpElementsUtil.getClass(getProject(),"My\\Foo\\Service\\Targets")).getValue(),
            new MyYamlKeyValueCondition("foo.yml_id"))
        );
    }

    public void testFindServiceDefinitionsForPhpClassAsLazyInsideXml() {
        assertNotNull(ContainerUtil.find(
            ServiceIndexUtil.findServiceDefinitionsLazy(PhpElementsUtil.getClass(getProject(),"My\\Foo\\Service\\Targets")).getValue(),
            new MyXmlTagCondition("foo.xml_id"))
        );
    }

    public void testFindParameterDefinitionsInsideYml() {
        assertNotNull(ContainerUtil.find(
            ServiceIndexUtil.findParameterDefinitions(PsiManager.getInstance(getProject()).findFile(ymlVirtualFile), "foo_yaml_parameter"),
            new MyYamlKeyValueCondition("foo_yaml_parameter"))
        );
    }

    public void testFindParameterDefinitionsInsideXml() {
        assertNotNull(ContainerUtil.find(
            ServiceIndexUtil.findParameterDefinitions(PsiManager.getInstance(getProject()).findFile(xmlVirtualFile), "foo_xml_parameter"),
            new MyXmlTagCondition("foo_xml_parameter", "key"))
        );
    }

    public void testGetParentServices() {
        Map<String, Collection<ContainerService>> parentServices = ServiceIndexUtil.getParentServices(getProject());

        assertNotNull(parentServices.get("foo.yml_id.parent").stream().filter(service -> "foo.yml_id".equals(service.getName())).findFirst().orElse(null));
        assertNotNull(parentServices.get("foo.xml_id.parent").stream().filter(service -> "foo.xml_id".equals(service.getName())).findFirst().orElse(null));
    }

    private static class MyYamlKeyValueCondition implements Condition<PsiElement> {

        @NotNull
        private final String key;

        public MyYamlKeyValueCondition(@NotNull String key) {
            this.key = key;
        }

        @Override
        public boolean value(PsiElement psiElement) {
            return psiElement instanceof YAMLKeyValue && ((YAMLKeyValue) psiElement).getKeyText().equals(this.key);
        }
    }

    private static class MyXmlTagCondition implements Condition<PsiElement> {

        @NotNull
        private final String key;

        @NotNull
        private final String attr;

        public MyXmlTagCondition(@NotNull String key) {
            this(key, "id");
        }

        public MyXmlTagCondition(@NotNull String key, @NotNull String attr) {
            this.key = key;
            this.attr = attr;
        }

        @Override
        public boolean value(PsiElement psiElement) {
            return psiElement instanceof XmlTag && this.key.equals(((XmlTag) psiElement).getAttributeValue(this.attr));
        }
    }
}
