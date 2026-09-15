package fr.adrienbrault.idea.symfony2plugin.tests.navigation

import com.intellij.navigation.NavigationItem
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.indexing.FindSymbolParameters
import fr.adrienbrault.idea.symfony2plugin.navigation.TwigMacroSymbolContributor
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase

/**
 * @see TwigMacroSymbolContributor
 */
class TwigMacroSymbolContributorTest : SymfonyLightCodeInsightFixtureTestCase() {
    fun testThatMacroNamesAndNavigationAreProvidedForSearch() {
        myFixture.addFileToProject(
            "templates/macros.html.twig",
            "{% macro render_item(item) %}{{ item }}{% endmacro %}",
        )

        val contributor = TwigMacroSymbolContributor()
        val names = mutableListOf<String>()
        contributor.processNames(names::add, GlobalSearchScope.allScope(project), null)

        assertContainsElements(names, "render_item")

        val items = mutableListOf<NavigationItem>()
        contributor.processElementsWithName(
            "render_item",
            items::add,
            FindSymbolParameters.simple(project, false),
        )
        assertTrue(items.any { it.name == "render_item" })
    }
}
