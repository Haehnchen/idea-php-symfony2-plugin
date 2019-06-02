package fr.adrienbrault.idea.symfony2plugin.tests.stubs;

import fr.adrienbrault.idea.symfony2plugin.dic.ContainerParameter;
import fr.adrienbrault.idea.symfony2plugin.dic.ContainerService;
import fr.adrienbrault.idea.symfony2plugin.stubs.ContainerCollectionResolver;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import org.jetbrains.yaml.YAMLFileType;

import java.util.Set;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.stubs.ContainerCollectionResolver
 */
public class ContainerCollectionResolverTest extends SymfonyLightCodeInsightFixtureTestCase {
    public void setUp() throws Exception {
        super.setUp();

        myFixture.configureByText("foo1.yml", "" +
            "parameters:\n" +
            "    bar: foo\n" +
            "\n" +
            "services:\n" +
            "    foo:\n" +
            "        class: DateTime\n" +
            "    foo_slash:\n" +
            "        class: \\DateTime\n" +
            "    foo_UPPER:\n" +
            "        class: \\DateTime\n" +
            "    foo_datetime:\n" +
            "        class: \\DateTime\n"
        );

        myFixture.configureByText("foo2.yml", "" +
            "services:\n" +
            "    foo_datetime:\n" +
            "        class: \\DateTimeInterface\n"
        );

        myFixture.configureByText("foo3.yml", "" +
            "services:\n" +
            "    foo_datetime:\n" +
            "        class: DateTimeInterface\n"
        );

        myFixture.configureByText("foo4.yml", "" +
            "parameters:\n" +
            "    bar: foo\n" +
            "\n" +
            "services:\n" +
            "    foo_datetime:\n" +
            "        class: %bar%\n"
        );

        myFixture.copyFileToProject("ContainerBuilder.php");
        myFixture.copyFileToProject("decorator.services.xml");
        myFixture.copyFileToProject("kernel_parameter.php");
    }

    public String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/stubs/fixtures";
    }

    public void testCaseInsensitiveService() {
        assertEquals("DateTime", ContainerCollectionResolver.getService(getProject(), "foo").getClassName());
        assertEquals("DateTime", ContainerCollectionResolver.getService(getProject(), "foo_upper").getClassName());
        assertEquals("DateTime", ContainerCollectionResolver.getService(getProject(), "Foo").getClassName());
        assertTrue(ContainerCollectionResolver.hasServiceNames(getProject(), "foo"));
        assertTrue(ContainerCollectionResolver.hasServiceNames(getProject(), "Foo"));
    }

    public void testCaseInsensitiveParameter() {
        assertTrue(ContainerCollectionResolver.getParameterNames(getProject()).contains("Bar"));
        assertTrue(ContainerCollectionResolver.getParameterNames(getProject()).contains("bar"));
    }

    public void testThatLeadingSlashIsStripped() {
        assertEquals("DateTime", ContainerCollectionResolver.getService(getProject(), "foo_slash").getClassName());
    }

    public void testThatDuplicateClassNamesProvidesVariantsAndResolvesParameter() {
        ContainerService results = ContainerCollectionResolver.getService(getProject(), "foo_datetime");
        Set<String> classNames = results.getClassNames();
        assertSize(3, classNames);

        assertTrue(classNames.contains("DateTime"));
        assertTrue(classNames.contains("DateTimeInterface"));
        assertTrue(classNames.contains("foo"));
    }

    public void testThatAliasedServiceIsEqualWithMainService() {

        myFixture.configureByText(YAMLFileType.YML, "" +
                "services:\n" +
                "    foo_as_alias:\n" +
                "        alias: foo\n" +
                "    foo:\n" +
                "        class: DateTime\n"
        );

        assertTrue(ContainerCollectionResolver.hasServiceNames(getProject(), "foo_as_alias"));
        assertTrue(ContainerCollectionResolver.hasServiceNames(getProject(), "foo"));
        assertEquals("DateTime", ContainerCollectionResolver.getService(getProject(), "foo_as_alias").getClassName());
        assertEquals("DateTime", ContainerCollectionResolver.getService(getProject(), "foo").getClassName());
    }

    public void testThatContainerBuilderParameterAreCollected() {
        assertContainsElements(ContainerCollectionResolver.getParameterNames(getProject()), "container.builder.parameter");
        ContainerParameter containerParameter = ContainerCollectionResolver.getParameters(getProject()).get("container.builder.parameter");
        assertNotNull(containerParameter);
        assertTrue(containerParameter.isWeak());
    }

    public void testThatDecoratedServiceProvidesInner() {
        ContainerService service = ContainerCollectionResolver.getService(getProject(), "espend.my_next_foo.inner");
        assertNotNull(service);

        assertEquals("espend\\MyFirstFoo", service.getClassName());
        assertEquals("espend.my_next_foo.inner", service.getName());
        assertTrue(service.isPrivate());

        service = ContainerCollectionResolver.getService(getProject(), "espend.my_bar_customer_inner.inner_foo");
        assertNotNull(service);

        assertEquals("espend\\MyNextFoo", service.getClassName());
        assertEquals("espend.my_bar_customer_inner.inner_foo", service.getName());

        assertEquals(true, service.isPrivate());
        assertEquals(true, service.isWeak());
    }

    public void testThatGetKernelParametersAreCollected() {
        assertContainsElements(ContainerCollectionResolver.getParameters(getProject()).keySet(), "kernel.foobar");
    }
}
