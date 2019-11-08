package fr.adrienbrault.idea.symfony2plugin.tests.templating.inspection;

import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.templating.inspection.TwigTemplateMissingInspection
 */
public class TwigTemplateMissingInspectionTest extends SymfonyLightCodeInsightFixtureTestCase {
    public void testThatUnknownTemplatesAreHighlighted() {
        assertLocalInspectionContains(
            "test.html.twig",
            "{% include 'f<caret>.html.twig' %}",
            "Twig: Missing Template"
        );

        assertLocalInspectionContains(
            "test.html.twig",
            "{{ include('f<caret>.html.twig') }}",
            "Twig: Missing Template"
        );

        assertLocalInspectionContains(
            "test.html.twig",
            "{{ source('f<caret>.html.twig') }}",
            "Twig: Missing Template"
        );

        assertLocalInspectionContains(
            "test.html.twig",
            "{% extends 'f<caret>.html.twig' %}",
            "Twig: Missing Template"
        );

        assertLocalInspectionContains(
            "test.html.twig",
            "{% import 'f<caret>.html.twig' %}",
            "Twig: Missing Template"
        );
    }

    public void testThatInvalidTemplateNamesAreNotHighlighted() {
        assertLocalInspectionNotContains(
            "test.html.twig",
            "{% include \"foo/\" ~ segment.typeKey ~ \".ht<caret>ml.twig\" %}",
            "Twig: Missing Template"
        );

        assertLocalInspectionNotContains(
            "test.html.twig",
            "{% include \"fo<caret>o/\" ~ segment.typeKey ~ \".html.twig\" %}",
            "Twig: Missing Template"
        );

        assertLocalInspectionNotContains(
            "test.html.twig",
            "{% include 'fo<caret>#{segment}.html.twig' %}",
            "Twig: Missing Template"
        );

        assertLocalInspectionNotContains(
            "test.html.twig",
            "{% include 'fo<caret>#{segment.typeKey}.html.twig' %}",
            "Twig: Missing Template"
        );
    }
}
