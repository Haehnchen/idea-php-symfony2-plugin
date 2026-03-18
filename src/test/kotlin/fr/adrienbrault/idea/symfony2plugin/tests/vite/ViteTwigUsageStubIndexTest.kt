package fr.adrienbrault.idea.symfony2plugin.tests.vite

import com.jetbrains.twig.TwigFileType
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase
import fr.adrienbrault.idea.symfony2plugin.vite.VITE_TWIG_USAGE_STUB_INDEX_KEY
import fr.adrienbrault.idea.symfony2plugin.vite.ViteTwigUsageStubIndex

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see ViteTwigUsageStubIndex
 */
class ViteTwigUsageStubIndexTest : SymfonyLightCodeInsightFixtureTestCase() {

    fun testViteEntryScriptTagsIsIndexed() {
        myFixture.configureByText(
            TwigFileType.INSTANCE,
            "{{ vite_entry_script_tags('app') }}"
        )

        assertIndexContains(VITE_TWIG_USAGE_STUB_INDEX_KEY, "app")
    }

    fun testViteEntryLinkTagsIsIndexed() {
        myFixture.configureByText(
            TwigFileType.INSTANCE,
            "{{ vite_entry_link_tags('styles') }}"
        )

        assertIndexContains(VITE_TWIG_USAGE_STUB_INDEX_KEY, "styles")
    }

    fun testMultipleEntriesInSameFileAreIndexed() {
        myFixture.configureByText(
            TwigFileType.INSTANCE,
            """
            {{ vite_entry_script_tags('app') }}
            {{ vite_entry_link_tags('admin') }}
            """.trimIndent()
        )

        assertIndexContains(VITE_TWIG_USAGE_STUB_INDEX_KEY, "app")
        assertIndexContains(VITE_TWIG_USAGE_STUB_INDEX_KEY, "admin")
    }

    fun testSlashEntryNameIsIndexed() {
        myFixture.configureByText(
            TwigFileType.INSTANCE,
            "{{ vite_entry_script_tags('grundproben/main') }}"
        )

        assertIndexContains(VITE_TWIG_USAGE_STUB_INDEX_KEY, "grundproben/main")
    }

    fun testNonViteFunctionIsNotIndexed() {
        myFixture.configureByText(
            TwigFileType.INSTANCE,
            "{{ asset('app.js') }}"
        )

        assertIndexNotContains(VITE_TWIG_USAGE_STUB_INDEX_KEY, "app.js")
    }

    fun testBlockTagSyntaxIsIndexed() {
        myFixture.configureByText(
            TwigFileType.INSTANCE,
            "{% set x = vite_entry_script_tags('app') %}"
        )

        assertIndexContains(VITE_TWIG_USAGE_STUB_INDEX_KEY, "app")
    }
}
