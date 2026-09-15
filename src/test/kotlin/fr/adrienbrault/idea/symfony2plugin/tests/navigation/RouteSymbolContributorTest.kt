package fr.adrienbrault.idea.symfony2plugin.tests.navigation

import com.intellij.navigation.NavigationItem
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.indexing.FindSymbolParameters
import fr.adrienbrault.idea.symfony2plugin.navigation.RouteSymbolContributor
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase

/**
 * @see RouteSymbolContributor
 */
class RouteSymbolContributorTest : SymfonyLightCodeInsightFixtureTestCase() {
    override fun setUp() {
        super.setUp()
        myFixture.copyFileToProject("routing/fixtures/RouteHelper.php", "src/Controller/RouteHelper.php")
        myFixture.copyFileToProject("routing/fixtures/RouteHelper.services.yml", "config/services.yml")
    }

    override fun getTestDataPath(): String {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests"
    }

    fun testThatRouteNamesAndNavigationAreProvidedForSearch() {
        val contributor = RouteSymbolContributor()
        val names = mutableListOf<String>()
        contributor.processNames(names::add, GlobalSearchScope.allScope(project), null)

        assertContainsElements(names, "my_car_foo_stuff")

        val items = mutableListOf<NavigationItem>()
        contributor.processElementsWithName(
            "my_car_foo_stuff",
            items::add,
            FindSymbolParameters.simple(project, false),
        )
        assertTrue(items.any { it.name == "my_car_foo_stuff" })
    }
}
