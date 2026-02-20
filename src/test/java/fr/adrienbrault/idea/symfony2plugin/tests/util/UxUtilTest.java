package fr.adrienbrault.idea.symfony2plugin.tests.util;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.PhpFileType;
import com.jetbrains.php.lang.psi.PhpFile;
import com.jetbrains.php.lang.psi.PhpPsiElementFactory;
import com.jetbrains.php.lang.psi.elements.Field;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.elements.PhpNamedElement;
import com.jetbrains.twig.TwigFile;
import com.jetbrains.twig.TwigFileType;
import fr.adrienbrault.idea.symfony2plugin.Settings;
import fr.adrienbrault.idea.symfony2plugin.dic.ContainerFile;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import fr.adrienbrault.idea.symfony2plugin.templating.path.TwigNamespaceSetting;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import fr.adrienbrault.idea.symfony2plugin.util.UxUtil;
import fr.adrienbrault.idea.symfony2plugin.util.SymfonyVarDirectoryWatcherKt;
import fr.adrienbrault.idea.symfony2plugin.util.dict.TwigComponentNamespace;
import kotlin.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class UxUtilTest extends SymfonyLightCodeInsightFixtureTestCase {
    private List<TwigNamespaceSetting> previousTwigNamespaces;
    private List<ContainerFile> previousContainerFiles;

    public void setUp() throws Exception {
        super.setUp();
        Settings settings = Settings.getInstance(getProject());
        previousTwigNamespaces = settings.twigNamespaces != null ? new ArrayList<>(settings.twigNamespaces) : new ArrayList<>();
        previousContainerFiles = settings.containerFiles != null ? new ArrayList<>(settings.containerFiles) : new ArrayList<>();
        settings.twigNamespaces = new ArrayList<>();
        settings.containerFiles = new ArrayList<>();

        myFixture.configureFromExistingVirtualFile(myFixture.copyFileToProject("UxUtil.php"));
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            Settings settings = Settings.getInstance(getProject());
            settings.twigNamespaces = new ArrayList<>(previousTwigNamespaces);
            settings.containerFiles = new ArrayList<>(previousContainerFiles);
            SymfonyVarDirectoryWatcherKt.getSymfonyVarDirectoryWatcher(getProject()).reloadConfiguration();
        } finally {
            super.tearDown();
        }
    }

    public String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/util/fixtures";
    }
    public void testUxUtil() {
        myFixture.copyFileToProject("twig_component.yaml");

        Collection<TwigComponentNamespace> namespaces = UxUtil.getNamespaces(getProject());
        assertEquals("components/", namespaces.stream().filter(n -> "App\\Twig\\Components\\".equals(n.namespace())).findFirst().get().templateDirectory());
        assertEquals("components", namespaces.stream().filter(n -> "App\\Twig\\Foobar\\".equals(n.namespace())).findFirst().get().templateDirectory());
        assertEquals("foobar/", namespaces.stream().filter(n -> "App\\Twig\\WhenSwitch\\".equals(n.namespace())).findFirst().get().templateDirectory());

        TwigComponentNamespace n1 = namespaces.stream().filter(n -> "App\\Twig\\Components2\\".equals(n.namespace())).findFirst().get();
        assertEquals("components", n1.templateDirectory());
        assertEquals("AppBar", n1.namePrefix());
    }

    public void testVisitAsTwigComponent() {
        PhpFile phpFile = (PhpFile) PhpPsiElementFactory.createPsiFileFromText(getProject(), "<?php\n" +
            "namespace App\\Components;\n" +
            "\n" +
            "use Symfony\\UX\\TwigComponent\\Attribute\\AsTwigComponent;\n" +
            "use Symfony\\UX\\LiveComponent\\Attribute\\AsLiveComponent;\n" +
            "\n" +
            "#[AsTwigComponent]\n" +
            "class Alert {}\n" +
            "\n" +
            "#[AsTwigComponent('Alert2Foobar')]\n" +
            "class Alert2 {}\n" +
            "\n" +
            "#[AsTwigComponent(name: 'Alert3Foobar')]\n" +
            "class Alert3 {}\n" +
            "\n" +
            "#[AsLiveComponent]\n" +
            "class AlertAsLiveComponent {}\n"
        );

        Map<String, UxUtil.TwigComponentIndex> components = new HashMap<>();
        UxUtil.visitComponentsForIndex(phpFile, pair -> components.put(pair.phpClass().getFQN(), pair));

        // assertEquals("\\App\\Components\\Alert", components.get("Alert").getFQN());
        assertEquals("Alert2Foobar", components.get("\\App\\Components\\Alert2").name());
        assertEquals("Alert3Foobar", components.get("\\App\\Components\\Alert3").name());

        assertNull(components.get("\\App\\Components\\AlertAsLiveComponent").name());
    }

    public void testGetTwigComponentNames() {
        myFixture.configureByText(PhpFileType.INSTANCE, "<?php\n" +
            "namespace App\\Twig\\Components;\n" +
            "\n" +
            "use Symfony\\UX\\TwigComponent\\Attribute\\AsTwigComponent;\n" +
            "use Symfony\\UX\\LiveComponent\\Attribute\\AsLiveComponent;\n" +
            "\n" +
            "#[AsTwigComponent]\n" +
            "class Alert {}\n" +
            "#[AsLiveComponent]\n" +
            "class AlertAsTwigComponent {}\n"
        );

        assertContainsElements(UxUtil.getTwigComponentNames(getProject()), "Alert");

        // @TODO
        //assertFalse(UxUtil.getTwigComponentNames(getProject()).contains("AlertAsTwigComponent"));
    }

    public void testGetAllComponentNames() {
        myFixture.configureByText(PhpFileType.INSTANCE, "<?php\n" +
            "namespace App\\Twig\\Components;\n" +
            "\n" +
            "use Symfony\\UX\\TwigComponent\\Attribute\\AsTwigComponent;\n" +
            "use Symfony\\UX\\LiveComponent\\Attribute\\AsLiveComponent;\n" +
            "\n" +
            "#[AsTwigComponent]\n" +
            "class Alert {}\n" +
            "#[AsLiveComponent]\n" +
            "class AlertAsTwigComponent {}\n" +
            "#[AsLiveComponent('my_foobar')]\n" +
            "class AlertAsTwigComponent2 {}\n" +
            "#[AsLiveComponent('foobar:foobar2')]\n" +
            "class AlertAsTwigComponent3 {}\n"
        );

        assertContainsElements(
            UxUtil.getAllComponentNames(getProject()).stream().map(UxUtil.TwigComponent::name).collect(Collectors.toSet()),
            "Alert", "AlertAsTwigComponent", "my_foobar", "foobar:foobar2"
        );
    }

    public void testGetTwigComponentNameTarget() {
        myFixture.configureByText(PhpFileType.INSTANCE, "<?php\n" +
            "namespace App\\Twig\\Components;\n" +
            "\n" +
            "use Symfony\\UX\\TwigComponent\\Attribute\\AsTwigComponent;\n" +
            "\n" +
            "#[AsTwigComponent]\n" +
            "class Alert {}\n" +
            "#[AsTwigComponent('test_component')]\n" +
            "class Alert2 {}\n"
        );

        Set<PhpClass> twigComponentNameTargets = UxUtil.getTwigComponentPhpClasses(getProject(), "Alert");
        assertTrue(twigComponentNameTargets.stream().anyMatch(phpClass -> "\\App\\Twig\\Components\\Alert".equals(phpClass.getFQN())));
    }

    public void testGetComponentTemplatesForPhpClass() {
        PsiFile psiFile = myFixture.configureByText(PhpFileType.INSTANCE, "<?php\n" +
            "namespace App\\Twig\\Components;\n" +
            "\n" +
            "use Symfony\\UX\\TwigComponent\\Attribute\\AsTwigComponent;\n" +
            "\n" +
            "#[AsTwigComponent(template: 'foobar.html.twig')]\n" +
            "class Alert {}\n" +
            "#[AsTwigComponent()]\n" +
            "class Alert2 {}\n" +
            "#[AsTwigComponent('my_alert_3')]\n" +
            "class Alert3 {}\n" +
            "#[AsTwigComponent('foobar:alert_4')]\n" +
            "class Alert4 {}\n"
        );

        PhpClass phpClass1 = PsiTreeUtil.collectElementsOfType(psiFile, PhpClass.class).stream().filter(p -> p.getFQN().equals("\\App\\Twig\\Components\\Alert")).findFirst().get();
        PhpClass phpClass2 = PsiTreeUtil.collectElementsOfType(psiFile, PhpClass.class).stream().filter(p -> p.getFQN().equals("\\App\\Twig\\Components\\Alert2")).findFirst().get();
        PhpClass phpClass3 = PsiTreeUtil.collectElementsOfType(psiFile, PhpClass.class).stream().filter(p -> p.getFQN().equals("\\App\\Twig\\Components\\Alert3")).findFirst().get();
        PhpClass phpClass4 = PsiTreeUtil.collectElementsOfType(psiFile, PhpClass.class).stream().filter(p -> p.getFQN().equals("\\App\\Twig\\Components\\Alert4")).findFirst().get();

        assertContainsElements(UxUtil.getComponentTemplatesForPhpClass(phpClass1), "foobar.html.twig");
        assertContainsElements(UxUtil.getComponentTemplatesForPhpClass(phpClass2), "components/Alert2.html.twig");
        assertContainsElements(UxUtil.getComponentTemplatesForPhpClass(phpClass3), "components/my_alert_3.html.twig");
        assertContainsElements(UxUtil.getComponentTemplatesForPhpClass(phpClass4), "components/foobar/alert_4.html.twig");
    }

    public void testVisitComponentVariables() {
        Collection<PhpClass> anyByFQN = PhpIndex.getInstance(getProject()).getAnyByFQN("\\App\\Alert");

        Map<String, PhpNamedElement> map = new HashMap<>();
        UxUtil.visitComponentVariables(anyByFQN.iterator().next(), pair -> map.put(pair.getFirst(), pair.getSecond()));

        assertTrue(map.get("message") instanceof Field);
        assertTrue(map.get("ico") instanceof Field);
        assertTrue(map.get("dismissable") instanceof Method);
        assertTrue(map.get("actions") instanceof Method);
        assertTrue(map.get("alert_type") instanceof Field);assertTrue(map.get("alert_type") instanceof Field);

        assertFalse(map.containsKey("notPublicField"));
        assertFalse(map.containsKey("notPrivateMethod"));
        assertFalse(map.containsKey("notExposedPublicMethod"));
    }

    public void testGetComponentTemplatesForNestedComponentNames() {
        PsiFile psiFile = myFixture.configureByText(PhpFileType.INSTANCE, "<?php\n" +
            "namespace App\\Twig\\Components;\n" +
            "\n" +
            "use Symfony\\UX\\TwigComponent\\Attribute\\AsTwigComponent;\n" +
            "\n" +
            "#[AsTwigComponent('Alert:Html:Foo_Bar_1')]\n" +
            "class AlertHtmlFooBar1 {}\n" +
            "#[AsTwigComponent('UI:Form:Input_Text_Field')]\n" +
            "class UiFormInputTextField {}\n"
        );

        PhpClass phpClass1 = PsiTreeUtil.collectElementsOfType(psiFile, PhpClass.class).stream()
            .filter(p -> p.getFQN().equals("\\App\\Twig\\Components\\AlertHtmlFooBar1"))
            .findFirst().get();
        PhpClass phpClass2 = PsiTreeUtil.collectElementsOfType(psiFile, PhpClass.class).stream()
            .filter(p -> p.getFQN().equals("\\App\\Twig\\Components\\UiFormInputTextField"))
            .findFirst().get();

        // Nested component names with colons should be converted to path with slashes
        // Alert:Html:Foo_Bar_1 -> components/Alert/Html/Foo_Bar_1.html.twig
        assertContainsElements(UxUtil.getComponentTemplatesForPhpClass(phpClass1), "components/Alert/Html/Foo_Bar_1.html.twig");
        assertContainsElements(UxUtil.getComponentTemplatesForPhpClass(phpClass2), "components/UI/Form/Input_Text_Field.html.twig");
    }

    public void testGetAllComponentNamesIncludesNestedComponents() {
        myFixture.configureByText(PhpFileType.INSTANCE, "<?php\n" +
            "namespace App\\Twig\\Components;\n" +
            "\n" +
            "use Symfony\\UX\\TwigComponent\\Attribute\\AsTwigComponent;\n" +
            "use Symfony\\UX\\LiveComponent\\Attribute\\AsLiveComponent;\n" +
            "\n" +
            "#[AsTwigComponent('Alert:Html:Foo_Bar_1')]\n" +
            "class AlertHtmlFooBar1 {}\n" +
            "#[AsTwigComponent('UI:Form:Input')]\n" +
            "class UiFormInput {}\n" +
            "#[AsLiveComponent('Button:Primary:Icon')]\n" +
            "class ButtonPrimaryIcon {}\n"
        );

        Set<String> componentNames = UxUtil.getAllComponentNames(getProject()).stream()
            .map(UxUtil.TwigComponent::name)
            .collect(Collectors.toSet());

        assertContainsElements(componentNames, "Alert:Html:Foo_Bar_1", "UI:Form:Input", "Button:Primary:Icon");
    }

    public void testGetTwigComponentPhpClassesForNestedComponentName() {
        myFixture.configureByText(PhpFileType.INSTANCE, "<?php\n" +
            "namespace App\\Twig\\Components;\n" +
            "\n" +
            "use Symfony\\UX\\TwigComponent\\Attribute\\AsTwigComponent;\n" +
            "\n" +
            "#[AsTwigComponent('Alert:Html:Foo_Bar_1')]\n" +
            "class AlertHtmlFooBar1 {}\n"
        );

        Set<PhpClass> twigComponentNameTargets = UxUtil.getTwigComponentPhpClasses(getProject(), "Alert:Html:Foo_Bar_1");
        assertTrue(twigComponentNameTargets.stream()
            .anyMatch(phpClass -> "\\App\\Twig\\Components\\AlertHtmlFooBar1".equals(phpClass.getFQN())));
    }

    public void testAnonymousIndexTemplateProvidesDirectoryComponentName() {
        myFixture.copyFileToProject("twig_component.yaml");
        myFixture.copyFileToProject("ide-twig.json", "ide-twig.json");
        PsiFile navIndexFile = myFixture.addFileToProject("templates/components/Nav/index.html.twig", "<nav></nav>");
        PsiFile navItemFile = myFixture.addFileToProject("templates/components/Nav/Item.html.twig", "<li></li>");

        Set<String> componentNames = UxUtil.getAllComponentNames(getProject()).stream()
            .map(UxUtil.TwigComponent::name)
            .collect(Collectors.toSet());

        assertContainsElements(componentNames, "Nav", "Nav:Item");
        assertFalse(componentNames.contains("Nav:index"));

        assertContainsElements(UxUtil.getTwigComponentNames(getProject()), "Nav", "Nav:Item");
        assertEquals("Nav", UxUtil.resolveTwigComponentName(getProject(), "Nav"));
        assertNull(UxUtil.resolveTwigComponentName(getProject(), "Nav:index"));
        assertTrue(UxUtil.hasTwigComponentName(getProject(), "Nav"));
        assertFalse(UxUtil.hasTwigComponentName(getProject(), "Nav:index"));

        assertTrue(UxUtil.getComponentTemplates(getProject(), "Nav").stream()
            .map(psiFile -> psiFile.getVirtualFile() != null ? psiFile.getVirtualFile().getPath() : "")
            .anyMatch(path -> path.endsWith("/templates/components/Nav/index.html.twig")));

        assertTrue(UxUtil.getComponentTemplates(getProject(), "Nav:Item").stream()
            .map(psiFile -> psiFile.getVirtualFile() != null ? psiFile.getVirtualFile().getPath() : "")
            .anyMatch(path -> path.endsWith("/templates/components/Nav/Item.html.twig")));

        PsiFile navIndexPsi = PsiManager.getInstance(getProject()).findFile(navIndexFile.getVirtualFile());
        assertNotNull(navIndexPsi);
        assertTrue(navIndexPsi instanceof TwigFile);
        Set<String> navIndexNames = UxUtil.getTemplateComponentNames((TwigFile) navIndexPsi);
        assertContainsElements(navIndexNames, "Nav");
        assertFalse(navIndexNames.contains("Nav:index"));

        PsiFile navItemPsi = PsiManager.getInstance(getProject()).findFile(navItemFile.getVirtualFile());
        assertNotNull(navItemPsi);
        assertTrue(navItemPsi instanceof TwigFile);
        Set<String> navItemNames = UxUtil.getTemplateComponentNames((TwigFile) navItemPsi);
        assertContainsElements(navItemNames, "Nav:Item");
    }

    public void testTwigComponentNamePrefixForImplicitClassComponent() {
        myFixture.addFileToProject("config/packages/twig_component_prefix.yaml",
            "twig_component:\n" +
                "  defaults:\n" +
                "    App\\Pizza\\Components\\:\n" +
                "      template_directory: components/pizza\n" +
                "      name_prefix: Pizza\n"
        );

        PsiFile psiFile = myFixture.configureByText(PhpFileType.INSTANCE, "<?php\n" +
            "namespace App\\Pizza\\Components\\Button;\n" +
            "\n" +
            "use Symfony\\UX\\TwigComponent\\Attribute\\AsTwigComponent;\n" +
            "\n" +
            "#[AsTwigComponent]\n" +
            "class Primary {}\n"
        );

        PhpClass primaryClass = PsiTreeUtil.collectElementsOfType(psiFile, PhpClass.class).stream()
            .filter(p -> p.getFQN().equals("\\App\\Pizza\\Components\\Button\\Primary"))
            .findFirst().get();

        Set<String> componentNames = UxUtil.getAllComponentNames(getProject()).stream()
            .map(UxUtil.TwigComponent::name)
            .collect(Collectors.toSet());
        assertContainsElements(componentNames, "Pizza:Button:Primary");

        Collection<String> classTemplates = UxUtil.getComponentTemplatesForPhpClass(primaryClass);
        assertContainsElements(classTemplates, "components/pizza/Button/Primary.html.twig");
        assertFalse(classTemplates.contains("components/pizza/Pizza/Button/Primary.html.twig"));

        PsiFile templateFile = myFixture.addFileToProject("templates/components/pizza/Button/Primary.html.twig", "<div></div>");
        myFixture.copyFileToProject("ide-twig.json", "ide-twig.json");
        assertContainsVirtualFile(UxUtil.getComponentTemplates(getProject(), "Pizza:Button:Primary"), "/templates/components/pizza/Button/Primary.html.twig");

        PsiFile templatePsi = PsiManager.getInstance(getProject()).findFile(templateFile.getVirtualFile());
        assertNotNull(templatePsi);
        assertTrue(templatePsi instanceof TwigFile);
        assertContainsElements(UxUtil.getTemplateComponentNames((TwigFile) templatePsi), "Pizza:Button:Primary");
        assertTrue(UxUtil.getComponentClassesForTemplateFile(getProject(), templatePsi).stream()
            .anyMatch(phpClass -> "\\App\\Pizza\\Components\\Button\\Primary".equals(phpClass.getFQN())));
    }

    public void testPhpConfiguredTwigComponentDefaultsExposeComponentName() {
        myFixture.addFileToProject("config/packages/twig_component.php", "<?php\n" +
            "use Symfony\\Component\\DependencyInjection\\Loader\\Configurator\\ContainerConfigurator;\n" +
            "\n" +
            "return static function (ContainerConfigurator $container): void {\n" +
            "    $container->extension('twig_component', [\n" +
            "        'defaults' => [\n" +
            "            'App\\\\Shared\\\\Ui\\\\Web\\\\Component\\\\' => [\n" +
            "                'template_directory' => '@Shared',\n" +
            "            ],\n" +
            "        ],\n" +
            "    ]);\n" +
            "};\n"
        );

        myFixture.configureByText(PhpFileType.INSTANCE, "<?php\n" +
            "namespace App\\Shared\\Ui\\Web\\Component\\PageIntro;\n" +
            "\n" +
            "use Symfony\\UX\\TwigComponent\\Attribute\\AsTwigComponent;\n" +
            "\n" +
            "#[AsTwigComponent('PageIntro', template: '@Shared/PageIntro/page-intro.html.twig')]\n" +
            "final class PageIntroComponent {}\n"
        );

        assertContainsElements(UxUtil.getTwigComponentNames(getProject()), "PageIntro");
        assertTrue(UxUtil.getTwigComponentPhpClasses(getProject(), "PageIntro").stream()
            .anyMatch(phpClass -> "\\App\\Shared\\Ui\\Web\\Component\\PageIntro\\PageIntroComponent".equals(phpClass.getFQN())));
    }

    public void testPhpConfiguredTwigComponentNamePrefixForImplicitClassComponent() {
        myFixture.addFileToProject("config/packages/twig_component_prefix.php", "<?php\n" +
            "return [\n" +
            "    'twig_component' => [\n" +
            "        'defaults' => [\n" +
            "            'App\\\\Pizza\\\\Components\\\\' => [\n" +
            "                'template_directory' => 'components/pizza',\n" +
            "                'name_prefix' => 'Pizza',\n" +
            "            ],\n" +
            "        ],\n" +
            "    ],\n" +
            "];\n"
        );

        PsiFile psiFile = myFixture.configureByText(PhpFileType.INSTANCE, "<?php\n" +
            "namespace App\\Pizza\\Components\\Button;\n" +
            "\n" +
            "use Symfony\\UX\\TwigComponent\\Attribute\\AsTwigComponent;\n" +
            "\n" +
            "#[AsTwigComponent]\n" +
            "class Primary {}\n"
        );

        PhpClass primaryClass = PsiTreeUtil.collectElementsOfType(psiFile, PhpClass.class).stream()
            .filter(p -> p.getFQN().equals("\\App\\Pizza\\Components\\Button\\Primary"))
            .findFirst().get();

        Set<String> componentNames = UxUtil.getAllComponentNames(getProject()).stream()
            .map(UxUtil.TwigComponent::name)
            .collect(Collectors.toSet());
        assertContainsElements(componentNames, "Pizza:Button:Primary");
        assertContainsElements(UxUtil.getComponentTemplatesForPhpClass(primaryClass), "components/pizza/Button/Primary.html.twig");
    }

    public void testPhpConfiguredAnonymousTemplateDirectoryContributesAnonymousComponents() {
        myFixture.addFileToProject("config/packages/twig_component_anonymous.php", "<?php\n" +
            "return [\n" +
            "    'twig_component' => [\n" +
            "        'anonymous_template_directory' => 'ux-components',\n" +
            "    ],\n" +
            "];\n"
        );
        myFixture.copyFileToProject("ide-twig.json", "ide-twig.json");
        myFixture.addFileToProject("templates/ux-components/Alert.html.twig", "<div></div>");

        assertContainsElements(UxUtil.getTwigComponentNames(getProject()), "Alert");
        assertContainsVirtualFile(UxUtil.getComponentTemplates(getProject(), "Alert"), "/templates/ux-components/Alert.html.twig");
    }

    public void testCompiledContainerComponentAppearsWithoutYamlOrAttribute() {
        configureContainerXml("""
            <service id="ux.twig_component.component_factory">
                <argument type="service" id="ux.twig_component.component_template_finder"/>
                <argument type="service" id=".service_locator.demo"/>
                <argument type="service" id="property_accessor"/>
                <argument type="service" id="event_dispatcher"/>
                <argument type="collection">
                    <argument key="Shop:Card" type="collection">
                        <argument key="class">App\\Twig\\Components\\ShopCard</argument>
                        <argument key="template">components/shop/Card.html.twig</argument>
                    </argument>
                </argument>
                <argument type="collection">
                    <argument key="App\\Twig\\Components\\ShopCard">Shop:Card</argument>
                </argument>
            </service>
        """);

        myFixture.addFileToProject("src/Twig/Components/ShopCard.php", "<?php\n" +
            "namespace App\\Twig\\Components;\n" +
            "class ShopCard {}\n"
        );

        assertContainsElements(UxUtil.getTwigComponentNames(getProject()), "Shop:Card");
        assertTrue(UxUtil.getTwigComponentPhpClasses(getProject(), "Shop:Card").stream()
            .anyMatch(phpClass -> "\\App\\Twig\\Components\\ShopCard".equals(phpClass.getFQN())));
    }

    public void testCompiledContainerTemplateWinsOverConfiguredTemplateDirectory() {
        myFixture.addFileToProject("config/packages/twig_component_prefix.yaml",
            "twig_component:\n" +
                "  defaults:\n" +
                "    App\\Twig\\Components\\:\n" +
                "      template_directory: components/from-yaml\n"
        );

        configureContainerXml("""
            <service id="ux.twig_component.component_factory">
                <argument type="service" id="ux.twig_component.component_template_finder"/>
                <argument type="service" id=".service_locator.demo"/>
                <argument type="service" id="property_accessor"/>
                <argument type="service" id="event_dispatcher"/>
                <argument type="collection">
                    <argument key="Alert" type="collection">
                        <argument key="class">App\\Twig\\Components\\Alert</argument>
                        <argument key="template">compiled/Alert.html.twig</argument>
                    </argument>
                </argument>
                <argument type="collection">
                    <argument key="App\\Twig\\Components\\Alert">Alert</argument>
                </argument>
            </service>
        """);

        PsiFile psiFile = myFixture.configureByText(PhpFileType.INSTANCE, "<?php\n" +
            "namespace App\\Twig\\Components;\n" +
            "class Alert {}\n"
        );
        myFixture.copyFileToProject("ide-twig.json", "ide-twig.json");
        myFixture.addFileToProject("templates/compiled/Alert.html.twig", "<div></div>");

        PhpClass alertClass = PsiTreeUtil.collectElementsOfType(psiFile, PhpClass.class).stream()
            .filter(p -> p.getFQN().equals("\\App\\Twig\\Components\\Alert"))
            .findFirst().get();

        assertContainsElements(UxUtil.getComponentTemplatesForPhpClass(alertClass), "compiled/Alert.html.twig");
        assertFalse(UxUtil.getComponentTemplatesForPhpClass(alertClass).contains("components/from-yaml/Alert.html.twig"));
        assertContainsVirtualFile(UxUtil.getComponentTemplates(getProject(), "Alert"), "/templates/compiled/Alert.html.twig");
    }

    public void testTemplateFileFindsComponentClassThroughCompiledContainerTemplate() {
        configureContainerXml("""
            <service id="ux.twig_component.component_factory">
                <argument type="service" id="ux.twig_component.component_template_finder"/>
                <argument type="service" id=".service_locator.demo"/>
                <argument type="service" id="property_accessor"/>
                <argument type="service" id="event_dispatcher"/>
                <argument type="collection">
                    <argument key="CompiledAlert" type="collection">
                        <argument key="class">App\\Twig\\Components\\CompiledAlert</argument>
                        <argument key="template">components/CompiledAlert.html.twig</argument>
                    </argument>
                </argument>
                <argument type="collection">
                    <argument key="App\\Twig\\Components\\CompiledAlert">CompiledAlert</argument>
                </argument>
            </service>
        """);

        myFixture.addFileToProject("src/Twig/Components/CompiledAlert.php", "<?php\n" +
            "namespace App\\Twig\\Components;\n" +
            "class CompiledAlert {}\n"
        );
        myFixture.copyFileToProject("ide-twig.json", "ide-twig.json");
        PsiFile templateFile = myFixture.addFileToProject("templates/components/CompiledAlert.html.twig", "<div></div>");
        PsiFile templatePsi = PsiManager.getInstance(getProject()).findFile(templateFile.getVirtualFile());

        assertNotNull(templatePsi);
        assertTrue(UxUtil.getComponentClassesForTemplateFile(getProject(), templatePsi).stream()
            .anyMatch(phpClass -> "\\App\\Twig\\Components\\CompiledAlert".equals(phpClass.getFQN())));
    }

    public void testExplicitComponentNameWithNamePrefixKeepsExplicitNameAndTemplate() {
        myFixture.addFileToProject("config/packages/twig_component_prefix_explicit.yaml",
            "twig_component:\n" +
                "  defaults:\n" +
                "    App\\Pizza\\Components\\:\n" +
                "      template_directory: components/pizza\n" +
                "      name_prefix: Pizza\n"
        );

        PsiFile psiFile = myFixture.configureByText(PhpFileType.INSTANCE, "<?php\n" +
            "namespace App\\Pizza\\Components;\n" +
            "\n" +
            "use Symfony\\UX\\TwigComponent\\Attribute\\AsTwigComponent;\n" +
            "\n" +
            "#[AsTwigComponent(name: 'Custom:Primary', template: 'custom/primary.html.twig')]\n" +
            "class Primary {}\n"
        );

        PhpClass primaryClass = PsiTreeUtil.collectElementsOfType(psiFile, PhpClass.class).stream()
            .filter(p -> p.getFQN().equals("\\App\\Pizza\\Components\\Primary"))
            .findFirst().get();

        Set<String> componentNames = UxUtil.getAllComponentNames(getProject()).stream()
            .map(UxUtil.TwigComponent::name)
            .collect(Collectors.toSet());
        assertContainsElements(componentNames, "Custom:Primary");
        assertFalse(componentNames.contains("Pizza:Custom:Primary"));

        assertContainsElements(UxUtil.getComponentTemplatesForPhpClass(primaryClass), "custom/primary.html.twig");
    }

    public void testTwigComponentDefaultsUseComponentsDirectoryAndNoPrefixWhenOmitted() {
        myFixture.addFileToProject("config/packages/twig_component_defaults.yaml",
            "twig_component:\n" +
                "  defaults:\n" +
                "    App\\Defaulted\\Components\\:\n" +
                "      name_prefix: ''\n"
        );

        PsiFile psiFile = myFixture.configureByText(PhpFileType.INSTANCE, "<?php\n" +
            "namespace App\\Defaulted\\Components;\n" +
            "\n" +
            "use Symfony\\UX\\TwigComponent\\Attribute\\AsTwigComponent;\n" +
            "\n" +
            "#[AsTwigComponent]\n" +
            "class Alert {}\n"
        );

        PhpClass alertClass = PsiTreeUtil.collectElementsOfType(psiFile, PhpClass.class).stream()
            .filter(p -> p.getFQN().equals("\\App\\Defaulted\\Components\\Alert"))
            .findFirst().get();

        Set<String> componentNames = UxUtil.getAllComponentNames(getProject()).stream()
            .map(UxUtil.TwigComponent::name)
            .collect(Collectors.toSet());
        assertContainsElements(componentNames, "Alert");
        assertFalse(componentNames.contains(":Alert"));
        assertContainsElements(UxUtil.getComponentTemplatesForPhpClass(alertClass), "components/Alert.html.twig");
    }

    public void testOverlappingTwigComponentDefaultsUseDeclarationOrder() {
        myFixture.addFileToProject("config/packages/twig_component_overlapping.yaml",
            "twig_component:\n" +
                "  defaults:\n" +
                "    App\\:\n" +
                "      template_directory: broad\n" +
                "      name_prefix: Broad\n" +
                "    App\\Twig\\Components\\:\n" +
                "      template_directory: narrow\n" +
                "      name_prefix: Narrow\n"
        );

        PsiFile psiFile = myFixture.configureByText(PhpFileType.INSTANCE, "<?php\n" +
            "namespace App\\Twig\\Components;\n" +
            "\n" +
            "use Symfony\\UX\\TwigComponent\\Attribute\\AsTwigComponent;\n" +
            "\n" +
            "#[AsTwigComponent]\n" +
            "class Alert {}\n"
        );

        PhpClass alertClass = PsiTreeUtil.collectElementsOfType(psiFile, PhpClass.class).stream()
            .filter(p -> p.getFQN().equals("\\App\\Twig\\Components\\Alert"))
            .findFirst().get();

        Set<String> componentNames = UxUtil.getAllComponentNames(getProject()).stream()
            .map(UxUtil.TwigComponent::name)
            .collect(Collectors.toSet());
        assertContainsElements(componentNames, "Broad:Twig:Components:Alert");
        assertFalse(componentNames.contains("Narrow:Alert"));
        assertContainsElements(UxUtil.getComponentTemplatesForPhpClass(alertClass), "broad/Twig/Components/Alert.html.twig");
    }

    public void testNamespacedAnonymousTwigComponentsResolveFromTwigNamespace() {
        configureTwigNamespaceSettings(
            new TwigNamespaceSetting(TwigUtil.MAIN, "templates", true, TwigUtil.NamespaceType.ADD_PATH, true),
            new TwigNamespaceSetting("Acme", "vendor/acme/acme_twig", true, TwigUtil.NamespaceType.ADD_PATH, true)
        );

        PsiFile primaryFile = myFixture.addFileToProject("vendor/acme/acme_twig/components/Button/Primary.html.twig", "<button></button>");
        PsiFile cardFile = myFixture.addFileToProject("vendor/acme/acme_twig/components/Card/index.html.twig", "<article></article>");
        PsiFile localButtonFile = myFixture.addFileToProject("templates/components/Acme/Button.html.twig", "local");
        myFixture.addFileToProject("vendor/acme/acme_twig/components/Button.html.twig", "namespace");
        PsiFile modalFile = myFixture.addFileToProject("vendor/acme/acme_twig/components/Modal.html.twig", "direct");
        myFixture.addFileToProject("vendor/acme/acme_twig/components/Modal/index.html.twig", "index");

        Set<String> componentNames = UxUtil.getAllComponentNames(getProject()).stream()
            .map(UxUtil.TwigComponent::name)
            .collect(Collectors.toSet());
        assertContainsElements(componentNames, "Acme:Button:Primary", "Acme:Card", "Acme:Button", "Acme:Modal");

        assertContainsVirtualFile(UxUtil.getComponentTemplates(getProject(), "Acme:Button:Primary"), "/vendor/acme/acme_twig/components/Button/Primary.html.twig");
        assertContainsVirtualFile(UxUtil.getComponentTemplates(getProject(), "Acme:Card"), "/vendor/acme/acme_twig/components/Card/index.html.twig");

        Collection<PsiFile> buttonTemplates = UxUtil.getComponentTemplates(getProject(), "Acme:Button");
        assertContainsVirtualFile(buttonTemplates, "/templates/components/Acme/Button.html.twig");
        assertDoesNotContainVirtualFile(buttonTemplates, "/vendor/acme/acme_twig/components/Button.html.twig");

        Collection<PsiFile> modalTemplates = UxUtil.getComponentTemplates(getProject(), "Acme:Modal");
        assertContainsVirtualFile(modalTemplates, "/vendor/acme/acme_twig/components/Modal.html.twig");
        assertDoesNotContainVirtualFile(modalTemplates, "/vendor/acme/acme_twig/components/Modal/index.html.twig");

        assertContainsElements(UxUtil.getTemplateComponentNames((TwigFile) PsiManager.getInstance(getProject()).findFile(primaryFile.getVirtualFile())), "Acme:Button:Primary");
        assertContainsElements(UxUtil.getTemplateComponentNames((TwigFile) PsiManager.getInstance(getProject()).findFile(cardFile.getVirtualFile())), "Acme:Card");
        assertContainsElements(UxUtil.getTemplateComponentNames((TwigFile) PsiManager.getInstance(getProject()).findFile(localButtonFile.getVirtualFile())), "Acme:Button");
        assertContainsElements(UxUtil.getTemplateComponentNames((TwigFile) PsiManager.getInstance(getProject()).findFile(modalFile.getVirtualFile())), "Acme:Modal");
    }

    public void testVisitComponentTemplateProps() {
        // Test: props with defaults
        TwigFile twigFile = (TwigFile) myFixture.configureByText(
            TwigFileType.INSTANCE,
            "{% props icon = null, type = 'primary' %}"
        );

        List<String> props = new ArrayList<>();
        UxUtil.visitComponentTemplateProps(twigFile, pair -> props.add(pair.getFirst()));
        assertContainsElements(props, "icon", "type");

        // Test: multiple props without defaults
        TwigFile twigFile2 = (TwigFile) myFixture.configureByText(
            TwigFileType.INSTANCE,
            "{% props icon_foobar, icon_foobar2 %}"
        );

        List<String> props2 = new ArrayList<>();
        UxUtil.visitComponentTemplateProps(twigFile2, pair -> props2.add(pair.getFirst()));
        assertContainsElements(props2, "icon_foobar", "icon_foobar2");

        // Test: single prop with underscore
        TwigFile twigFile3 = (TwigFile) myFixture.configureByText(
            TwigFileType.INSTANCE,
            "{% props icon_foobar_2 %}"
        );

        List<String> props3 = new ArrayList<>();
        UxUtil.visitComponentTemplateProps(twigFile3, pair -> props3.add(pair.getFirst()));
        assertContainsElements(props3, "icon_foobar_2");

        // Test: prop with string default
        TwigFile twigFile4 = (TwigFile) myFixture.configureByText(
            TwigFileType.INSTANCE,
            "{% props icon_foobar_4 = 'aaa' %}"
        );

        List<String> props4 = new ArrayList<>();
        UxUtil.visitComponentTemplateProps(twigFile4, pair -> props4.add(pair.getFirst()));
        assertContainsElements(props4, "icon_foobar_4");
    }

    /**
     * Test that visitComponentTemplateProps returns PsiElements that can be navigated to.
     */
    public void testVisitComponentTemplatePropsReturnsPsiElements() {
        TwigFile twigFile = (TwigFile) myFixture.configureByText(
            TwigFileType.INSTANCE,
            "{% props icon, type %}"
        );

        List<Pair<String, PsiElement>> propPairs = new ArrayList<>();
        UxUtil.visitComponentTemplateProps(twigFile, propPairs::add);

        assertEquals("Should have 2 props", 2, propPairs.size());

        for (Pair<String, PsiElement> pair : propPairs) {
            assertNotNull("Prop name should not be null", pair.getFirst());
            assertNotNull("Prop PsiElement should not be null", pair.getSecond());
            assertTrue("Prop name should not be blank", !pair.getFirst().isBlank());
        }

        // Verify prop names
        List<String> propNames = propPairs.stream().map(Pair::getFirst).collect(Collectors.toList());
        assertContainsElements(propNames, "icon", "type");
    }

    /**
     * Test that visitComponentTemplateProps handles complex prop names.
     */
    public void testVisitComponentTemplatePropsWithComplexNames() {
        TwigFile twigFile = (TwigFile) myFixture.configureByText(
            TwigFileType.INSTANCE,
            "{% props icon_type, messageText, is_active, data_id %}"
        );

        List<String> props = new ArrayList<>();
        UxUtil.visitComponentTemplateProps(twigFile, pair -> props.add(pair.getFirst()));

        assertContainsElements(props, "icon_type", "messageText", "is_active", "data_id");
        assertEquals("Should have 4 props", 4, props.size());
    }

    /**
     * Test that visitComponentTemplateProps handles props with various default values.
     */
    public void testVisitComponentTemplatePropsWithVariousDefaults() {
        // Test with null default
        TwigFile twigFile1 = (TwigFile) myFixture.configureByText(
            TwigFileType.INSTANCE,
            "{% props icon = null %}"
        );

        List<String> props1 = new ArrayList<>();
        UxUtil.visitComponentTemplateProps(twigFile1, pair -> props1.add(pair.getFirst()));
        assertContainsElements(props1, "icon");

        // Test with numeric default
        TwigFile twigFile2 = (TwigFile) myFixture.configureByText(
            TwigFileType.INSTANCE,
            "{% props count = 0 %}"
        );

        List<String> props2 = new ArrayList<>();
        UxUtil.visitComponentTemplateProps(twigFile2, pair -> props2.add(pair.getFirst()));
        assertContainsElements(props2, "count");

        // Test with boolean default
        TwigFile twigFile3 = (TwigFile) myFixture.configureByText(
            TwigFileType.INSTANCE,
            "{% props enabled = true %}"
        );

        List<String> props3 = new ArrayList<>();
        UxUtil.visitComponentTemplateProps(twigFile3, pair -> props3.add(pair.getFirst()));
        assertContainsElements(props3, "enabled");
    }

    /**
     * Test that visitComponentTemplateProps ignores non-props tags.
     */
    public void testVisitComponentTemplatePropsIgnoresOtherTags() {
        TwigFile twigFile = (TwigFile) myFixture.configureByText(
            TwigFileType.INSTANCE,
            "{% set foo = 'bar' %}\n" +
            "{% props icon, type %}\n" +
            "{% if icon %}{{ icon }}{% endif %}"
        );

        List<String> props = new ArrayList<>();
        UxUtil.visitComponentTemplateProps(twigFile, pair -> props.add(pair.getFirst()));

        // Should only find props, not other variables
        assertContainsElements(props, "icon", "type");
        assertEquals("Should only have 2 props", 2, props.size());
    }

    public void testGetComponentTemplatePropDefaults() {
        // defaults are read as written; required props (no "=") are excluded
        assertEquals(
            Map.of("open", "false", "variant", "'brand'", "as", "'div'"),
            propDefaults("{%- props open = false, variant = 'brand', as = 'div' -%}")
        );

        Map<String, String> withRequired = propDefaults("{% props id, open = false %}");
        assertEquals("false", withRequired.get("open"));
        assertFalse("required prop without a default is excluded", withRequired.containsKey("id"));

        // commas inside arrays and strings must not split props
        Map<String, String> nested = propDefaults("{% props items = ['x', 'y'], label = 'a, b', open = false %}");
        assertEquals("['x', 'y']", nested.get("items"));
        assertEquals("'a, b'", nested.get("label"));
        assertEquals("false", nested.get("open"));

        // bare literals stay bare
        assertEquals(Map.of("count", "0", "name", "null"), propDefaults("{% props count = 0, name = null %}"));

        // non-literal default expression is kept verbatim
        assertEquals("foo ~ bar", propDefaults("{% props label = foo ~ bar %}").get("label"));

        // no props tag / other tags contribute nothing
        assertTrue(propDefaults("<div>{{ foo }}</div>").isEmpty());
        assertTrue(propDefaults("{% set foo = 'bar' %}").isEmpty());
    }

    private Map<String, String> propDefaults(@NotNull String source) {
        TwigFile twigFile = (TwigFile) myFixture.configureByText(TwigFileType.INSTANCE, source);
        return UxUtil.getComponentTemplatePropDefaults(twigFile);
    }

    private void configureTwigNamespaceSettings(@NotNull TwigNamespaceSetting... settings) {
        Settings.getInstance(getProject()).twigNamespaces.clear();
        Settings.getInstance(getProject()).twigNamespaces.addAll(List.of(settings));
    }

    private void configureContainerXml(@NotNull String services) {
        String path = "var/cache/dev/" + getTestName(false) + "Container.xml";
        createFileInProjectRoot(path, "<?xml version=\"1.0\" encoding=\"utf-8\"?><container><services>" + services + "</services></container>");
        Settings.getInstance(getProject()).containerFiles = List.of(new ContainerFile(path));
        SymfonyVarDirectoryWatcherKt.getSymfonyVarDirectoryWatcher(getProject()).reloadConfiguration();
    }

    private static void assertContainsVirtualFile(@NotNull Collection<PsiFile> psiFiles, @NotNull String pathSuffix) {
        assertTrue("Expected path suffix " + pathSuffix + " in " + psiFiles.stream()
                .map(psiFile -> psiFile.getVirtualFile() != null ? psiFile.getVirtualFile().getPath() : "")
                .collect(Collectors.toList()),
            psiFiles.stream()
            .map(psiFile -> psiFile.getVirtualFile() != null ? psiFile.getVirtualFile().getPath() : "")
            .anyMatch(path -> path.endsWith(pathSuffix)));
    }

    private static void assertDoesNotContainVirtualFile(@NotNull Collection<PsiFile> psiFiles, @NotNull String pathSuffix) {
        assertFalse("Unexpected path suffix " + pathSuffix + " in " + psiFiles.stream()
                .map(psiFile -> psiFile.getVirtualFile() != null ? psiFile.getVirtualFile().getPath() : "")
                .collect(Collectors.toList()),
            psiFiles.stream()
            .map(psiFile -> psiFile.getVirtualFile() != null ? psiFile.getVirtualFile().getPath() : "")
            .anyMatch(path -> path.endsWith(pathSuffix)));
    }
}
