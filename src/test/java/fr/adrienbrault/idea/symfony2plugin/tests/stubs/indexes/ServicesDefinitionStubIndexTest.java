package fr.adrienbrault.idea.symfony2plugin.tests.stubs.indexes;

import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.indexing.FileBasedIndexImpl;
import fr.adrienbrault.idea.symfony2plugin.dic.container.ServiceInterface;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.ServicesDefinitionStubIndex;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import org.jetbrains.annotations.NotNull;

import java.io.File;

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
        assertEquals(false, getFirstValue("foo.yml_id.private").isPublic());
    }

    public void testThatServiceIdOfXmlFileIsIndexed() {
        assertIndexContains(ServicesDefinitionStubIndex.KEY, "foo.xml_id");

        assertEquals("AppBundle\\Controller\\DefaultController", getFirstValue("foo.xml_id").getClassName());

        assertEquals("AppBundle\\Controller\\DefaultController", getFirstValue("foo.xml_id.private").getClassName());
        assertEquals(false, getFirstValue("foo.xml_id.private").isPublic());
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

    private ServiceInterface getFirstValue(@NotNull String key) {
        return FileBasedIndexImpl.getInstance().getValues(ServicesDefinitionStubIndex.KEY, key, GlobalSearchScope.allScope(getProject())).get(0);
    }
}
