package fr.adrienbrault.idea.symfony2plugin.tests.dic.container.util;

import com.google.gson.Gson;
import com.intellij.openapi.util.Condition;
import com.intellij.psi.PsiFile;
import com.intellij.util.containers.ContainerUtil;
import fr.adrienbrault.idea.symfony2plugin.dic.container.ServiceInterface;
import fr.adrienbrault.idea.symfony2plugin.dic.container.util.ServiceContainerUtil;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import org.jetbrains.annotations.NotNull;

import java.io.File;

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
    }

    public String getTestDataPath() {
        return new File(this.getClass().getResource("fixtures").getFile()).getAbsolutePath();
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
            ServiceInterface bar = ContainerUtil.find(ServiceContainerUtil.getServicesInFile(psiFile), MyStringServiceInterfaceCondition.create("bar.upper"));
            assertEquals("bar.upper", bar.getId());
        }
    }

    public void testNonDefaultValuesAreSerialized() {
        for (PsiFile psiFile : new PsiFile[]{xmlFile, ymlFile}) {
            ServiceInterface bar = ContainerUtil.find(ServiceContainerUtil.getServicesInFile(psiFile), MyStringServiceInterfaceCondition.create("non.defaults"));
            assertEquals(
                "{\"id\":\"non.defaults\",\"class\":\"DateTime\",\"public\":false,\"lazy\":true,\"abstract\":true,\"autowire\":true,\"deprecated\":true,\"alias\":\"foo\",\"decorates\":\"foo\",\"decoration_inner_name\":\"foo\",\"parent\":\"foo\"}",
                new Gson().toJson(bar)
            );
        }
    }

    public void testThatDefaultValueAreNullAndNotSerialized() {
        for (PsiFile psiFile : new PsiFile[]{xmlFile, ymlFile}) {
            ServiceInterface bar = ContainerUtil.find(ServiceContainerUtil.getServicesInFile(psiFile), MyStringServiceInterfaceCondition.create("defaults"));
            assertEquals(
                "{\"id\":\"defaults\",\"class\":\"DateTime\"}",
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
