package fr.adrienbrault.idea.symfony2plugin.tests.actions.ui;

import com.intellij.psi.PsiFile;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.Parameter;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.action.ui.MethodParameter;
import fr.adrienbrault.idea.symfony2plugin.action.ui.ServiceBuilder;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlPsiElementFactory;
import org.apache.commons.lang.StringUtils;

import java.io.File;
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
        return new File(this.getClass().getResource("fixtures").getFile()).getAbsolutePath();
    }

    public void testBuildForClassWithoutParameter() {
        PsiFile dummyFile = YamlPsiElementFactory.createDummyFile(getProject(), "foo.yml", "services:\n  foobar: ~");
        ServiceBuilder serviceBuilder = new ServiceBuilder(Collections.emptyList(), dummyFile);

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
        PsiFile dummyFile = YamlPsiElementFactory.createDummyFile(getProject(), "foo.yml", "services:\n  foobar: ~");

        PhpClass anyByFQN = PhpIndex.getInstance(getProject()).getAnyByFQN("\\Foo\\Bar").iterator().next();

        Method constructor = anyByFQN.getConstructor();
        assertNotNull(constructor);

        Parameter parameter = constructor.getParameters()[0];

        List<MethodParameter.MethodModelParameter> parameters = Collections.singletonList(
            new MethodParameter.MethodModelParameter(constructor, parameter, 0, new HashSet<>(Collections.singletonList("foobar")), "foobar")
        );

        ServiceBuilder serviceBuilder = new ServiceBuilder(parameters, dummyFile);

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
}
