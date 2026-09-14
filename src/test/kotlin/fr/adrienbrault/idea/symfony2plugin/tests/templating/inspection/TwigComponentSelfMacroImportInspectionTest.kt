package fr.adrienbrault.idea.symfony2plugin.tests.templating.inspection

import fr.adrienbrault.idea.symfony2plugin.templating.inspection.TwigComponentSelfMacroImportInspection
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.templating.inspection.TwigComponentSelfMacroImportInspection
 */
class TwigComponentSelfMacroImportInspectionTest : SymfonyLightCodeInsightFixtureTestCase() {
    fun testSelfImportInsideHtmlComponentReportsError() {
        assertLocalInspectionContains(TwigComponentSelfMacroImportInspection::class.java,
            "test.html.twig",
            "{% macro message_formatter(message) %}<strong>{{ message }}</strong>{% endmacro %}\n<twig:Alert>\n    {% from <caret>_self import message_formatter %}\n    {{ message_formatter('') }}\n</twig:Alert>",
            "Cannot use '_self' to import macros inside a Twig component. Use the full template path instead."
        )
    }

    fun testSelfImportInsideTwigComponentTagReportsError() {
        assertLocalInspectionContains(TwigComponentSelfMacroImportInspection::class.java,
            "test.html.twig",
            "{% macro message_formatter(message) %}<strong>{{ message }}</strong>{% endmacro %}\n{% component 'Alert' %}\n    {% from <caret>_self import message_formatter %}\n    {{ message_formatter('') }}\n{% endcomponent %}",
            "Cannot use '_self' to import macros inside a Twig component. Use the full template path instead."
        )
    }

    fun testSelfImportOutsideComponentDoesNotReport() {
        assertLocalInspectionNotContains(TwigComponentSelfMacroImportInspection::class.java,
            "test.html.twig",
            "{% macro message_formatter(message) %}<strong>{{ message }}</strong>{% endmacro %}\n{% from <caret>_self import message_formatter %}\n{{ message_formatter('') }}",
            "Cannot use '_self' to import macros inside a Twig component. Use the full template path instead."
        )
    }

    fun testFullPathImportInsideComponentDoesNotReport() {
        assertLocalInspectionNotContains(TwigComponentSelfMacroImportInspection::class.java,
            "test.html.twig",
            "{% macro message_formatter(message) %}<strong>{{ message }}</strong>{% endmacro %}\n<twig:Alert>\n    {% from 'test.html.twig' import message_<caret>formatter %}\n    {{ message_formatter('') }}\n</twig:Alert>",
            "Cannot use '_self' to import macros inside a Twig component. Use the full template path instead."
        )
    }

    fun testSelfImportInsideNestedHtmlComponentReportsError() {
        assertLocalInspectionContains(TwigComponentSelfMacroImportInspection::class.java,
            "test.html.twig",
            "{% macro msg(m) %}{{ m }}{% endmacro %}\n<twig:Card>\n    <twig:Alert>\n        {% from <caret>_self import msg %}\n    </twig:Alert>\n</twig:Card>",
            "Cannot use '_self' to import macros inside a Twig component. Use the full template path instead."
        )
    }
}
