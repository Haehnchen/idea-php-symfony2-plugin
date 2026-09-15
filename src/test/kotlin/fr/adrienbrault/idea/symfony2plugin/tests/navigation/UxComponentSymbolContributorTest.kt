package fr.adrienbrault.idea.symfony2plugin.tests.navigation

import com.intellij.navigation.NavigationItem
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.indexing.FindSymbolParameters
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase
import fr.adrienbrault.idea.symfony2plugin.ux.UxComponentSymbolContributor

/**
 * @see fr.adrienbrault.idea.symfony2plugin.ux.UxComponentSymbolContributor
 */
class UxComponentSymbolContributorTest : SymfonyLightCodeInsightFixtureTestCase() {
    fun testThatComponentNamesAreProvidedForSearch() {
        myFixture.addFileToProject(
            "src/Twig/Components/Alert.php",
            "<?php\n" +
                "namespace App\\Twig\\Components;\n" +
                "\n" +
                "use Symfony\\UX\\TwigComponent\\Attribute\\AsTwigComponent;\n" +
                "\n" +
                "#[AsTwigComponent]\n" +
                "class Alert {}\n",
        )

        val contributor = UxComponentSymbolContributor()

        val names = mutableListOf<String>()
        contributor.processNames(names::add, GlobalSearchScope.allScope(project), null)

        assertContainsElements(names, "Alert")
    }

    fun testThatComponentNavigationIsProvidedForSearch() {
        myFixture.addFileToProject(
            "src/Twig/Components/Alert.php",
            "<?php\n" +
                "namespace App\\Twig\\Components;\n" +
                "\n" +
                "use Symfony\\UX\\TwigComponent\\Attribute\\AsTwigComponent;\n" +
                "\n" +
                "#[AsTwigComponent]\n" +
                "class Alert {}\n",
        )
        myFixture.addFileToProject("templates/components/Alert.html.twig", "<div></div>")

        val contributor = UxComponentSymbolContributor()

        val items = mutableListOf<NavigationItem>()
        contributor.processElementsWithName("Alert", items::add, FindSymbolParameters.simple(project, false))

        assertTrue(items.any { it.name == "Alert" && it.presentation!!.locationString == "TwigComponent (Alert)" })

        val unknownItems = mutableListOf<NavigationItem>()
        contributor.processElementsWithName("Unknown", unknownItems::add, FindSymbolParameters.simple(project, false))
        assertEmpty(unknownItems)
    }
}
