package fr.adrienbrault.idea.symfony2plugin.tests.util;

import com.intellij.psi.PsiFile;
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
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import fr.adrienbrault.idea.symfony2plugin.util.UxUtil;
import fr.adrienbrault.idea.symfony2plugin.util.dict.TwigComponentNamespace;

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

    public void setUp() throws Exception {
        super.setUp();
        myFixture.configureFromExistingVirtualFile(myFixture.copyFileToProject("UxUtil.php"));
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
}
