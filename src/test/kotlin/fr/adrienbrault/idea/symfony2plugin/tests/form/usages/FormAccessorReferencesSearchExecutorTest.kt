package fr.adrienbrault.idea.symfony2plugin.tests.form.usages

import com.intellij.find.findUsages.PsiElement2UsageTargetAdapter
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.LocalSearchScope
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.php.lang.psi.elements.PhpClass
import fr.adrienbrault.idea.symfony2plugin.form.usages.FormFieldUsageTypeProvider

class FormAccessorReferencesSearchExecutorTest : FormFieldUsageTestCase() {
    fun testPrivatePropertyUsesPublicGetterAndSetter() {
        val data = dataClass("private string \$title; public function getTitle(): string {} public function setTitle(string \$value): void {}")
        form()

        for (name in listOf("getTitle", "setTitle")) {
            val method = data.findMethodByName(name)!!
            val reference = usages(method).single()

            assertEquals(method, reference.resolve())
            assertEquals("title", reference.rangeInElement.substring(reference.element.text))
            assertEquals("Symfony form field", FormFieldUsageTypeProvider().getUsageType(reference.element, arrayOf(PsiElement2UsageTargetAdapter(method, true))).toString())
        }

        assertEmpty(usages(data.findFieldByName("title", false)!!))
    }

    fun testInheritedAccessorsExcludeOverridesAndUnrelatedClasses() {
        val base = dataClass("private \$title; public function getTitle() {} public function setTitle(\$value) {}", "BaseData")
        childClass("ProfileData", "BaseData")
        childClass("DetailData", "ProfileData")
        val overridden = childClass("OverrideData", "BaseData", "public function getTitle() {} public function setTitle(\$value) {}")
        dataClass("public function getTitle() {} public function setTitle(\$value) {}", "UnrelatedData")

        form()
        form(name = "DetailType", data = "DetailData")
        form(name = "OverrideType", data = "OverrideData")
        form(name = "UnrelatedType", data = "UnrelatedData")

        for (name in listOf("getTitle", "setTitle")) {
            assertEquals(setOf("ProfileType.php", "DetailType.php"), usages(base.findMethodByName(name)!!).map { it.element.containingFile.name }.toSet())
            assertEquals(listOf("OverrideType.php"), usages(overridden.findMethodByName(name)!!).map { it.element.containingFile.name })
        }
    }

    fun testGetterOverridePreservesInheritedSetterUsage() {
        val base = dataClass("public function getTitle() {} public function setTitle(\$value) {}", "BaseData")
        val child = childClass("ProfileData", "BaseData", "public function getTitle() {}")
        form()

        assertEmpty(usages(base.findMethodByName("getTitle")!!))
        assertEquals(1, usages(base.findMethodByName("setTitle")!!).size)
        assertEquals(1, usages(child.findMethodByName("getTitle")!!).size)
    }

    fun testInheritedPublicProperty() {
        val base = dataClass("public \$title;", "BaseData")
        childClass("ProfileData", "BaseData")
        form()

        assertEquals(1, usages(base.findFieldByName("title", false)!!).size)
    }

    fun testGetterPrecedenceAndBooleanAccessors() {
        val data = dataClass("public function getTitle() {} public function isTitle() {} public function hasTitle() {} public function isActive(): bool {} public function hasItems(): bool {}")
        form("\$builder->add('title'); \$builder->add('active'); \$builder->add('items');")

        for (name in listOf("getTitle", "isActive", "hasItems")) {
            assertEquals(1, usages(data.findMethodByName(name)!!).size)
        }

        assertEmpty(usages(data.findMethodByName("isTitle")!!))
        assertEmpty(usages(data.findMethodByName("hasTitle")!!))
    }

    fun testUnsupportedMethodsAndInvalidSignatures() {
        val data = dataClass("""
            private function getTitle() {}
            protected function setTitle(${ '$' }value) {}
            public static function getShared() {}
            public static function setShared(${ '$' }value) {}
            public function getRequired(${ '$' }value) {}
            public function getEmpty(): void {}
            public function getNever(): never {}
            public function setMissing() {}
            public function setExtra(${ '$' }a, ${ '$' }b) {}
            public function processTitle() {}
        """.trimIndent())
        form("\$builder->add('title'); \$builder->add('shared'); \$builder->add('required'); \$builder->add('empty'); \$builder->add('never'); \$builder->add('missing'); \$builder->add('extra');")

        data.methods.forEach { assertEmpty(it.name, usages(it)) }
    }

    fun testOptionalParametersAndCamelCaseNormalization() {
        val data = dataClass("public function getDisplayTitle(\$fallback = null) {} public function setDisplayTitle(\$value, \$options = []) {}")
        form("\$builder->add('displayTitle'); \$builder->create('display_title');")

        for (name in listOf("getDisplayTitle", "setDisplayTitle")) {
            assertEquals(setOf("displayTitle", "display_title"), usages(data.findMethodByName(name)!!).map { it.element.contents }.toSet())
        }
    }

    fun testMappingAndScopeForInheritedAccessors() {
        val base = dataClass("public function getTitle() {} public function setTitle(\$value) {}", "BaseData")
        childClass("ProfileData", "BaseData")
        val first = form("\$builder->add('title'); \$builder->add('title', null, ['mapped' => false]); \$builder->add('title', null, ['property_path' => 'other']); \$builder->get('child')->add('title');")
        form(name = "SecondType")

        for (name in listOf("getTitle", "setTitle")) {
            val target = base.findMethodByName(name)!!
            val reference = usages(target, GlobalSearchScope.fileScope(first)).single()

            assertEquals(1, usages(target, LocalSearchScope(reference.element)).size)
            assertEquals(2, usages(target).size)
        }
    }

    private fun childClass(name: String, parent: String, members: String = ""): PhpClass {
        val file = myFixture.addFileToProject("$name.php", "<?php namespace App; class $name extends $parent { $members }")

        return PsiTreeUtil.findChildOfType(file, PhpClass::class.java)!!
    }
}
