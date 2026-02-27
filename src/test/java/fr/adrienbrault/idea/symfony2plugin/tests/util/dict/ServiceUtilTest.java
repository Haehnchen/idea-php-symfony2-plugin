package fr.adrienbrault.idea.symfony2plugin.tests.util.dict;

import com.intellij.ide.highlighter.XmlFileType;
import com.intellij.openapi.util.Condition;
import com.intellij.util.containers.ContainerUtil;
import com.jetbrains.php.lang.PhpFileType;
import fr.adrienbrault.idea.symfony2plugin.dic.ContainerService;
import fr.adrienbrault.idea.symfony2plugin.stubs.ContainerCollectionResolver;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.dict.ServiceUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Set;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ServiceUtilTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void setUp() throws Exception {
        super.setUp();
        myFixture.copyFileToProject("classes.php");
        myFixture.copyFileToProject("services.xml");
    }

    public String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/util/dict/fixtures";
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.util.dict.ServiceUtil#getPhpClassTags
     */
    public void testGetPhpClassTags() {
        Set<String> myTaggedClass = ServiceUtil.getPhpClassTags(PhpElementsUtil.getClass(getProject(), "MyTaggedClass"));
        assertContainsElements(myTaggedClass, "foo_datetime");
        assertContainsElements(myTaggedClass, "foo_iterator");
        assertDoesntContain(myTaggedClass, "foo_extends");
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.util.dict.ServiceUtil#getServiceSuggestionForPhpClass
     */
    public void testGetServiceSuggestionForPhpClass() {

        myFixture.configureByText(XmlFileType.INSTANCE, "" +
            "<container>\n" +
            "    <services>\n" +
            "        <service id=\"my_foo_instance\" class=\"MyFooInstance\"/>\n" +
            "    </services>\n" +
            "</container>\n"
        );

        myFixture.configureByText(PhpFileType.INSTANCE, "<?php\n" +
            "class MyFooInstance implements MyFooInstanceInterface{};\n" +
            "interface MyFooInstanceInterface{};"
        );

        Map<String, ContainerService> services = ContainerCollectionResolver.getServices(getProject());

        assertNotNull(ContainerUtil.find(
            ServiceUtil.getServiceSuggestionForPhpClass(PhpElementsUtil.getClassInterface(getProject(), "MyFooInstanceInterface"), services),
            new MyNameContainerServiceCondition("my_foo_instance")
        ));

        assertNotNull(ContainerUtil.find(
            ServiceUtil.getServiceSuggestionForPhpClass(PhpElementsUtil.getClassInterface(getProject(), "MyFooInstance"), services),
            new MyNameContainerServiceCondition("my_foo_instance")
        ));
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.util.dict.ServiceUtil#getParameterParameters
     */
    public void testGetParameterParameters() {
        myFixture.copyFileToProject("kernel_parameter.php");

        assertContainsElements(
            ServiceUtil.getParameterParameters(getProject()),
            "kernel.root_dir", "kernel.project_dir", "kernel.array_merge",
            "kernel.array_merge_recursive", "kernel.array_replace", "kernel.array", "kernel.array_replace_2"
        );
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.util.dict.ServiceUtil#getParameterParameters
     */
    public void testGetParameterParametersModernSymfony() {
        myFixture.copyFileToProject("kernel_parameter.php");

        // Symfony 6.4/7.x style with short array syntax and + operator for array union
        assertContainsElements(
            ServiceUtil.getParameterParameters(getProject()),
            // Direct array keys
            "kernel.project_dir", "kernel.environment", "kernel.runtime_environment", "kernel.debug",
            "kernel.build_dir", "kernel.cache_dir", "kernel.logs_dir", "kernel.bundles",
            "kernel.bundles_metadata", "kernel.charset", "kernel.container_class",
            // From the + operator with ternary expression
            "kernel.share_dir"
        );
    }

    private static class MyNameContainerServiceCondition implements Condition<ContainerService> {

        @NotNull
        private final String serviceName;

        public MyNameContainerServiceCondition(@NotNull String serviceName) {
            this.serviceName = serviceName;
        }

        @Override
        public boolean value(ContainerService containerService) {
            return containerService.getName().equals(this.serviceName);
        }
    }
}
