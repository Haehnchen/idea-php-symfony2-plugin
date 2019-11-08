package fr.adrienbrault.idea.symfonyplugin.tests.action.ui;

import com.intellij.psi.PsiFile;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.Parameter;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfonyplugin.action.ui.MethodParameter;
import fr.adrienbrault.idea.symfonyplugin.action.ui.ServiceBuilder;
import fr.adrienbrault.idea.symfonyplugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import fr.adrienbrault.idea.symfonyplugin.util.yaml.YamlPsiElementFactory;
import org.apache.commons.lang.StringUtils;
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
        return "src/test/java/fr/adrienbrault/idea/symfonyplugin/tests/action/ui/fixtures";
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

    @NotNull
    private List<MethodParameter.MethodModelParameter> getMethodModelParameters() {
        PhpClass anyByFQN = PhpIndex.getInstance(getProject()).getAnyByFQN("\\Foo\\Bar").iterator().next();

        Method constructor = anyByFQN.getConstructor();
        assertNotNull(constructor);

        Parameter parameter = constructor.getParameters()[0];

        return Collections.singletonList(
            new MethodParameter.MethodModelParameter(constructor, parameter, 0, new HashSet<>(Collections.singletonList("foobar")), "foobar")
        );
    }
}
