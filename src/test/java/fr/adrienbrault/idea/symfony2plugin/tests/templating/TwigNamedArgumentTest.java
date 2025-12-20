package fr.adrienbrault.idea.symfony2plugin.tests.templating;

import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.jetbrains.php.lang.psi.elements.Parameter;
import com.jetbrains.twig.TwigFileType;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.templating.TwigNamedArgumentReferenceContributor
 * @see fr.adrienbrault.idea.symfony2plugin.templating.TwigTemplateCompletionContributor.TwigNamedArgumentCompletionProvider
 */
public class TwigNamedArgumentTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void setUp() throws Exception {
        super.setUp();
        myFixture.copyFileToProject("TwigNamedArgumentTest.php");
    }

    public String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/templating/fixtures";
    }

    /**
     * Test completion for named arguments in filter calls
     */
    public void testFilterNamedArgumentCompletion() {
        // Test that 'timezone' parameter is suggested after positional argument
        assertCompletionContains(
            TwigFileType.INSTANCE,
            "{{ 'now'|date('Y-m-d', time<caret>) }}",
            "timezone"
        );

        // Test multiple parameter suggestions
        assertCompletionContains(
            TwigFileType.INSTANCE,
            "{{ 'text'|convert_encoding(fr<caret>) }}",
            "from"
        );
    }

    /**
     * Test completion for named arguments in function calls
     */
    public void testFunctionNamedArgumentCompletion() {
        // Test range function parameters
        assertCompletionContains(
            TwigFileType.INSTANCE,
            "{{ range(1, 10, st<caret>) }}",
            "step"
        );

        // Test all parameters are available
        assertCompletionContains(
            TwigFileType.INSTANCE,
            "{{ range(lo<caret>) }}",
            "low"
        );
    }

    /**
     * Test navigation from named argument to parameter declaration
     */
    public void testNamedArgumentNavigation() {
        // Navigate from 'timezone' to parameter in PHP method
        assertNavigationMatch(
            TwigFileType.INSTANCE,
            "{{ 'now'|date(time<caret>zone: 'UTC') }}",
            PlatformPatterns.psiElement(Parameter.class)
        );

        // Navigate from 'step' to parameter
        assertNavigationMatch(
            TwigFileType.INSTANCE,
            "{{ range(low: 1, high: 10, st<caret>ep: 2) }}",
            PlatformPatterns.psiElement(Parameter.class)
        );
    }

    /**
     * Test that already-used parameters are not suggested
     */
    public void testAlreadyUsedParametersNotSuggested() {
        // After using 'low', it should not be suggested again
        assertCompletionNotContains(
            TwigFileType.INSTANCE,
            "{{ range(low: 1, lo<caret>) }}",
            "low"
        );

        // But other parameters should still be available
        assertCompletionContains(
            TwigFileType.INSTANCE,
            "{{ range(low: 1, hi<caret>) }}",
            "high"
        );
    }

    /**
     * Test mixed positional and named arguments
     */
    public void testMixedPositionalAndNamedArguments() {
        // Positional + named in filter
        assertCompletionContains(
            TwigFileType.INSTANCE,
            "{{ 'now'|date('Y-m-d', time<caret>) }}",
            "timezone"
        );

        // Multiple positional + named in function
        assertCompletionContains(
            TwigFileType.INSTANCE,
            "{{ range(1, 10, st<caret>) }}",
            "step"
        );
    }

    /**
     * Test that filters skip the piped value parameter
     */
    public void testFilterSkipsPipedParameter() {
        // 'value' parameter should not be suggested for filters
        // (it's automatically passed via the pipe)
        assertCompletionNotContains(
            TwigFileType.INSTANCE,
            "{{ 'now'|date(val<caret>) }}",
            "value"
        );

        // But 'format' and 'timezone' should be available
        assertCompletionContains(
            TwigFileType.INSTANCE,
            "{{ 'now'|date(form<caret>) }}",
            "format"
        );
    }
}
