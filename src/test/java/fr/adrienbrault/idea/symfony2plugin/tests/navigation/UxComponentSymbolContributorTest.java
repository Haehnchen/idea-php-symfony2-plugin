package fr.adrienbrault.idea.symfony2plugin.tests.navigation;

import com.intellij.navigation.NavigationItem;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.indexing.FindSymbolParameters;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import fr.adrienbrault.idea.symfony2plugin.ux.UxComponentSymbolContributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @see fr.adrienbrault.idea.symfony2plugin.ux.UxComponentSymbolContributor
 */
public class UxComponentSymbolContributorTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void testThatComponentNamesAreProvidedForSearch() {
        myFixture.addFileToProject("src/Twig/Components/Alert.php", "<?php\n" +
            "namespace App\\Twig\\Components;\n" +
            "\n" +
            "use Symfony\\UX\\TwigComponent\\Attribute\\AsTwigComponent;\n" +
            "\n" +
            "#[AsTwigComponent]\n" +
            "class Alert {}\n"
        );

        UxComponentSymbolContributor contributor = new UxComponentSymbolContributor();

        List<String> names = new ArrayList<>();
        contributor.processNames(names::add, GlobalSearchScope.allScope(getProject()), null);

        assertContainsElements(names, "Alert");
    }

    public void testThatComponentNavigationIsProvidedForSearch() {
        myFixture.addFileToProject("src/Twig/Components/Alert.php", "<?php\n" +
            "namespace App\\Twig\\Components;\n" +
            "\n" +
            "use Symfony\\UX\\TwigComponent\\Attribute\\AsTwigComponent;\n" +
            "\n" +
            "#[AsTwigComponent]\n" +
            "class Alert {}\n"
        );
        myFixture.addFileToProject("templates/components/Alert.html.twig", "<div></div>");

        UxComponentSymbolContributor contributor = new UxComponentSymbolContributor();

        List<NavigationItem> items = new ArrayList<>();
        contributor.processElementsWithName("Alert", items::add, FindSymbolParameters.simple(getProject(), false));

        assertTrue(items.stream().anyMatch(item -> "Alert".equals(item.getName()) && "TwigComponent (Alert)".equals(Objects.requireNonNull(item.getPresentation()).getLocationString())));

        List<NavigationItem> unknownItems = new ArrayList<>();
        contributor.processElementsWithName("Unknown", unknownItems::add, FindSymbolParameters.simple(getProject(), false));
        assertEmpty(unknownItems);
    }
}
