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
        myFixture.configureFromExistingVirtualFile(myFixture.copyFileToProject("services_array.php"));
        myFixture.configureFromExistingVirtualFile(myFixture.copyFileToProject("services_with_defaults.yaml"));
        myFixture.configureFromExistingVirtualFile(myFixture.copyFileToProject("services_fluent_chained.php"));
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

        ServiceInterface debugCommand = getFirstValue("twig.command.debug");
        assertContainsElements(debugCommand.getTags(), "console.command");
        assertTrue(debugCommand.isPublic());

        ServiceInterface decorated = getFirstValue("twig.service.decorated");
        assertEquals("twig.command.debug", decorated.getDecorates());
        assertEquals("twig.service.decorated.inner_custom", decorated.getDecorationInnerName());

        // auto-generated inner service from decorates
        assertIndexContains(ServicesDefinitionStubIndex.KEY, "twig.service.decorated.inner_custom");

        ServiceInterface withParent = getFirstValue("twig.service.with_parent");
        assertEquals("twig.command.debug", withParent.getParent());
        assertTrue(withParent.isLazy());
        assertTrue(withParent.isAutowire());
    }

    public void testThatServiceIdOfPhpArrayFileIsIndexed() {
        assertIndexContains(ServicesDefinitionStubIndex.KEY, "php_array.service");
        assertIndexContains(ServicesDefinitionStubIndex.KEY, "php_array.alias");

        ServiceInterface defaultsService = getFirstValue("datetime");
        assertEquals("DateTime", defaultsService.getClassName());
        assertTrue(defaultsService.isAutowire());
        assertFalse(defaultsService.isPublic());

        ServiceInterface configuredService = getFirstValue("php_array.service");
        assertEquals("DateTimeImmutable", configuredService.getClassName());
        assertContainsElements(configuredService.getTags(), "php_array_tag", "php_array_named_tag");

        ServiceInterface aliasService = getFirstValue("php_array.alias");
        assertEquals("php_array.service", aliasService.getAlias());

        ServiceInterface decoratedService = getFirstValue("php_array.decorated");
        assertEquals("ArrayObject", decoratedService.getClassName());
        assertEquals("php_array.service", decoratedService.getDecorates());
        assertEquals("php_array.decorated.inner_custom", decoratedService.getDecorationInnerName());

        ServiceInterface decoratedInner = getFirstValue("php_array.decorated.inner_custom");
        assertEquals("php_array.decorated.inner_custom", decoratedInner.getId());

        ServiceInterface resourceService = getFirstValue("php_array.resource");
        assertEquals("php_array.resource", resourceService.getId());
        assertEquals("php_array.resource", resourceService.getClassName());
        assertContainsElements(resourceService.getResource(), "../src/*", "../src2/*");
        assertContainsElements(resourceService.getExclude(), "../src/{Tests,Kernel.php}");
        assertFalse(resourceService.isAutowire());
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

    public void testThatAutowireIsPropagatedToResourceServices() {
        // Resource service should inherit autowire from _defaults
        ServiceInterface resourceService = getFirstValue("app\\service\\");
        assertTrue("Resource service should inherit autowire from _defaults", resourceService.isAutowire());

        // Resource service with explicit override should not be autowired
        ServiceInterface resourceServiceOverride = getFirstValue("app\\controller2\\");
        assertFalse("Resource service should use explicit autowire override", resourceServiceOverride.isAutowire());
    }

    /**
     * $container->parameters()->set() defines a container parameter, not a service.
     * Both the parameters and services configurators expose ->set(), so the parser
     * must walk the classRef chain to distinguish them.
     */
    public void testThatParametersSetIsNotIndexedAsService() {
        assertIndexNotContains(ServicesDefinitionStubIndex.KEY, "fluent.parameter");
        assertIndexNotContains(ServicesDefinitionStubIndex.KEY, "fluent.parameter2");
        assertIndexNotContains(ServicesDefinitionStubIndex.KEY, "fluent.parameter_variable");
    }

    /**
     * Services defined via a direct chain on $container->services() (no intermediate
     * $services variable) must be fully indexed with all chain attributes.
     */
    public void testThatDirectChainServicesAreIndexed() {
        assertIndexContains(ServicesDefinitionStubIndex.KEY, "fluent.chain.a");
        assertIndexContains(ServicesDefinitionStubIndex.KEY, "fluent.chain.b");
        assertIndexContains(ServicesDefinitionStubIndex.KEY, "fluent.chain.decorated");
        assertIndexContains(ServicesDefinitionStubIndex.KEY, "fluent.chain.last");
    }

    public void testThatDirectChainTagsAreIndexed() {
        ServiceInterface serviceA = getFirstValue("fluent.chain.a");
        assertContainsElements(serviceA.getTags(), "console.command");
        assertTrue(serviceA.isPublic());

        ServiceInterface serviceB = getFirstValue("fluent.chain.b");
        assertContainsElements(serviceB.getTags(), "app.tag_one", "app.tag_two");
        assertTrue(serviceB.isLazy());
        assertTrue(serviceB.isAutowire());
    }

    public void testThatDirectChainDecoratesIsIndexed() {
        ServiceInterface decorated = getFirstValue("fluent.chain.decorated");
        assertEquals("fluent.chain.a", decorated.getDecorates());
        assertEquals("fluent.chain.decorated.inner_custom", decorated.getDecorationInnerName());

        // auto-generated inner service from decorate()
        assertIndexContains(ServicesDefinitionStubIndex.KEY, "fluent.chain.decorated.inner_custom");
    }

    private ServiceInterface getFirstValue(@NotNull String key) {
        return FileBasedIndex.getInstance().getValues(ServicesDefinitionStubIndex.KEY, key, GlobalSearchScope.allScope(getProject())).get(0);
    }
}
