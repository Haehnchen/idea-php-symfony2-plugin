package fr.adrienbrault.idea.symfony2plugin.tests.doctrine

import com.intellij.patterns.PlatformPatterns
import com.jetbrains.php.lang.PhpFileType
import com.jetbrains.php.lang.psi.elements.Method
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.doctrine.ObjectManagerFindTypeProvider
 */
class ObjectManagerFindTypeProviderTest : SymfonyLightCodeInsightFixtureTestCase() {
    override fun setUp() {
        super.setUp()
        myFixture.copyFileToProject("ObjectManagerFindTypeProvider.php")
    }

    override fun getTestDataPath(): String {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/doctrine/fixtures"
    }

    fun testThatObjectManagerFindMethodIsResolved() {
        assertPhpReferenceResolveTo(
            PhpFileType.INSTANCE,
            "<?php" +
                "/** @var \\Doctrine\\Common\\Persistence\\ObjectManager \$om */\n" +
                "\$om->find('\\Foo\\Bar', 'foobar')->get<caret>Id();",
            PlatformPatterns.psiElement(Method::class.java).withName("getId"),
        )

        assertPhpReferenceResolveTo(
            PhpFileType.INSTANCE,
            "<?php" +
                "/** @var \\Doctrine\\Common\\Persistence\\ObjectManager \$om */\n" +
                "\$om->find(\\Foo\\Bar::class, 'foobar')->get<caret>Id();",
            PlatformPatterns.psiElement(Method::class.java).withName("getId"),
        )
    }
}
