package fr.adrienbrault.idea.symfony2plugin.tests.navigation.controller

import com.intellij.navigation.GotoRelatedItem
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.php.lang.PhpFileType
import com.jetbrains.php.lang.psi.elements.Method
import fr.adrienbrault.idea.symfony2plugin.extension.ControllerActionGotoRelatedCollectorParameter
import fr.adrienbrault.idea.symfony2plugin.navigation.controller.TwigControllerUsageControllerRelatedGotoCollector
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase

/**
 * @see fr.adrienbrault.idea.symfony2plugin.navigation.controller.TwigControllerUsageControllerRelatedGotoCollector
 */
class TwigControllerUsageControllerRelatedGotoCollectorTest : SymfonyLightCodeInsightFixtureTestCase() {
    fun testControllerFunctionProvidesTwigTarget() {
        val template = myFixture.addFileToProject(
            "templates/controller.html.twig",
            "{{ controller('App\\\\Controller\\\\HomeController::menu') }}",
        )
        val method = configureController(
            "<?php namespace App\\Controller; class HomeController {" +
                " public function me<caret>nu() {}" +
                "}",
        )
        val relatedItems = ArrayList<GotoRelatedItem>()

        TwigControllerUsageControllerRelatedGotoCollector().collectGotoRelatedItems(
            ControllerActionGotoRelatedCollectorParameter(method, relatedItems),
        )

        assertEquals(1, relatedItems.size)
        assertSame(template, relatedItems.single().element!!.containingFile)
    }

    private fun configureController(contents: String): Method {
        val file = myFixture.configureByText(PhpFileType.INSTANCE, contents)
        return PsiTreeUtil.getParentOfType(file.findElementAt(myFixture.caretOffset), Method::class.java)!!
    }
}
