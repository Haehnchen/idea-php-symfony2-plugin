package fr.adrienbrault.idea.symfony2plugin.tests.navigation

import com.intellij.navigation.NavigationItem
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.indexing.FindSymbolParameters
import fr.adrienbrault.idea.symfony2plugin.doctrine.EntityTableSymbolContributor
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase

/**
 * @see EntityTableSymbolContributor
 */
class EntityTableSymbolContributorTest : SymfonyLightCodeInsightFixtureTestCase() {
    override fun setUp() {
        super.setUp()
        myFixture.copyFileToProject("doctrine/metadata/util/fixtures/attribute_entity.php", "src/Entity/AttributeUser.php")
    }

    override fun getTestDataPath(): String {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests"
    }

    fun testThatEntityTableNamesAndNavigationAreProvidedForSearch() {
        val contributor = EntityTableSymbolContributor()
        val names = mutableListOf<String>()
        contributor.processNames(names::add, GlobalSearchScope.allScope(project), null)

        assertContainsElements(names, "php_attribute_table")

        val items = mutableListOf<NavigationItem>()
        contributor.processElementsWithName(
            "php_attribute_table",
            items::add,
            FindSymbolParameters.simple(project, false),
        )
        assertTrue(items.any { it.name == "php_attribute_table" })
    }
}
