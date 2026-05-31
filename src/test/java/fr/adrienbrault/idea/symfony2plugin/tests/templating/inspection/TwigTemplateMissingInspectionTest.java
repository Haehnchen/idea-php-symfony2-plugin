package fr.adrienbrault.idea.symfony2plugin.tests.templating.inspection;

import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import fr.adrienbrault.idea.symfony2plugin.tests.templating.TestTwigFileUsage;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.templating.inspection.TwigTemplateMissingInspection
 */
public class TwigTemplateMissingInspectionTest extends SymfonyLightCodeInsightFixtureTestCase {
    @Override
    public void setUp() throws Exception {
        super.setUp();

        TwigUtil.TWIG_FILE_USAGE_EXTENSIONS.getPoint().registerExtension(new TestTwigFileUsage(), getTestRootDisposable());
    }

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

    public void testThatExternalTemplateUsageUnknownTemplatesAreHighlighted() {
        assertLocalInspectionContains(
            "test.html.twig",
            "{% custom_template 'missing<caret>.html.twig' %}",
            "Twig: Missing Template"
        );
    }

    public void testThatExternalTemplateUsageExistingTemplatesAreNotHighlighted() {
        myFixture.addFileToProject("ide-twig.json", "{ \"namespaces\": [{ \"namespace\": \"\", \"path\": \"templates\" }] }");
        myFixture.addFileToProject("templates/existing.html.twig", "");

        assertLocalInspectionNotContains(
            "test.html.twig",
            "{% custom_template 'existing<caret>.html.twig' %}",
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

    public void testThatExternalTemplateUsageInvalidTemplateNamesAreNotHighlighted() {
        assertLocalInspectionNotContains(
            "test.html.twig",
            "{% custom_template \"foo/\" ~ segment.typeKey ~ \".ht<caret>ml.twig\" %}",
            "Twig: Missing Template"
        );

        assertLocalInspectionNotContains(
            "test.html.twig",
            "{% custom_template 'fo<caret>#{segment}.html.twig' %}",
            "Twig: Missing Template"
        );
    }
}
