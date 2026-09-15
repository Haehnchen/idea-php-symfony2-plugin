package fr.adrienbrault.idea.symfony2plugin.tests.navigation

import com.intellij.navigation.NavigationItem
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.indexing.FindSymbolParameters
import fr.adrienbrault.idea.symfony2plugin.navigation.ServiceSymbolContributor
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase

/**
 * @see ServiceSymbolContributor
 */
class ServiceSymbolContributorTest : SymfonyLightCodeInsightFixtureTestCase() {
    override fun setUp() {
        super.setUp()
        myFixture.copyFileToProject("stubs/fixtures/services.yml", "config/services.yml")
    }

    override fun getTestDataPath(): String {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests"
    }

    fun testThatServiceNamesAndNavigationAreProvidedForSearch() {
        val contributor = ServiceSymbolContributor()
        val names = mutableListOf<String>()
        contributor.processNames(names::add, GlobalSearchScope.allScope(project), null)

        assertContainsElements(names, "foo.yml_id")

        val items = mutableListOf<NavigationItem>()
        contributor.processElementsWithName(
            "foo.yml_id",
            items::add,
            FindSymbolParameters.simple(project, false),
        )
        assertTrue(items.any { it.name == "foo.yml_id" })
    }
}
