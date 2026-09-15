package fr.adrienbrault.idea.symfony2plugin.tests.navigation

import com.intellij.navigation.NavigationItem
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.indexing.FindSymbolParameters
import fr.adrienbrault.idea.symfony2plugin.navigation.TwigBlockSymbolContributor
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 *
 * @see fr.adrienbrault.idea.symfony2plugin.navigation.TwigBlockSymbolContributor
 */
class TwigBlockSymbolContributorTest : SymfonyLightCodeInsightFixtureTestCase() {
    override fun setUp() {
        super.setUp()
        myFixture.copyFileToProject("test.html.twig")
        myFixture.copyFileToProject("test2.html.twig")
    }

    override fun getTestDataPath(): String {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/navigation/fixtures"
    }

    fun testThatBlockNamesAreProvidedForSearch() {
        val contributor = TwigBlockSymbolContributor()

        val names = mutableListOf<String>()
        contributor.processNames(names::add, GlobalSearchScope.allScope(project), null)

        assertContainsElements(
            names,
            "my_support_block_name",
            "my_support_block_name_2",
        )
    }

    fun testThatBlockNavigationIsProvidedForSearch() {
        val contributor = TwigBlockSymbolContributor()

        // Test for "my_support_block_name"
        val items1 = mutableListOf<NavigationItem>()
        val params1 = FindSymbolParameters.simple(project, false)
        contributor.processElementsWithName("my_support_block_name", items1::add, params1)
        assertTrue(items1.any { it.name == "my_support_block_name" })

        // Test for "my_support_block_name_2"
        val items2 = mutableListOf<NavigationItem>()
        val params2 = FindSymbolParameters.simple(project, false)
        contributor.processElementsWithName("my_support_block_name_2", items2::add, params2)
        assertTrue(items2.any { it.name == "my_support_block_name_2" })

        // Test for unknown block
        val items3 = mutableListOf<NavigationItem>()
        val params3 = FindSymbolParameters.simple(project, false)
        contributor.processElementsWithName("UNKNOWN_BLOCK", items3::add, params3)
        assertSize(0, items3)
    }
}
