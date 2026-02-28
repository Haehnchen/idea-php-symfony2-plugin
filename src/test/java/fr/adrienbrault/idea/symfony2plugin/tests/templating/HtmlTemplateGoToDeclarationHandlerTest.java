package fr.adrienbrault.idea.symfony2plugin.tests.templating;

import com.intellij.psi.PsiElement;
import com.intellij.patterns.PlatformPatterns;
import com.jetbrains.twig.TwigFile;
import com.jetbrains.twig.TwigFileType;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import fr.adrienbrault.idea.symfony2plugin.util.UxUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.templating.HtmlTemplateGoToDeclarationHandler
 */
public class HtmlTemplateGoToDeclarationHandlerTest extends SymfonyLightCodeInsightFixtureTestCase {
    public String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/templating/fixtures";
    }

    public void setUp() throws Exception {
        super.setUp();
    }

    /**
     * Test that navigation pattern matches for twig component attributes.
     */
    public void testTwigComponentAttributePatternMatches() {
        myFixture.copyFileToProject("PropsAlert.php", "src/PropsAlert.php");
        myFixture.copyFileToProject("twig_component.yaml", "config/packages/twig_component.yaml");
        myFixture.copyFileToProject("ide-twig.json", "ide-twig.json");
        myFixture.copyFileToProject("PropsAlert.html.twig", "templates/components/PropsAlert.html.twig");

        assertNavigationMatch(
            TwigFileType.INSTANCE,
            "<twig:PropsAlert i<caret>con=\"test\" />",
            PlatformPatterns.psiElement().withText("icon")
        );
    }

    public void testNavigationForAnonymousDirectoryIndexComponent() {
        myFixture.copyFileToProject("ide-twig.json", "ide-twig.json");
        myFixture.addFileToProject("templates/components/Nav/index.html.twig", "<nav></nav>");

        assertNavigationMatch(
            TwigFileType.INSTANCE,
            "<twig:Na<caret>v />",
            PlatformPatterns.psiElement(TwigFile.class)
        );
    }

    /**
     * Test that navigation to {% props %} works when using component template directly.
     */
    public void testNavigationToPropsFromComponentTemplate() {
        // Setup configuration files
        myFixture.copyFileToProject("twig_component.yaml", "config/packages/twig_component.yaml");
        myFixture.copyFileToProject("ide-twig.json", "ide-twig.json");

        // Component template with props - this is the target we want to navigate to
        TwigFile componentTemplate = (TwigFile) myFixture.configureByText(
            "PropsAlert.html.twig",
            "{% props icon, type, message = 'Default message' %}\n" +
            "<div class=\"alert alert-{{ type }}\">\n" +
            "    {% if icon %}<i class=\"icon-{{ icon }}\"></i>{% endif %}\n" +
            "    {{ message }}\n" +
            "</div>"
        );

        // Verify that props can be extracted from the template
        List<String> props = new ArrayList<>();
        UxUtil.visitComponentTemplateProps(componentTemplate, pair -> props.add(pair.getFirst()));

        // Verify props are found
        assertTrue("Props should be extracted from template", props.contains("icon"));
        assertTrue("Props should be extracted from template", props.contains("type"));
        assertTrue("Props should be extracted from template", props.contains("message"));
    }

    /**
     * Test that navigation works for props with default values.
     */
    public void testNavigationToPropsWithDefaultValues() {
        TwigFile componentTemplate = (TwigFile) myFixture.configureByText(
            "ComponentWithDefaults.html.twig",
            "{% props icon = null, type = 'primary', message = 'Hello' %}"
        );

        List<String> props = new ArrayList<>();
        UxUtil.visitComponentTemplateProps(componentTemplate, pair -> props.add(pair.getFirst()));

        assertContainsElements(props, "icon", "type", "message");
    }

    /**
     * Test that navigation works for props without default values.
     */
    public void testNavigationToPropsWithoutDefaultValues() {
        TwigFile componentTemplate = (TwigFile) myFixture.configureByText(
            "ComponentWithoutDefaults.html.twig",
            "{% props icon, type, message %}"
        );

        List<String> props = new ArrayList<>();
        UxUtil.visitComponentTemplateProps(componentTemplate, pair -> props.add(pair.getFirst()));

        assertContainsElements(props, "icon", "type", "message");
    }

    /**
     * Test that navigation returns PsiElement for each prop.
     */
    public void testNavigationReturnsPsiElementForProps() {
        TwigFile componentTemplate = (TwigFile) myFixture.configureByText(
            "ComponentWithPsi.html.twig",
            "{% props icon, type %}"
        );

        List<PsiElement> propElements = new ArrayList<>();
        UxUtil.visitComponentTemplateProps(componentTemplate, pair -> propElements.add(pair.getSecond()));

        // Must have exactly 2 prop PsiElements
        assertEquals("Should have 2 prop PsiElements", 2, propElements.size());

        // Each prop must have a valid PsiElement with parent
        PsiElement iconElement = propElements.get(0);
        PsiElement typeElement = propElements.get(1);

        assertNotNull("icon PsiElement should not be null", iconElement);
        assertNotNull("icon PsiElement should have a parent", iconElement.getParent());

        assertNotNull("type PsiElement should not be null", typeElement);
        assertNotNull("type PsiElement should have a parent", typeElement.getParent());
    }
}
