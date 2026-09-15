package fr.adrienbrault.idea.symfony2plugin.tests.navigation.controller

import com.intellij.navigation.GotoRelatedItem
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.php.lang.PhpFileType
import com.jetbrains.php.lang.psi.elements.Method
import fr.adrienbrault.idea.symfony2plugin.extension.ControllerActionGotoRelatedCollectorParameter
import fr.adrienbrault.idea.symfony2plugin.navigation.controller.TemplatesControllerRelatedGotoCollector
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase

/**
 * @see fr.adrienbrault.idea.symfony2plugin.navigation.controller.TemplatesControllerRelatedGotoCollector
 */
class TemplatesControllerRelatedGotoCollectorTest : SymfonyLightCodeInsightFixtureTestCase() {
    fun testRenderProvidesTemplateTarget() {
        myFixture.addFileToProject(
            "ide-twig.json",
            """
            {
              "namespaces": [
                { "namespace": "", "path": "templates" }
              ]
            }
            """.trimIndent(),
        )
        val template = myFixture.addFileToProject(
            "templates/product/index.html.twig",
            "{{ product.name }}",
        )
        val method = configureController(
            "<?php namespace App\\Controller; class ProductController {" +
                " public function in<caret>dex() {" +
                " return \$this->render('product/index.html.twig');" +
                " }" +
                "}",
        )
        val relatedItems = ArrayList<GotoRelatedItem>()

        TemplatesControllerRelatedGotoCollector().collectGotoRelatedItems(
            ControllerActionGotoRelatedCollectorParameter(method, relatedItems),
        )

        assertEquals(1, relatedItems.size)
        assertSame(template, relatedItems.single().element)
        assertEquals("product/index.html.twig", relatedItems.single().customName)
    }

    private fun configureController(contents: String): Method {
        val file = myFixture.configureByText(PhpFileType.INSTANCE, contents)
        return PsiTreeUtil.getParentOfType(file.findElementAt(myFixture.caretOffset), Method::class.java)!!
    }
}
