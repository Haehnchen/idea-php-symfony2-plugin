package fr.adrienbrault.idea.symfony2plugin.tests.navigation.controller

import com.intellij.navigation.GotoRelatedItem
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.php.lang.PhpFileType
import com.jetbrains.php.lang.psi.elements.Method
import com.jetbrains.php.lang.psi.elements.PhpClass
import fr.adrienbrault.idea.symfony2plugin.extension.ControllerActionGotoRelatedCollectorParameter
import fr.adrienbrault.idea.symfony2plugin.navigation.controller.ModelsControllerRelatedGotoCollector
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase

/**
 * @see fr.adrienbrault.idea.symfony2plugin.navigation.controller.ModelsControllerRelatedGotoCollector
 */
class ModelsControllerRelatedGotoCollectorTest : SymfonyLightCodeInsightFixtureTestCase() {
    fun testRepositoryParameterProvidesEntityTarget() {
        myFixture.addFileToProject(
            "Doctrine.php",
            "<?php namespace Doctrine\\Persistence; interface ObjectManager { public function getRepository(\$class); }",
        )
        myFixture.addFileToProject(
            "src/Entity/Product.php",
            "<?php namespace App\\Entity; class Product {}",
        )

        val method = configureController(
            "<?php namespace App\\Controller; class ProductController {" +
                " public function in<caret>dex(\\Doctrine\\Persistence\\ObjectManager \$manager) {" +
                " \$manager->getRepository(\\App\\Entity\\Product::class);" +
                " }" +
                "}",
        )
        val relatedItems = ArrayList<GotoRelatedItem>()

        ModelsControllerRelatedGotoCollector().collectGotoRelatedItems(
            ControllerActionGotoRelatedCollectorParameter(method, relatedItems),
        )

        assertEquals(1, relatedItems.size)
        assertEquals("\\App\\Entity\\Product", (relatedItems.single().element as PhpClass).fqn)
    }

    private fun configureController(contents: String): Method {
        val file = myFixture.configureByText(PhpFileType.INSTANCE, contents)
        return PsiTreeUtil.getParentOfType(file.findElementAt(myFixture.caretOffset), Method::class.java)!!
    }
}
