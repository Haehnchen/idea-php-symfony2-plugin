package fr.adrienbrault.idea.symfony2plugin.tests.twig.icon;

import com.intellij.psi.PsiFile;
import com.intellij.ui.LayeredIcon;
import fr.adrienbrault.idea.symfony2plugin.twig.icon.TwigIconProvider;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see TwigIconProvider
 */
public class TwigIconProviderTest extends SymfonyLightCodeInsightFixtureTestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();

        // Copy the ide-twig.json configuration to set up Twig namespace
        myFixture.copyFileToProject("ide-twig.json");
    }

    @Override
    public String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/templating/fixtures";
    }

    /**
     * Test that templates included via {% include %} show the implements badge icon.
     */
    public void testIncludeTagShowsImplementsBadge() {
        // Create a template that includes partial.html.twig
        myFixture.addFileToProject(
            "templates/including_test.html.twig",
            "{% include 'partial.html.twig' %}"
        );

        // Create the partial template
        PsiFile partialFile = myFixture.addFileToProject(
            "templates/partial.html.twig",
            "<div>Partial content</div>"
        );

        // Get the icon from the provider
        Icon icon = getIconFromProvider(partialFile);

        // Should return a LayeredIcon with the TWIG_IMPLEMENTS_FILE badge
        assertNotNull("Icon should not be null for included template", icon);
        assertTrue("Icon should be a LayeredIcon for included template", icon instanceof LayeredIcon);
    }

    /**
     * Test that templates included via {{ include() }} show the implements badge icon.
     */
    public void testIncludeFunctionShowsImplementsBadge() {
        // Create a template that includes partial_func.html.twig via function
        myFixture.addFileToProject(
            "templates/including_func_test.html.twig",
            "{{ include('partial_func.html.twig') }}"
        );

        // Create the partial template
        PsiFile partialFile = myFixture.addFileToProject(
            "templates/partial_func.html.twig",
            "<div>Partial content</div>"
        );

        // Get the icon from the provider
        Icon icon = getIconFromProvider(partialFile);

        // Should return a LayeredIcon with the TWIG_IMPLEMENTS_FILE badge
        assertNotNull("Icon should not be null for included template", icon);
        assertTrue("Icon should be a LayeredIcon for included template", icon instanceof LayeredIcon);
    }

    /**
     * Test that templates included via {{ source() }} show the implements badge icon.
     */
    public void testSourceFunctionShowsImplementsBadge() {
        // Create a template that sources source_test.html.twig
        myFixture.addFileToProject(
            "templates/sourcing_test.html.twig",
            "{{ source('source_test.html.twig') }}"
        );

        // Create the sourced template
        PsiFile sourceFile = myFixture.addFileToProject(
            "templates/source_test.html.twig",
            "<div>Source content</div>"
        );

        // Get the icon from the provider
        Icon icon = getIconFromProvider(sourceFile);

        // Should return a LayeredIcon with the TWIG_IMPLEMENTS_FILE badge
        assertNotNull("Icon should not be null for sourced template", icon);
        assertTrue("Icon should be a LayeredIcon for sourced template", icon instanceof LayeredIcon);
    }

    /**
     * Test that templates NOT included by others do NOT show the implements badge.
     */
    public void testStandaloneTemplateDoesNotShowImplementsBadge() {
        // Create a standalone template that is not included by any other
        PsiFile standaloneFile = myFixture.addFileToProject(
            "templates/standalone.html.twig",
            "<div>Standalone content</div>"
        );

        // Get the icon from the provider
        Icon icon = getIconFromProvider(standaloneFile);

        // Should return null (no overlay needed for standalone template without controller/extends)
        assertNull("Icon should be null for standalone template without special features", icon);
    }

    /**
     * Test that templates with extends tag show the extends badge.
     */
    public void testExtendsTemplateShowsExtendsBadge() {
        // Create a base template
        myFixture.addFileToProject(
            "templates/base.html.twig",
            "<html><body>{% block content %}{% endblock %}</body></html>"
        );

        // Create a child template that extends base
        PsiFile childFile = myFixture.addFileToProject(
            "templates/child.html.twig",
            "{% extends 'base.html.twig' %}\n{% block content %}Child{% endblock %}"
        );

        // Get the icon from the provider
        Icon icon = getIconFromProvider(childFile);

        // Should return a LayeredIcon with the TWIG_EXTENDS_FILE badge
        assertNotNull("Icon should not be null for extending template", icon);
        assertTrue("Icon should be a LayeredIcon for extending template", icon instanceof LayeredIcon);
    }

    /**
     * Test that a template that is both included and extends shows both badges.
     */
    public void testIncludedAndExtendingTemplateShowsBothBadges() {
        // Create a base template
        myFixture.addFileToProject(
            "templates/layout.html.twig",
            "<html><body>{% block content %}{% endblock %}</body></html>"
        );

        // Create a template that includes child_layout.html.twig
        myFixture.addFileToProject(
            "templates/parent.html.twig",
            "{% include 'child_layout.html.twig' %}"
        );

        // Create a child template that extends layout AND is included by parent
        PsiFile childFile = myFixture.addFileToProject(
            "templates/child_layout.html.twig",
            "{% extends 'layout.html.twig' %}\n{% block content %}Content{% endblock %}"
        );

        // Get the icon from the provider
        Icon icon = getIconFromProvider(childFile);

        // Should return a LayeredIcon with both badges
        assertNotNull("Icon should not be null", icon);
        assertTrue("Icon should be a LayeredIcon", icon instanceof LayeredIcon);
    }

    /**
     * Get icon from TwigIconProvider for the given PsiFile.
     */
    private Icon getIconFromProvider(@NotNull PsiFile psiFile) {
        TwigIconProvider provider = new TwigIconProvider();
        return provider.getIcon(psiFile, 0);
    }
}
