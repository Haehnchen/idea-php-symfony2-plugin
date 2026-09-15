package fr.adrienbrault.idea.symfony2plugin.tests.navigation

import com.intellij.navigation.NavigationItem
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.indexing.FindSymbolParameters
import fr.adrienbrault.idea.symfony2plugin.navigation.SymfonyCommandSymbolContributor
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase

/**
 * @see SymfonyCommandSymbolContributor
 */
class SymfonyCommandSymbolContributorTest : SymfonyLightCodeInsightFixtureTestCase() {
    override fun setUp() {
        super.setUp()
        myFixture.copyFileToProject("util/fixtures/SymfonyCommandUtilTest.php", "src/Command/Commands.php")
    }

    override fun getTestDataPath(): String {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests"
    }

    fun testThatCommandNamesAndNavigationAreProvidedForSearch() {
        val contributor = SymfonyCommandSymbolContributor()
        val names = mutableListOf<String>()
        contributor.processNames(names::add, GlobalSearchScope.allScope(project), null)

        assertContainsElements(names, "app:create-user-1")

        val items = mutableListOf<NavigationItem>()
        contributor.processElementsWithName(
            "app:create-user-1",
            items::add,
            FindSymbolParameters.simple(project, false),
        )
        assertTrue(items.any { it.name == "app:create-user-1" })
    }
}
