package fr.adrienbrault.idea.symfony2plugin.tests.navigation

import com.intellij.navigation.NavigationItem
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.indexing.FindSymbolParameters
import fr.adrienbrault.idea.symfony2plugin.doctrine.EntitySymbolContributor
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase

/**
 * @see EntitySymbolContributor
 */
class EntitySymbolContributorTest : SymfonyLightCodeInsightFixtureTestCase() {
    override fun setUp() {
        super.setUp()
        myFixture.copyFileToProject("doctrine/metadata/util/fixtures/attribute_entity.php", "src/Entity/AttributeUser.php")
    }

    override fun getTestDataPath(): String {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests"
    }

    fun testThatEntityNamesAndNavigationAreProvidedForSearch() {
        val contributor = EntitySymbolContributor()
        val names = mutableListOf<String>()
        contributor.processNames(names::add, GlobalSearchScope.allScope(project), null)

        assertContainsElements(names, "AttributeUser")

        val items = mutableListOf<NavigationItem>()
        contributor.processElementsWithName(
            "AttributeUser",
            items::add,
            FindSymbolParameters.simple(project, false),
        )
        assertTrue(items.any { it.name == "AttributeUser" })
    }
}
