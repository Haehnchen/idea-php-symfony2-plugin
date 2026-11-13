package fr.adrienbrault.idea.symfony2plugin.tests.translation.inspection

import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase
import fr.adrienbrault.idea.symfony2plugin.translation.inspection.TwigTranslationParameterInspection

class TwigTranslationParameterInspectionTest : SymfonyLightCodeInsightFixtureTestCase() {
    override fun setUp() {
        super.setUp()
        myFixture.addFileToProject(
            "translations/home+intl-icu.en.yaml",
            """
            project_aria_label: '{sum, number} contributions on {label}: {reviews, number} reviews, {prs, number} pull requests'
            invitation: '{gender, select, female {She} male {He} other {They}} invited {guest}'
            broken: 'Hello {name'
            """.trimIndent()
        )
        myFixture.addFileToProject("translations/home+intl-icu.fr.yaml", "invitation: '{guest} est invité chez {host}'")
        myFixture.addFileToProject("translations/legacy.en.yaml", "greeting: 'Hello %name%'")
        myFixture.addFileToProject(
            "translations/pages+intl-icu.en.xlf",
            """
            <?xml version="1.0"?>
            <xliff version="1.2"><file source-language="en" datatype="plaintext" original="file.ext"><body>
            <trans-unit id="t" resname="home.title"><source>Welcome {name}</source><target>Welcome {name}</target></trans-unit>
            </body></file></xliff>
            """.trimIndent()
        )
    }

    fun testUnknownParameterIsReported() {
        assertUnknown("{{ 'project_aria_label'|trans({su<caret>mm: 1}, 'home') }}")
        assertUnknown("{{ 'project_aria_label'|trans({'su<caret>mm': 1}, 'home') }}")
        assertUnknown("{{ 'project_aria_label'|trans(domain: 'home', arguments: {fo<caret>o: 1}) }}")
        assertUnknown("{{ 'project_aria_label'|transchoice(1, {fo<caret>o: 1}, 'home') }}")
        assertUnknown("{% trans_default_domain 'home' %}{{ 'project_aria_label'|trans({fo<caret>o: 1}) }}")
        assertUnknown("{{ 'invitation'|trans_html({}, {fo<caret>o: 1}, 'home') }}")
        assertUnknown("{{ 'home.title'|trans({fo<caret>o: 1}, 'pages') }}")
    }

    fun testKnownParametersAreNotReported() {
        assertKnown("{{ 'project_aria_label'|trans({su<caret>m: 1, label: 'x'}, 'home') }}")
        assertKnown("{{ 'project_aria_label'|trans({'%su<caret>m%': 1}, 'home') }}")
        assertKnown("{{ 'invitation'|trans({ho<caret>st: 1}, 'home') }}")
        assertKnown("{{ 'invitation'|trans_html({fo<caret>o: {}}, {}, 'home') }}")
        assertKnown("{{ 'home.title'|trans({na<caret>me: 1}, 'pages') }}")
    }

    fun testParametersAreNotReportedWhenTheMessageCannotBeChecked() {
        assertKnown("{{ 'greeting'|trans({fo<caret>o: 1}, 'legacy') }}")
        assertKnown("{{ 'broken'|trans({fo<caret>o: 1}, 'home') }}")
        assertKnown("{{ 'missing'|trans({fo<caret>o: 1}, 'home') }}")
        assertKnown("{{ 'project_aria_label'|trans({fo<caret>o: 1}, domain) }}")
    }

    private fun assertUnknown(template: String) {
        assertLocalInspectionContains(TwigTranslationParameterInspection::class.java, "test.html.twig", template, TwigTranslationParameterInspection.MESSAGE)
    }

    private fun assertKnown(template: String) {
        assertLocalInspectionNotContains(TwigTranslationParameterInspection::class.java, "test.html.twig", template, TwigTranslationParameterInspection.MESSAGE)
    }
}
