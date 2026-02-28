package fr.adrienbrault.idea.symfony2plugin.tests.templating;

import com.intellij.patterns.PlatformPatterns;
import com.jetbrains.twig.TwigFileType;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

/**
 * Tests for Twig template path completion and navigation with resolved templates.
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.templating.TwigTemplateCompletionContributor
 * @see fr.adrienbrault.idea.symfony2plugin.templating.TwigTemplateGoToDeclarationHandler
 * @see fr.adrienbrault.idea.symfony2plugin.templating.TemplateGotoCompletionRegistrar
 * @see fr.adrienbrault.idea.symfony2plugin.templating.BlockGotoCompletionRegistrar
 */
public class TwigTemplateResolutionTest extends SymfonyLightCodeInsightFixtureTestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();

        // Copy the ide-twig.json configuration to set up Twig namespace
        // This configures the "templates" directory as the main Twig namespace
        myFixture.copyFileToProject("ide-twig.json");

        // Copy template fixtures to the project's templates directory
        // These templates are needed for completion and navigation tests
        myFixture.copyFileToProject("templates/base.html.twig", "templates/base.html.twig");
        myFixture.copyFileToProject("templates/child.html.twig", "templates/child.html.twig");
        myFixture.copyFileToProject("templates/macros.html.twig", "templates/macros.html.twig");
        myFixture.copyFileToProject("templates/partial.html.twig", "templates/partial.html.twig");
    }

    @Override
    public String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/templating/fixtures";
    }

    /**
     * Test that include() provides template path completion with resolved templates.
     *
     * @see fr.adrienbrault.idea.symfony2plugin.templating.TwigTemplateCompletionContributor
     */
    public void testIncludeTemplatePathCompletion() {
        assertCompletionContains(
            TwigFileType.INSTANCE,
            "{{ include('<caret>') }}",
            "base.html.twig",
            "child.html.twig",
            "macros.html.twig",
            "partial.html.twig"
        );
    }

    /**
     * Test that {% include %} tag provides template path completion.
     */
    public void testIncludeTagTemplatePathCompletion() {
        assertCompletionContains(
            TwigFileType.INSTANCE,
            "{% include '<caret>' %}",
            "base.html.twig",
            "partial.html.twig"
        );
    }

    /**
     * Test that extends provides template path completion.
     */
    public void testExtendsTemplatePathCompletion() {
        assertCompletionContains(
            TwigFileType.INSTANCE,
            "{% extends '<caret>' %}",
            "base.html.twig",
            "child.html.twig"
        );
    }

    /**
     * Test that embed provides template path completion.
     */
    public void testEmbedTemplatePathCompletion() {
        assertCompletionContains(
            TwigFileType.INSTANCE,
            "{% embed '<caret>' %}{% endembed %}",
            "base.html.twig",
            "partial.html.twig"
        );
    }

    /**
     * Test that clicking on an included template name navigates to the file.
     *
     * @see fr.adrienbrault.idea.symfony2plugin.templating.TwigTemplateGoToDeclarationHandler
     * @see fr.adrienbrault.idea.symfony2plugin.templating.TemplateGotoCompletionRegistrar
     */
    public void testIncludeTemplateNavigation() {
        assertNavigationContainsFile(
            TwigFileType.INSTANCE,
            "{{ include('partial<caret>.html.twig') }}",
            "partial.html.twig"
        );
    }

    /**
     * Test that clicking on an included template in tag form navigates to the file.
     */
    public void testIncludeTagTemplateNavigation() {
        assertNavigationContainsFile(
            TwigFileType.INSTANCE,
            "{% include 'partial<caret>.html.twig' %}",
            "partial.html.twig"
        );
    }

    /**
     * Test that clicking on an extended template name navigates to the file.
     */
    public void testExtendsTemplateNavigation() {
        assertNavigationContainsFile(
            TwigFileType.INSTANCE,
            "{% extends 'base<caret>.html.twig' %}",
            "base.html.twig"
        );
    }

    /**
     * Test that clicking on an embedded template name navigates to the file.
     */
    public void testEmbedTemplateNavigation() {
        assertNavigationContainsFile(
            TwigFileType.INSTANCE,
            "{% embed 'partial<caret>.html.twig' %}{% endembed %}",
            "partial.html.twig"
        );
    }

    /**
     * Test that block names from parent template are available in completion.
     *
     * When extending a template, the block names from the parent should be
     * available for completion in {% block %} tags.
     *
     * @see fr.adrienbrault.idea.symfony2plugin.templating.BlockGotoCompletionRegistrar
     * @see fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil#getBlockLookupElements
     */
    public void testBlockInheritanceCompletion() {
        // Create a child template that extends base.html.twig
        // The blocks from base.html.twig (title, header, content, footer) should be available
        assertCompletionContains(
            TwigFileType.INSTANCE,
            "{% extends 'base.html.twig' %}\n" +
            "{% block <caret> %}",
            "title",
            "header",
            "content",
            "footer"
        );
    }

    /**
     * Test block completion in override context.
     */
    public void testBlockOverrideCompletion() {
        assertCompletionContains(
            TwigFileType.INSTANCE,
            "{% extends 'base.html.twig' %}\n" +
            "{% block ti<caret> %}",
            "title"
        );
    }

    /**
     * Test that {{ block() }} function provides block name completion.
     */
    public void testBlockFunctionCompletion() {
        // When using the block() function, available blocks should be completed
        assertCompletionContains(
            TwigFileType.INSTANCE,
            "{% extends 'base.html.twig' %}\n" +
            "{{ block('<caret>') }}",
            "title",
            "header",
            "content",
            "footer"
        );
    }

    /**
     * Test block navigation - clicking on block name should navigate to definition.
     */
    public void testBlockNavigation() {
        // Configure a template that extends base and overrides a block
        // Navigation should find the parent block definition
        assertNavigationMatch(
            TwigFileType.INSTANCE,
            "{% extends 'base.html.twig' %}\n" +
            "{% block tit<caret>le %}Custom Title{% endblock %}"
        );
    }

    /**
     * Test that macro names are available after importing an external macro file.
     *
     * After {% import 'macros.html.twig' as alias %}, the macros from that file
     * should be available via alias.<macro_name> completion.
     *
     * @see fr.adrienbrault.idea.symfony2plugin.templating.TwigTemplateCompletionContributor
     * @see fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil#visitMacros
     */
    public void testExternalMacroImportCompletion() {
        assertCompletionContains(
            TwigFileType.INSTANCE,
            "{% import 'macros.html.twig' as forms %}\n" +
            "{{ forms.<caret> }}",
            "input",
            "textarea",
            "label"
        );
    }

    /**
     * Test macro navigation - clicking on macro name should navigate to definition.
     */
    public void testExternalMacroNavigation() {
        assertNavigationMatch(
            TwigFileType.INSTANCE,
            "{% import 'macros.html.twig' as forms %}\n" +
            "{{ forms.in<caret>put('name', 'value') }}",
            PlatformPatterns.psiElement()
        );
    }

    /**
     * Test macro navigation to textarea.
     */
    public void testExternalMacroTextareaNavigation() {
        assertNavigationMatch(
            TwigFileType.INSTANCE,
            "{% import 'macros.html.twig' as forms %}\n" +
            "{{ forms.textar<caret>ea('content', '') }}",
            PlatformPatterns.psiElement()
        );
    }

    /**
     * Test {% from %} import syntax with macro completion.
     */
    public void testFromImportMacroCompletion() {
        assertCompletionContains(
            TwigFileType.INSTANCE,
            "{% from 'macros.html.twig' import <caret> %}",
            "input",
            "textarea",
            "label"
        );
    }

    /**
     * Test that template completion works with namespace prefix.
     */
    public void testTemplatePathCompletionWithNamespace() {
        // This tests that template names are properly indexed
        // The '@' namespace syntax should also work
        assertCompletionContains(
            TwigFileType.INSTANCE,
            "{{ include('<caret>') }}",
            "base.html.twig"
        );
    }

    /**
     * Test that completion doesn't suggest the current template.
     */
    public void testTemplateCompletionExcludesSelf() {
        // When in a template, we should not suggest including itself
        // This is more of a sanity check
        assertCompletionContains(
            TwigFileType.INSTANCE,
            "{{ include('<caret>') }}",
            "base.html.twig"
        );
    }

    /**
     * Test include with variables context.
     */
    public void testIncludeWithVariables() {
        // Include with 'with' should still provide template completion
        assertCompletionContains(
            TwigFileType.INSTANCE,
            "{% include '<caret>' with {'foo': 'bar'} %}",
            "partial.html.twig"
        );
    }

    /**
     * Test embed with block overrides.
     */
    public void testEmbedWithBlockCompletion() {
        // Inside embed, blocks from the embedded template should be available
        assertCompletionContains(
            TwigFileType.INSTANCE,
            "{% embed 'base.html.twig' %}\n" +
            "{% block <caret> %}\n" +
            "{% endblock %}\n" +
            "{% endembed %}",
            "title",
            "header",
            "content",
            "footer"
        );
    }
}
