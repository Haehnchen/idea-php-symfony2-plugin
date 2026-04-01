package fr.adrienbrault.idea.symfony2plugin.tests.action.ui;

import com.intellij.psi.PsiFile;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.Parameter;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.action.ui.MethodParameter;
import fr.adrienbrault.idea.symfony2plugin.action.ui.ServiceBuilder;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlPsiElementFactory;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ServiceBuilderTest extends SymfonyLightCodeInsightFixtureTestCase {
    public void setUp() throws Exception {
        super.setUp();
        myFixture.configureFromExistingVirtualFile(myFixture.copyFileToProject("classes.php"));
    }

    protected String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/action/ui/fixtures";
    }

    public void testBuildForClassWithoutParameter() {
        PsiFile dummyFile = YamlPsiElementFactory.createDummyFile(getProject(), "foo.yml", "services:\n  foobar: ~");
        ServiceBuilder serviceBuilder = new ServiceBuilder(Collections.emptyList(), dummyFile, false);

        assertEquals(
            "foobar:\n  class: Foobar",
            serviceBuilder.build(ServiceBuilder.OutputType.Yaml, "Foobar", "foobar")
        );

        assertEquals(
            "<service class=\"Foobar\" id=\"foobar\"/>",
            StringUtils.trim(serviceBuilder.build(ServiceBuilder.OutputType.XML, "Foobar", "foobar"))
        );
    }

    public void testBuildForConstructor() {
        ServiceBuilder serviceBuilder = new ServiceBuilder(
            getMethodModelParameters(),
            YamlPsiElementFactory.createDummyFile(getProject(), "foo.yml", "services:\n  foobar: ~"),
            false
        );

        String expectedYaml = "" +
            "foobar:\n" +
            "  class: Foo\\Bar\n" +
            "  arguments: ['@foobar']";

        assertEquals(
            expectedYaml,
            serviceBuilder.build(ServiceBuilder.OutputType.Yaml, "Foo\\Bar", "foobar")
        );

        String expectedXml = "" +
            "<service class=\"Foo\\Bar\" id=\"foobar\">\n" +
            "  <argument id=\"foobar\" type=\"service\"/>\n" +
            "</service>";

        assertEquals(
            expectedXml,
            StringUtils.trim(serviceBuilder.build(ServiceBuilder.OutputType.XML, "Foo\\Bar", "foobar"))
        );
    }

    public void testBuildForClassAsId() {
        ServiceBuilder serviceBuilder = new ServiceBuilder(Collections.emptyList(), getProject(), true);

        assertEquals(
            "Foobar: ~",
            serviceBuilder.build(ServiceBuilder.OutputType.Yaml, "Foobar", "foobar")
        );

        assertEquals(
            "<service id=\"Foobar\"/>",
            StringUtils.trim(serviceBuilder.build(ServiceBuilder.OutputType.XML, "Foobar", "foobar"))
        );
    }

    public void testBuildForClassAsIdWithParameter() {
        ServiceBuilder serviceBuilder = new ServiceBuilder(
            getMethodModelParameters(),
            getProject(),
            true
        );

        assertEquals(
            "Foobar:\n    arguments: ['@foobar']",
            serviceBuilder.build(ServiceBuilder.OutputType.Yaml, "Foobar", "foobar")
        );

        assertEquals(
            "<service id=\"Foobar\">\n  <argument id=\"foobar\" type=\"service\"/>\n</service>",
            StringUtils.trim(serviceBuilder.build(ServiceBuilder.OutputType.XML, "Foobar", "foobar"))
        );
    }

    public void testBuildFluentForClassWithoutParameter() {
        ServiceBuilder serviceBuilder = new ServiceBuilder(Collections.emptyList(), getProject(), false);

        assertEquals(
            "$services->set('foobar', \\Foobar::class);",
            serviceBuilder.build(ServiceBuilder.OutputType.Fluent, "Foobar", "foobar")
        );
    }

    public void testBuildFluentForConstructor() {
        ServiceBuilder serviceBuilder = new ServiceBuilder(
            getMethodModelParameters(),
            getProject(),
            false
        );

        String expected = "" +
            "$services->set('foobar', \\Foo\\Bar::class)\n" +
            "    ->args([\n" +
            "        service('foobar'),\n" +
            "    ]);";

        assertEquals(
            expected,
            serviceBuilder.build(ServiceBuilder.OutputType.Fluent, "Foo\\Bar", "foobar")
        );
    }

    public void testBuildFluentForClassAsId() {
        ServiceBuilder serviceBuilder = new ServiceBuilder(Collections.emptyList(), getProject(), true);

        assertEquals(
            "$services->set(\\Foobar::class);",
            serviceBuilder.build(ServiceBuilder.OutputType.Fluent, "Foobar", "foobar")
        );
    }

    public void testBuildPhpArrayForClassWithoutParameter() {
        ServiceBuilder serviceBuilder = new ServiceBuilder(Collections.emptyList(), getProject(), false);

        assertEquals(
            "'foobar' => [\n    'class' => \\Foobar::class,\n],",
            serviceBuilder.build(ServiceBuilder.OutputType.PhpArray, "Foobar", "foobar")
        );
    }

    public void testBuildPhpArrayForConstructor() {
        ServiceBuilder serviceBuilder = new ServiceBuilder(
            getMethodModelParameters(),
            getProject(),
            false
        );

        String expected = "" +
            "'foobar' => [\n" +
            "    'class' => \\Foo\\Bar::class,\n" +
            "    'arguments' => [\n" +
            "        service('foobar'),\n" +
            "    ],\n" +
            "],";

        assertEquals(
            expected,
            serviceBuilder.build(ServiceBuilder.OutputType.PhpArray, "Foo\\Bar", "foobar")
        );
    }

    public void testBuildFluentForInterfaceServiceIdArgument() {
        ServiceBuilder serviceBuilder = new ServiceBuilder(
            getMethodModelParameters("Foo\\BarInterface"),
            getProject(),
            false
        );

        String expected = "" +
            "$services->set('foobar', \\Foo\\Bar::class)\n" +
            "    ->args([\n" +
            "        service('Foo\\BarInterface'),\n" +
            "    ]);";

        assertEquals(
            expected,
            serviceBuilder.build(ServiceBuilder.OutputType.Fluent, "Foo\\Bar", "foobar")
        );
    }

    public void testBuildPhpArrayForClassAsId() {
        ServiceBuilder serviceBuilder = new ServiceBuilder(Collections.emptyList(), getProject(), true);

        assertEquals(
            "\\Foobar::class => [\n],",
            serviceBuilder.build(ServiceBuilder.OutputType.PhpArray, "Foobar", "foobar")
        );
    }

    public void testBuildPhpArrayForInterfaceServiceIdArgument() {
        ServiceBuilder serviceBuilder = new ServiceBuilder(
            getMethodModelParameters("Foo\\BarInterface"),
            getProject(),
            false
        );

        String expected = "" +
            "'foobar' => [\n" +
            "    'class' => \\Foo\\Bar::class,\n" +
            "    'arguments' => [\n" +
            "        service('Foo\\BarInterface'),\n" +
            "    ],\n" +
            "],";

        assertEquals(
            expected,
            serviceBuilder.build(ServiceBuilder.OutputType.PhpArray, "Foo\\Bar", "foobar")
        );
    }

    public void testIsPossibleServiceFalseExcludesConstructorArguments() {
        PhpClass anyByFQN = PhpIndex.getInstance(getProject()).getAnyByFQN("\\Foo\\Bar").iterator().next();

        Method constructor = anyByFQN.getConstructor();
        assertNotNull(constructor);

        Parameter parameter = constructor.getParameters()[0];

        MethodParameter.MethodModelParameter modelParameter = new MethodParameter.MethodModelParameter(constructor, parameter, 0, new HashSet<>(Collections.singletonList("foobar")), "foobar");
        modelParameter.setPossibleService(false);

        PsiFile dummyFile = YamlPsiElementFactory.createDummyFile(getProject(), "foo.yml", "services:\n  foobar: ~");

        // With isPossibleService=false, arguments should not be generated for YAML
        ServiceBuilder yamlBuilder = new ServiceBuilder(
            Collections.singletonList(modelParameter),
            dummyFile,
            false
        );

        assertEquals(
            "foobar:\n  class: Foo\\Bar",
            yamlBuilder.build(ServiceBuilder.OutputType.Yaml, "Foo\\Bar", "foobar")
        );

        // XML output
        ServiceBuilder xmlBuilder = new ServiceBuilder(
            Collections.singletonList(modelParameter),
            getProject(),
            false
        );

        assertEquals(
            "<service class=\"Foo\\Bar\" id=\"foobar\"/>",
            StringUtils.trim(xmlBuilder.build(ServiceBuilder.OutputType.XML, "Foo\\Bar", "foobar"))
        );

        // Fluent output
        assertEquals(
            "$services->set('foobar', \\Foo\\Bar::class);",
            xmlBuilder.build(ServiceBuilder.OutputType.Fluent, "Foo\\Bar", "foobar")
        );

        // PhpArray output
        assertEquals(
            "'foobar' => [\n    'class' => \\Foo\\Bar::class,\n],",
            xmlBuilder.build(ServiceBuilder.OutputType.PhpArray, "Foo\\Bar", "foobar")
        );
    }

    public void testIsPossibleServiceTrueWithConstructorArguments() {
        PhpClass anyByFQN = PhpIndex.getInstance(getProject()).getAnyByFQN("\\Foo\\Bar").iterator().next();

        Method constructor = anyByFQN.getConstructor();
        assertNotNull(constructor);

        Parameter parameter = constructor.getParameters()[0];

        MethodParameter.MethodModelParameter modelParameter = new MethodParameter.MethodModelParameter(constructor, parameter, 0, new HashSet<>(Collections.singletonList("foobar")), "foobar");
        // isPossibleService is true by default via this constructor

        PsiFile dummyFile = YamlPsiElementFactory.createDummyFile(getProject(), "foo.yml", "services:\n  foobar: ~");

        // With isPossibleService=true, arguments should be generated
        ServiceBuilder yamlBuilder = new ServiceBuilder(
            Collections.singletonList(modelParameter),
            dummyFile,
            false
        );

        assertEquals(
            "foobar:\n  class: Foo\\Bar\n  arguments: ['@foobar']",
            yamlBuilder.build(ServiceBuilder.OutputType.Yaml, "Foo\\Bar", "foobar")
        );

        // XML output
        ServiceBuilder xmlBuilder = new ServiceBuilder(
            Collections.singletonList(modelParameter),
            getProject(),
            false
        );

        assertEquals(
            "<service class=\"Foo\\Bar\" id=\"foobar\">\n  <argument id=\"foobar\" type=\"service\"/>\n</service>",
            StringUtils.trim(xmlBuilder.build(ServiceBuilder.OutputType.XML, "Foo\\Bar", "foobar"))
        );
    }

    @NotNull
    private List<MethodParameter.MethodModelParameter> getMethodModelParameters() {
        return getMethodModelParameters("foobar");
    }

    @NotNull
    private List<MethodParameter.MethodModelParameter> getMethodModelParameters(@NotNull String serviceId) {
        PhpClass anyByFQN = PhpIndex.getInstance(getProject()).getAnyByFQN("\\Foo\\Bar").iterator().next();

        Method constructor = anyByFQN.getConstructor();
        assertNotNull(constructor);

        Parameter parameter = constructor.getParameters()[0];

        return Collections.singletonList(
            new MethodParameter.MethodModelParameter(constructor, parameter, 0, new HashSet<>(Collections.singletonList(serviceId)), serviceId)
        );
    }
}
