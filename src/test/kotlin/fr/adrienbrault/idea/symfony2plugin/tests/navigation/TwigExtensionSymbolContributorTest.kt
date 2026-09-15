package fr.adrienbrault.idea.symfony2plugin.tests.navigation

import com.intellij.navigation.NavigationItem
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.indexing.FindSymbolParameters
import fr.adrienbrault.idea.symfony2plugin.navigation.TwigExtensionSymbolContributor
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase

/**
 * @see TwigExtensionSymbolContributor
 */
class TwigExtensionSymbolContributorTest : SymfonyLightCodeInsightFixtureTestCase() {
    override fun setUp() {
        super.setUp()
        myFixture.copyFileToProject("templating/util/fixtures/twig_extensions.php", "src/Twig/Extensions.php")
    }

    override fun getTestDataPath(): String {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests"
    }

    fun testThatExtensionNamesAndNavigationAreProvidedForSearch() {
        val contributor = TwigExtensionSymbolContributor()
        val names = mutableListOf<String>()
        contributor.processNames(names::add, GlobalSearchScope.allScope(project), null)

        assertContainsElements(names, "trans", "max")

        val items = mutableListOf<NavigationItem>()
        contributor.processElementsWithName("trans", items::add, FindSymbolParameters.simple(project, false))
        assertTrue(items.any { it.name == "trans" })
    }
}
