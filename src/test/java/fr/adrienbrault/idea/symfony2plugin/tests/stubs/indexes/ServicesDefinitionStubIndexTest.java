package fr.adrienbrault.idea.symfony2plugin.tests.stubs.indexes;

import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.indexing.FileBasedIndex;
import fr.adrienbrault.idea.symfony2plugin.dic.container.ServiceInterface;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.ServicesDefinitionStubIndex;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import org.jetbrains.annotations.NotNull;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.stubs.indexes.ServicesDefinitionStubIndex
 */
public class ServicesDefinitionStubIndexTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void setUp() throws Exception {
        super.setUp();

        myFixture.configureFromExistingVirtualFile(myFixture.copyFileToProject("services.xml"));
        myFixture.configureFromExistingVirtualFile(myFixture.copyFileToProject("services.yml"));
        myFixture.configureFromExistingVirtualFile(myFixture.copyFileToProject("services.yaml"));
        myFixture.configureFromExistingVirtualFile(myFixture.copyFileToProject("services.php"));
    }

    public String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/stubs/indexes/fixtures";
    }

    public void testThatServiceIdOfYamlFileIsIndexed() {
        assertIndexContains(ServicesDefinitionStubIndex.KEY, "foo.yml_id");
        assertIndexContains(ServicesDefinitionStubIndex.KEY, "foo.yaml_id");
        assertIndexContains(ServicesDefinitionStubIndex.KEY, "foo.yml_id.alias");

        assertEquals("AppBundle\\Controller\\DefaultController", getFirstValue("foo.yml_id").getClassName());

        assertEquals("AppBundle\\Controller\\DefaultController", getFirstValue("foo.yml_id.private").getClassName());
        assertFalse(getFirstValue("foo.yml_id.private").isPublic());
    }

    public void testThatServiceIdOfXmlFileIsIndexed() {
        assertIndexContains(ServicesDefinitionStubIndex.KEY, "foo.xml_id");

        ServiceInterface firstValue = getFirstValue("foo.xml_id");
        assertEquals("AppBundle\\Controller\\DefaultController", firstValue.getClassName());
        assertTrue(firstValue.isAutowire());

        ServiceInterface firstValue1 = getFirstValue("foo.xml_id.private");
        assertEquals("AppBundle\\Controller\\DefaultController", firstValue1.getClassName());
        assertFalse(firstValue1.isPublic());
        assertFalse(firstValue1.isAutowire());

        ServiceInterface firstValue2 = getFirstValue("foo.xml_id.invalid_autowire");
        assertFalse(firstValue2.isAutowire());

        ServiceInterface firstValue3 = getFirstValue("my\\fooclass");
        assertTrue(firstValue3.isAutowire());
    }

    public void testThatServiceIdOfPhpFileIsIndexed() {
        assertIndexContains(ServicesDefinitionStubIndex.KEY, "twig.command.debug");
        assertIndexContains(ServicesDefinitionStubIndex.KEY, "twig.command.debug_alias");
    }

    public void testServiceIdOfYmlWithDeprecatedShortcut() {
        assertTrue(getFirstValue("foo.yml_id.deprecated_tilde").isDeprecated());
        assertTrue(getFirstValue("foo.yml_id.deprecated_bool").isDeprecated());
    }

    public void testServiceIdOfIsConvertedToLower() {
        assertIndexContains(ServicesDefinitionStubIndex.KEY, "foo.xml_id.upper");
        assertEquals("foo.xml_id.UPPER", getFirstValue("foo.xml_id.upper").getId());
    }

    public void testThatIndexProcessStripsLeadingSlash() {
        assertEquals("AppBundle\\Controller\\DefaultController", getFirstValue("foo.xml_id.slash").getClassName());
        assertEquals("AppBundle\\Controller\\DefaultController", getFirstValue("foo.yml_id.slash").getClassName());
    }

    public void testThatDecoratedServiceProvidesOriginServiceAsInnerId() {
        assertEquals("espend.my_bar_foo.inner", getFirstValue("espend.my_bar_foo.inner").getId());
        assertEquals("espend.my_bar_customer_inner.inner_foo", getFirstValue("espend.my_bar_customer_inner.inner_foo").getId());
    }

    public void testThatResourceAndExcludeAttributesAreExtractedForYaml() {
        ServiceInterface firstValue = getFirstValue("app\\controller\\");

        assertEquals("App\\Controller\\", firstValue.getId());
        assertContainsElements(firstValue.getResource(), "../src/Controller");
        assertContainsElements(firstValue.getExclude(), "../src/{Entity,Tests}");
    }

    public void testThatResourceAndExcludeAttributesAreExtractedForXml() {
        ServiceInterface firstValue = getFirstValue("app\\xml\\");

        assertEquals("App\\Xml\\", firstValue.getId());
        assertContainsElements(firstValue.getResource(), "../src/*");
        assertContainsElements(firstValue.getExclude(), "../src/{DependencyInjection,Entity,Migrations,Tests,Kernel.php}");
        assertContainsElements(firstValue.getExclude(), "../src/foobar");
    }

    public void testThatTagAreInIndexForYaml() {
        ServiceInterface firstValue = getFirstValue("foo.tagged.yaml_type");
        assertContainsElements(firstValue.getTags(), "yaml_type_tag");

        firstValue = getFirstValue("foo.tagged.yaml_type2");
        assertContainsElements(firstValue.getTags(), "yaml_type_tag2");
        assertContainsElements(firstValue.getTags(), "yaml_type_tag21");

        firstValue = getFirstValue("foo.tagged.yaml_type3");
        assertContainsElements(firstValue.getTags(), "yaml_type_tag3");
    }

    public void testThatTagAreInIndexForXml() {
        ServiceInterface firstValue = getFirstValue("foo.tagged.xml_type");

        assertContainsElements(firstValue.getTags(), "xml_type_tag");
    }

    private ServiceInterface getFirstValue(@NotNull String key) {
        return FileBasedIndex.getInstance().getValues(ServicesDefinitionStubIndex.KEY, key, GlobalSearchScope.allScope(getProject())).get(0);
    }
}
