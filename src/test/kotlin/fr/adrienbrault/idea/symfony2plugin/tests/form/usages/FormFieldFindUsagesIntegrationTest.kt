package fr.adrienbrault.idea.symfony2plugin.tests.form.usages

import com.intellij.find.FindManager
import com.intellij.find.impl.FindManagerImpl
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.refactoring.rename.RenameProcessor
import com.intellij.usageView.UsageInfo
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression

class FormFieldFindUsagesIntegrationTest : FormFieldUsageTestCase() {
    fun testNativeHandlerIncludesPhpAndFormUsages() {
        val target = dataClass().findFieldByName("title", false)!!
        val form = form()
        val php = myFixture.addFileToProject("native.php", "<?php \$dto = new \\App\\ProfileData(); echo \$dto->title;")

        val manager = FindManager.getInstance(project) as FindManagerImpl
        val handler = manager.findUsagesManager.getFindUsagesHandler(target, false)!!
        val options = handler.findUsagesOptions
        options.searchScope = GlobalSearchScope.projectScope(project)
        options.isSearchForTextOccurrences = false
        options.isUsages = true

        val usages = mutableListOf<UsageInfo>()

        assertTrue(handler.processElementUsages(target, { usages.add(it) }, options))
        assertTrue(usages.toString(), usages.any { it.element is StringLiteralExpression && it.virtualFile == form.virtualFile })
        assertTrue(usages.toString(), usages.any { it.virtualFile == php.virtualFile })
    }

    fun testNativeHandlerIncludesInheritedGetterAndSetterFormUsages() {
        val base = dataClass("private \$title; public function getTitle() {} public function setTitle(\$value) {}", "BaseData")
        myFixture.addFileToProject("ProfileData.php", "<?php namespace App; class ProfileData extends BaseData {}")
        val form = form()
        val php = myFixture.addFileToProject("native.php", "<?php \$dto = new \\App\\ProfileData(); \$dto->getTitle(); \$dto->setTitle('value');")

        for (name in listOf("getTitle", "setTitle")) {
            val target = base.findMethodByName(name)!!

            val manager = FindManager.getInstance(project) as FindManagerImpl
            val handler = manager.findUsagesManager.getFindUsagesHandler(target, false)!!
            val options = handler.findUsagesOptions
            options.searchScope = GlobalSearchScope.projectScope(project)
            options.isSearchForTextOccurrences = false
            options.isUsages = true

            val found = mutableListOf<UsageInfo>()

            assertTrue(handler.processElementUsages(target, { found.add(it) }, options))
            assertTrue(name, found.any { it.element is StringLiteralExpression && it.virtualFile == form.virtualFile })
            assertTrue(name, found.any { it.virtualFile == php.virtualFile })
        }
    }

    fun testAccessorRenamePreservesFormField() {
        val data = dataClass("private \$title; public function getTitle() {} public function setTitle(\$value) {}")
        val form = form()
        val before = form.text
        val php = myFixture.addFileToProject("native.php", "<?php \$dto = new \\App\\ProfileData(); \$dto->getTitle(); \$dto->setTitle('value');")

        for (name in listOf("getTitle", "setTitle")) {
            val target = data.findMethodByName(name)!!

            assertEquals(1, usages(target).size)

            RenameProcessor(project, target, name + "Updated", false, false).run()

            assertEquals(before, form.text)
            assertTrue(php.text, php.text.contains("->" + name + "Updated"))
        }
    }

    fun testPropertyRenamePreservesFormFieldWithGetterAndSetter() {
        val target = dataClass("public \$title; public function getTitle() {} public function setTitle(\$value) {}").findFieldByName("title", false)!!
        val form = form()
        val before = form.text
        val php = myFixture.addFileToProject("native.php", "<?php \$dto = new \\App\\ProfileData(); echo \$dto->title;")

        assertEquals(1, usages(target).size)

        RenameProcessor(project, target, "updatedTitle", false, false).run()

        assertEquals(before, form.text)
        assertTrue(php.text, php.text.contains("->updatedTitle"))
    }
}
