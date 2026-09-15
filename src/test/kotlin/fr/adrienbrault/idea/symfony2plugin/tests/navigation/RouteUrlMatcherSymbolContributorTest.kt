package fr.adrienbrault.idea.symfony2plugin.tests.navigation

import com.intellij.navigation.NavigationItem
import com.intellij.util.indexing.FindSymbolParameters
import fr.adrienbrault.idea.symfony2plugin.navigation.RouteUrlMatcherSymbolContributor
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase

/**
 * @see RouteUrlMatcherSymbolContributor
 */
class RouteUrlMatcherSymbolContributorTest : SymfonyLightCodeInsightFixtureTestCase() {
    override fun setUp() {
        super.setUp()
        myFixture.copyFileToProject("routing/fixtures/RouteHelper.php", "src/Controller/RouteHelper.php")
        myFixture.copyFileToProject("routing/fixtures/RouteHelper.services.yml", "config/services.yml")
    }

    override fun getTestDataPath(): String {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests"
    }

    fun testThatMatchingUrlNamesAndNavigationAreProvidedForSearch() {
        val contributor = RouteUrlMatcherSymbolContributor()
        val parameters = FindSymbolParameters.wrap("/edit/12", project, false)
        val names = mutableListOf<String>()
        contributor.processNames(names::add, parameters)

        assertContainsElements(names, "/edit/12")

        val items = mutableListOf<NavigationItem>()
        contributor.processElementsWithName("/edit/12", items::add, parameters)
        assertFalse(items.isEmpty())
    }
}
