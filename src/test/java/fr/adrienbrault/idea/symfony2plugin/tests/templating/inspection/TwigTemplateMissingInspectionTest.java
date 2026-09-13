package fr.adrienbrault.idea.symfony2plugin.tests.templating.inspection;

import fr.adrienbrault.idea.symfony2plugin.templating.inspection.TwigTemplateMissingInspection;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.templating.inspection.TwigTemplateMissingInspection
 */
public class TwigTemplateMissingInspectionTest extends SymfonyLightCodeInsightFixtureTestCase {
    public void testThatUnknownTemplatesAreHighlighted() {
        assertLocalInspectionContains(TwigTemplateMissingInspection.class,
            "test.html.twig",
            "{% include 'f<caret>.html.twig' %}",
            "Twig: Missing Template"
        );

        assertLocalInspectionContains(TwigTemplateMissingInspection.class,
            "test.html.twig",
            "{{ include('f<caret>.html.twig') }}",
            "Twig: Missing Template"
        );

        assertLocalInspectionContains(TwigTemplateMissingInspection.class,
            "test.html.twig",
            "{{ source('f<caret>.html.twig') }}",
            "Twig: Missing Template"
        );

        assertLocalInspectionContains(TwigTemplateMissingInspection.class,
            "test.html.twig",
            "{% extends 'f<caret>.html.twig' %}",
            "Twig: Missing Template"
        );

        assertLocalInspectionContains(TwigTemplateMissingInspection.class,
            "test.html.twig",
            "{% import 'f<caret>.html.twig' %}",
            "Twig: Missing Template"
        );
    }

    public void testThatInvalidTemplateNamesAreNotHighlighted() {
        assertLocalInspectionNotContains(TwigTemplateMissingInspection.class,
            "test.html.twig",
            "{% include \"foo/\" ~ segment.typeKey ~ \".ht<caret>ml.twig\" %}",
            "Twig: Missing Template"
        );

        assertLocalInspectionNotContains(TwigTemplateMissingInspection.class,
            "test.html.twig",
            "{% include \"fo<caret>o/\" ~ segment.typeKey ~ \".html.twig\" %}",
            "Twig: Missing Template"
        );

        assertLocalInspectionNotContains(TwigTemplateMissingInspection.class,
            "test.html.twig",
            "{% include 'fo<caret>#{segment}.html.twig' %}",
            "Twig: Missing Template"
        );

        assertLocalInspectionNotContains(TwigTemplateMissingInspection.class,
            "test.html.twig",
            "{% include 'fo<caret>#{segment.typeKey}.html.twig' %}",
            "Twig: Missing Template"
        );
    }

}
