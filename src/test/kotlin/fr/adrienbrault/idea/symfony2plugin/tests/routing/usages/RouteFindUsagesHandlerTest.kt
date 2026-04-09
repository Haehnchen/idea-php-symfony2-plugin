package fr.adrienbrault.idea.symfony2plugin.tests.routing.usages

import com.intellij.find.findUsages.FindUsagesHandler
import com.intellij.navigation.NavigationItem
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons
import fr.adrienbrault.idea.symfony2plugin.routing.usages.RouteFindUsagesHandlerFactory
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase

class RouteFindUsagesHandlerTest : SymfonyLightCodeInsightFixtureTestCase() {
    fun testFindUsagesHandlerPrimaryElementUsesRoutePresentationForPhpAttribute() {
        val element = myFixture.configureByText(
            "controller.php",
            """
            <?php
            namespace App\Controller;
            use Symfony\Component\Routing\Attribute\Route;
            class MyController {
                #[Route('/foo', name: 'app_report_submitter<caret>')]
                public function __invoke() {}
            }
            """.trimIndent()
        ).findElementAt(myFixture.caretOffset)

        assertNotNull(element)

        val handler: FindUsagesHandler? = RouteFindUsagesHandlerFactory().createFindUsagesHandler(element!!, false)
        assertNotNull(handler)

        val primaryElement = handler!!.primaryElements[0] as NavigationItem
        assertEquals("app_report_submitter", primaryElement.presentation!!.presentableText)
        assertEquals("/foo, controller.php", primaryElement.presentation!!.locationString)
        assertEquals(Symfony2Icons.ROUTE, primaryElement.presentation!!.getIcon(false))
    }
}
