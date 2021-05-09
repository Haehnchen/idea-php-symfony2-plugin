package fr.adrienbrault.idea.symfony2plugin.tests.dic.container.util;

import com.google.gson.Gson;
import com.intellij.openapi.util.Condition;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.Consumer;
import com.intellij.util.containers.ContainerUtil;
import com.jetbrains.php.lang.psi.elements.Parameter;
import fr.adrienbrault.idea.symfony2plugin.dic.container.ServiceInterface;
import fr.adrienbrault.idea.symfony2plugin.dic.container.ServiceSerializable;
import fr.adrienbrault.idea.symfony2plugin.dic.container.dict.ServiceTypeHint;
import fr.adrienbrault.idea.symfony2plugin.dic.container.util.ServiceContainerUtil;
import fr.adrienbrault.idea.symfony2plugin.stubs.ContainerCollectionResolver;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.psi.YAMLFile;
import org.jetbrains.yaml.psi.YAMLScalar;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.function.Predicate;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 *
 * @see fr.adrienbrault.idea.symfony2plugin.dic.container.util.ServiceContainerUtil
 */
public class ServiceContainerUtilTest extends SymfonyLightCodeInsightFixtureTestCase {

    private PsiFile xmlFile;
    private PsiFile ymlFile;

    public void setUp() throws Exception {
        super.setUp();
        this.xmlFile = myFixture.configureByFile("services.xml");
        this.ymlFile = myFixture.configureByFile("services.yml");

        myFixture.configureByFile("usage.services.xml");
        myFixture.configureByFile("usage1.services.xml");

        myFixture.configureByFile("classes.php");
    }

    public String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/dic/container/util/fixtures";
    }

    public void testDefaults() {
        for (PsiFile psiFile : new PsiFile[]{xmlFile, ymlFile}) {
            ServiceInterface bar = ContainerUtil.find(ServiceContainerUtil.getServicesInFile(psiFile), MyStringServiceInterfaceCondition.create("defaults"));
            assertEquals("defaults", bar.getId());
            assertEquals("DateTime", bar.getClassName());

            assertEquals(false, bar.isAbstract());
            assertEquals(false, bar.isAutowire());
            assertEquals(false, bar.isDeprecated());
            assertEquals(false, bar.isLazy());
            assertEquals(true, bar.isPublic());

            assertNull(bar.getDecorates());
            assertNull(bar.getDecorationInnerName());
            assertNull(bar.getParent());
            assertNull(bar.getAlias());
        }
    }

    public void testDecoratorPatternCustomLanguageUnderscoreKeys() {
        for (PsiFile psiFile : new PsiFile[]{xmlFile, ymlFile}) {
            ServiceInterface bar = ContainerUtil.find(ServiceContainerUtil.getServicesInFile(psiFile), MyStringServiceInterfaceCondition.create("bar"));
            assertEquals("bar", bar.getId());
            assertEquals("stdClass", bar.getClassName());
            assertEquals("foo", bar.getDecorates());
            assertEquals("bar.wooz", bar.getDecorationInnerName());
            assertEquals(false, bar.isPublic());
        }
    }

    public void testNonDefaults() {
        for (PsiFile psiFile : new PsiFile[]{xmlFile, ymlFile}) {
            ServiceInterface bar = ContainerUtil.find(ServiceContainerUtil.getServicesInFile(psiFile), MyStringServiceInterfaceCondition.create("non.defaults"));

            assertEquals(true, bar.isAbstract());
            assertEquals(true, bar.isAutowire());
            assertEquals(true, bar.isDeprecated());
            assertEquals(true, bar.isLazy());
            assertEquals(false, bar.isPublic());

            assertEquals("foo", bar.getDecorates());
            assertEquals("foo", bar.getDecorationInnerName());
            assertEquals("foo", bar.getParent());
            assertEquals("foo", bar.getAlias());
        }
    }

    public void testUpperToLower() {
        for (PsiFile psiFile : new PsiFile[]{xmlFile, ymlFile}) {
            ServiceInterface bar = ContainerUtil.find(ServiceContainerUtil.getServicesInFile(psiFile), serviceInterface ->
                serviceInterface.getId().equalsIgnoreCase("bar.UPPER")
            );

            assertEquals("bar.UPPER", bar.getId());
        }
    }

    public void testNonDefaultValuesAreSerialized() {
        for (PsiFile psiFile : new PsiFile[]{xmlFile, ymlFile}) {
            ServiceInterface bar = ContainerUtil.find(ServiceContainerUtil.getServicesInFile(psiFile), MyStringServiceInterfaceCondition.create("non.defaults"));
            assertEquals(
                "{\"id\":\"non.defaults\",\"class\":\"DateTime\",\"public\":false,\"lazy\":true,\"abstract\":true,\"autowire\":true,\"deprecated\":true,\"alias\":\"foo\",\"decorates\":\"foo\",\"decoration_inner_name\":\"foo\",\"parent\":\"foo\",\"resource\":[],\"exclude\":[],\"tags\":[]}",
                new Gson().toJson(bar)
            );
        }
    }

    public void testPhpServicesAreInIndex() {
        PsiFile phpFile = myFixture.configureByFile("services.php");

        Collection<ServiceSerializable> servicesInFile = ServiceContainerUtil.getServicesInFile(phpFile);

        ServiceSerializable translationWarmer = servicesInFile.stream().filter(s -> "translator.default".equals(s.getId())).findFirst().get();
        assertEquals(
            "Symfony\\Bundle\\FrameworkBundle\\Translation\\Translator",
            translationWarmer.getClassName()
        );

        ServiceSerializable translationReader = servicesInFile.stream().filter(s -> "Symfony\\Contracts\\Translation\\TranslatorInterface".equals(s.getId())).findFirst().get();
        assertEquals(
            "translator",
            translationReader.getAlias()
        );
    }

    public void testThatDefaultValueAreNullAndNotSerialized() {
        for (PsiFile psiFile : new PsiFile[]{xmlFile, ymlFile}) {
            ServiceInterface bar = ContainerUtil.find(ServiceContainerUtil.getServicesInFile(psiFile), MyStringServiceInterfaceCondition.create("defaults"));
            assertEquals(
                "{\"id\":\"defaults\",\"class\":\"DateTime\",\"resource\":[],\"exclude\":[],\"tags\":[]}",
                new Gson().toJson(bar)
            );
        }
    }

    public void testYmlDeprecatedAsTilde() {
        ServiceInterface bar = ContainerUtil.find(ServiceContainerUtil.getServicesInFile(ymlFile), MyStringServiceInterfaceCondition.create("bar.deprecated"));
        assertEquals(true, bar.isDeprecated());
    }

    public void testYmlAliasDefinitionAsInline() {
        assertEquals("bar", ContainerUtil.find(ServiceContainerUtil.getServicesInFile(ymlFile), MyStringServiceInterfaceCondition.create("alias.inline_1")).getAlias());
        assertEquals("bar", ContainerUtil.find(ServiceContainerUtil.getServicesInFile(ymlFile), MyStringServiceInterfaceCondition.create("alias.inline_2")).getAlias());
        assertEquals("bar", ContainerUtil.find(ServiceContainerUtil.getServicesInFile(ymlFile), MyStringServiceInterfaceCondition.create("alias.inline_3")).getAlias());
        assertNull(ContainerUtil.find(ServiceContainerUtil.getServicesInFile(ymlFile), MyStringServiceInterfaceCondition.create("alias.inline_4")).getAlias());
    }

    public void testServiceWithoutClassMustUseIdAsClass() {
        for (PsiFile psiFile : new PsiFile[]{xmlFile, ymlFile}) {
            ServiceInterface bar = ContainerUtil.find(ServiceContainerUtil.getServicesInFile(psiFile), MyStringServiceInterfaceCondition.create("My\\Class\\Id\\First"));
            assertEquals("My\\Class\\Id\\First", bar.getClassName());

            assertNull(ContainerUtil.find(ServiceContainerUtil.getServicesInFile(psiFile), MyStringServiceInterfaceCondition.create("my_abstract_without_class")).getClassName());
            assertNull(ContainerUtil.find(ServiceContainerUtil.getServicesInFile(psiFile), MyStringServiceInterfaceCondition.create("my_alias_without_class")).getClassName());
        }
    }

    public void testGetServiceUsage() {
        assertEquals(3, ServiceContainerUtil.getServiceUsage(getProject(), "usage_xml_foobar"));
        assertEquals(3, ServiceContainerUtil.getServiceUsage(getProject(), "usage_xml_foobar2"));
        assertEquals(1, ServiceContainerUtil.getServiceUsage(getProject(), "usage_xml_foobar3"));
    }

    public void testGetSortedServiceId() {
        List<String> sortedServiceId = ServiceContainerUtil.getSortedServiceId(getProject(), Arrays.asList("foobar.default", "foobar", "usage_xml_foobar"));

        assertEquals("usage_xml_foobar", sortedServiceId.get(0));
        assertEquals("foobar", sortedServiceId.get(1));
        assertEquals("foobar.default", sortedServiceId.get(2));
    }

    public void testGetSortedServiceIdByUsage() {
        List<String> sortedServiceId = ServiceContainerUtil.getSortedServiceId(getProject(), Arrays.asList("foobar.default", "usage_xml_foobar3", "usage_xml_foobar2"));

        assertEquals("usage_xml_foobar2", sortedServiceId.get(0));
        assertEquals("usage_xml_foobar3", sortedServiceId.get(1));
        assertEquals("foobar.default", sortedServiceId.get(2));
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.dic.container.util.ServiceContainerUtil#getServicesInFile
     */
    public void testYamlFileScopeDefaultsForSymfony33() {
        Collection<ServiceSerializable> services = ServiceContainerUtil.getServicesInFile(myFixture.configureByFile("services3-3.yml"));

        assertFalse(services.stream().anyMatch(service -> "_defaults".equals(service.getId())));

        ServiceSerializable defaults = services.stream().filter(service -> "_yaml.defaults".equals(service.getId())).findFirst().get();
        assertTrue(defaults.isAutowire());
        assertFalse(defaults.isPublic());

        ServiceSerializable defaultsOverwrite = services.stream().filter(service -> "_yaml.defaults_overwrite".equals(service.getId())).findFirst().get();
        assertFalse(defaultsOverwrite.isAutowire());
        assertTrue(defaultsOverwrite.isPublic());

        ServiceSerializable defaultsClass = services.stream().filter(service -> "Yaml\\DefaultClassPrivateAutowire".equals(service.getId())).findFirst().get();
        assertTrue(defaultsClass.isAutowire());
        assertFalse(defaultsClass.isPublic());

        ServiceSerializable defaultsAlias = services.stream().filter(service -> "_yaml.defaults_alias".equals(service.getId())).findFirst().get();
        assertTrue(defaultsAlias.isAutowire());
        assertFalse(defaultsAlias.isPublic());
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.dic.container.util.ServiceContainerUtil#getServicesInFile
     */
    public void testYamlFileScopeDefaultsForSymfony5() {
        Collection<ServiceSerializable> services = ServiceContainerUtil.getServicesInFile(myFixture.configureByFile("services5.yml"));

        ServiceSerializable appResourceSingle = services.stream().filter(service -> "AppSingle\\".equals(service.getId())).findFirst().get();
        assertContainsElements(appResourceSingle.getResource(), "../src/*");
        assertContainsElements(appResourceSingle.getExclude(), "../src/{DependencyInjection,Entity,Migrations,Tests,Kernel.php}");

        ServiceSerializable appResourceArray = services.stream().filter(service -> "AppArray\\".equals(service.getId())).findFirst().get();
        assertContainsElements(appResourceArray.getResource(), "../src2/*", "../src/*");
        assertContainsElements(appResourceArray.getExclude(), "../src/{DependencyInjection,Kernel.php}", "../src2/{Tests,Kernel.php}");
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.dic.container.util.ServiceContainerUtil#getServicesInFile
     */
    public void testXmlFileScopeDefaultsForSymfony33() {
        Collection<ServiceSerializable> services = ServiceContainerUtil.getServicesInFile(myFixture.configureByFile("services3-3.xml"));

        ServiceSerializable defaults = services.stream().filter(service -> "_xml.defaults".equals(service.getId())).findFirst().get();
        assertTrue(defaults.isAutowire());
        assertFalse(defaults.isPublic());

        ServiceSerializable defaultsOverwrite = services.stream().filter(service -> "_xml.defaults_overwrite".equals(service.getId())).findFirst().get();
        assertFalse(defaultsOverwrite.isAutowire());
        assertTrue(defaultsOverwrite.isPublic());
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.dic.container.util.ServiceContainerUtil#getYamlConstructorTypeHint
     */
    public void testGetYamlConstructorTypeHint() {
        myFixture.configureByText("test.yml", "" +
            "services:\n" +
            "   NamedArgument\\Foobar:\n" +
            "       arguments: ['<caret>']\n"
        );

        PsiElement psiElement = myFixture.getFile().findElementAt(myFixture.getCaretOffset());

        YAMLScalar parent = (YAMLScalar) psiElement.getParent();

        ServiceTypeHint typeHint = ServiceContainerUtil.getYamlConstructorTypeHint(
            parent,
            new ContainerCollectionResolver.LazyServiceCollector(getProject())
        );

        assertEquals(0, typeHint.getIndex());
        assertEquals("__construct", typeHint.getMethod().getName());
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.dic.container.util.ServiceContainerUtil#getYamlConstructorTypeHint
     */
    public void testGetYamlConstructorTypeHintForNamedArgument() {
        myFixture.configureByText("test.yml", "" +
            "services:\n" +
            "   NamedArgument\\Foobar:\n" +
            "       arguments:\n" +
            "           $foobar: '<caret>'\n"
        );

        PsiElement psiElement = myFixture.getFile().findElementAt(myFixture.getCaretOffset());

        YAMLScalar parent = (YAMLScalar) psiElement.getParent();

        ServiceTypeHint typeHint = ServiceContainerUtil.getYamlConstructorTypeHint(
            parent,
            new ContainerCollectionResolver.LazyServiceCollector(getProject())
        );

        assertEquals(0, typeHint.getIndex());
        assertEquals("__construct", typeHint.getMethod().getName());
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.dic.container.util.ServiceContainerUtil#getXmlConstructorTypeHint
     */
    public void testGetXmlConstructorTypeHint() {
        myFixture.configureByText("services.xml", "" +
                "<services>" +
                "     <service id=\"NamedArgument\\Foobar\">\n" +
                "         <argument type=\"service\" key=\"$foobar\" id=\"args<caret>_bar\"/>\n" +
                "     </service>" +
                "</services>"
        );

        PsiElement psiElement = myFixture.getFile().findElementAt(myFixture.getCaretOffset());

        ServiceTypeHint typeHint = ServiceContainerUtil.getXmlConstructorTypeHint(
            psiElement,
            new ContainerCollectionResolver.LazyServiceCollector(getProject())
        );

        assertEquals(0, typeHint.getIndex());
        assertEquals("__construct", typeHint.getMethod().getName());
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.dic.container.util.ServiceContainerUtil#getXmlCallTypeHint
     */
    public void testGetXmlCallTypeHint() {
        myFixture.configureByText("services.xml", "" +
            "<services>" +
            "     <service id=\"NamedArgument\\Foobar\">\n" +
            "         <call method=\"setFoo\">\n" +
            "             <argument type=\"service\" key=\"$foobar\" id=\"args_bar<caret>\"/>\n" +
            "         </call>\n" +
            "     </service>" +
            "</services>"
        );

        PsiElement psiElement = myFixture.getFile().findElementAt(myFixture.getCaretOffset());

        ServiceTypeHint typeHint = ServiceContainerUtil.getXmlCallTypeHint(
            psiElement,
            new ContainerCollectionResolver.LazyServiceCollector(getProject())
        );

        assertEquals(1, typeHint.getIndex());
        assertEquals("setFoo", typeHint.getMethod().getName());
    }

    public void testVisitNamedArguments() {
        PsiFile psiFile = myFixture.configureByText("test.yml", "" +
            "services:\n" +
            "   NamedArgument\\Foobar:\n" +
            "       arguments: []\n" +
            "" +
            "   App\\Controller\\:\n" +
            "       resource: '../src/Controller'\n" +
            "       tags: ['controller.service_arguments']\n"
        );

        Collection<String> arguments = new HashSet<>();
        ServiceContainerUtil.visitNamedArguments(psiFile, parameter -> arguments.add(parameter.getName()));

        assertTrue(arguments.contains("foobar"));

        assertTrue(arguments.contains("foobarString"));
        assertFalse(arguments.contains("private"));
    }

    public void testGetTargetsForConstantForEmptyClassConstName() {
        assertEmpty(ServiceContainerUtil.getTargetsForConstant(getProject(), "\\App\\Service\\FooService::"));
    }

    private static class MyStringServiceInterfaceCondition implements Condition<ServiceInterface> {

        @NotNull
        private final String id;

        private MyStringServiceInterfaceCondition(@NotNull String id) {
            this.id = id;
        }

        @Override
        public boolean value(ServiceInterface serviceInterface) {
            return serviceInterface.getId().equals(this.id);
        }

        public static MyStringServiceInterfaceCondition create(@NotNull String id) {
            return new MyStringServiceInterfaceCondition(id);
        }
    }
}
