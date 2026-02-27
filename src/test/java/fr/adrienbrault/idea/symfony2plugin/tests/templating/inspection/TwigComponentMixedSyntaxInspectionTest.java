package fr.adrienbrault.idea.symfony2plugin.tests.templating.inspection;

import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.templating.inspection.TwigComponentMixedSyntaxInspection
 */
public class TwigComponentMixedSyntaxInspectionTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void testTwigBlockInsideHtmlComponentReportsError() {
        assertLocalInspectionContains(
            "test.html.twig",
            "<twig:Card>\n    {% <caret>block footer %}\n        content\n    {% endblock %}\n</twig:Card>",
            "Cannot use Twig block syntax inside HTML component syntax. Use <twig:block name=\"...\"> instead."
        );
    }

    public void testValidHtmlSyntaxDoesNotReport() {
        assertLocalInspectionNotContains(
            "test.html.twig",
            "<twig:Card>\n    <twig:block name=\"footer\">\n        <caret>content\n    </twig:block>\n</twig:Card>",
            "Cannot use Twig block syntax inside HTML component syntax. Use <twig:block name=\"...\"> instead."
        );
    }

    public void testValidTwigBlockOutsideComponentDoesNotReport() {
        assertLocalInspectionNotContains(
            "test.html.twig",
            "{% extends 'base.html.twig' %}\n{% <caret>block footer %}\n    content\n{% endblock %}",
            "Cannot use Twig block syntax inside HTML component syntax. Use <twig:block name=\"...\"> instead."
        );
    }

    public void testTwigBlockInsideNestedHtmlComponentReportsError() {
        assertLocalInspectionContains(
            "test.html.twig",
            "<twig:Card>\n    <twig:block name=\"body\">\n        <twig:Alert>\n            {% <caret>block content %}\n                inner\n            {% endblock %}\n        </twig:Alert>\n    </twig:block>\n</twig:Card>",
            "Cannot use Twig block syntax inside HTML component syntax. Use <twig:block name=\"...\"> instead."
        );
    }
}
