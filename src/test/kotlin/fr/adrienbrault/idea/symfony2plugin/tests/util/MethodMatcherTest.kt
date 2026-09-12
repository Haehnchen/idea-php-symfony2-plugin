package fr.adrienbrault.idea.symfony2plugin.tests.util

import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.php.lang.PhpFileType
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase
import fr.adrienbrault.idea.symfony2plugin.util.MethodMatcher

class MethodMatcherTest : SymfonyLightCodeInsightFixtureTestCase() {
    override fun setUp() {
        super.setUp()
        myFixture.addFileToProject("generator.php", """
            <?php
            namespace Test;
            class Generator {
                public function generate(${'$'}name, array ${'$'}parameters = [], ${'$'}referenceType = 0) {}
            }
            class ChildGenerator extends Generator {}
            class OtherGenerator {
                public function generate(${'$'}name, array ${'$'}parameters = []) {}
            }
        """.trimIndent())
    }

    private fun literal(call: String): StringLiteralExpression {
        myFixture.configureByText(PhpFileType.INSTANCE, "<?php $call;")
        val leaf = myFixture.file.findElementAt(myFixture.caretOffset)

        return PsiTreeUtil.getParentOfType(leaf, StringLiteralExpression::class.java, false)!!
    }

    private fun stringMatch(call: String, index: Int = 0): MethodMatcher.MethodMatchParameter? =
        MethodMatcher.StringParameterMatcher(literal(call), index)
            .withSignature("\\Test\\Generator", "generate")
            .match()

    fun testPositionalStringAndUnrestrictedIndex() {
        assertNotNull(stringMatch("(new \\Test\\Generator)->generate('ro<caret>ute', [])"))
        assertNull(stringMatch("(new \\Test\\Generator)->generate('route', [], 'ro<caret>ute')"))
        assertNotNull(stringMatch("(new \\Test\\Generator)->generate('route', [], 'ro<caret>ute')", -1))
    }

    fun testNamedStringUsesDeclaredPosition() {
        assertNotNull(stringMatch("(new \\Test\\Generator)->generate(name: 'ro<caret>ute', parameters: [])"))
        assertNotNull(stringMatch("(new \\Test\\Generator)->generate(parameters: [], name: 'ro<caret>ute')"))
        assertNotNull(stringMatch("(new \\Test\\ChildGenerator)->generate(parameters: [], name: 'ro<caret>ute')"))
        assertNull(stringMatch("(new \\Test\\Generator)->generate(referenceType: 'ro<caret>ute', name: 'route')"))
        assertNull(stringMatch("(new \\Test\\Generator)->generate(unknown: 'ro<caret>ute')"))
    }

    fun testUnrelatedMethodDoesNotMatch() {
        assertNull(stringMatch("(new \\Test\\OtherGenerator)->generate(name: 'ro<caret>ute')"))
    }
}
