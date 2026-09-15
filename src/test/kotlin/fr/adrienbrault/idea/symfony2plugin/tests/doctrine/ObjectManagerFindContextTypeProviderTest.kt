package fr.adrienbrault.idea.symfony2plugin.tests.doctrine

import com.intellij.patterns.PlatformPatterns
import com.jetbrains.php.lang.PhpFileType
import com.jetbrains.php.lang.psi.elements.Method
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.doctrine.ObjectManagerFindContextTypeProvider
 */
class ObjectManagerFindContextTypeProviderTest : SymfonyLightCodeInsightFixtureTestCase() {
    override fun setUp() {
        super.setUp()
        myFixture.copyFileToProject("ObjectManagerFindContextTypeProvider.orm.yml")
        myFixture.copyFileToProject("ObjectManagerFindContextTypeProvider.php")
    }

    override fun getTestDataPath(): String {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/doctrine/fixtures"
    }

    fun testThatEntityIsAttachedToVariables() {
        assertPhpReferenceResolveTo(
            PhpFileType.INSTANCE,
            "<?php\n" +
                "/* @var \$er \\Foo\\BarRepository */" +
                "\$er->find()->get<caret>Id();\n",
            PlatformPatterns.psiElement(Method::class.java).withName("getId"),
        )

        assertPhpReferenceResolveTo(
            PhpFileType.INSTANCE,
            "<?php\n" +
                "/* @var \$er \\Foo\\BarRepository */" +
                "\$er->findOneByName()->get<caret>Id();\n",
            PlatformPatterns.psiElement(Method::class.java).withName("getId"),
        )
    }

    fun testThatEntityIsAttachedToAVariableContext() {
        assertPhpReferenceResolveTo(
            PhpFileType.INSTANCE,
            "<?php\n" +
                "function(\\Foo\\BarRepository \$er)\n" +
                "{\n" +
                "\$er->find()->get<caret>Id();\n" +
                "}\n",
            PlatformPatterns.psiElement(Method::class.java).withName("getId"),
        )
    }
}
