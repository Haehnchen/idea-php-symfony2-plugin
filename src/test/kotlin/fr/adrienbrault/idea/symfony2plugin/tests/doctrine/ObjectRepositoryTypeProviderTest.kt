package fr.adrienbrault.idea.symfony2plugin.tests.doctrine

import com.intellij.patterns.PlatformPatterns
import com.jetbrains.php.lang.PhpFileType
import com.jetbrains.php.lang.psi.elements.Method
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
class ObjectRepositoryTypeProviderTest : SymfonyLightCodeInsightFixtureTestCase() {
    override fun setUp() {
        super.setUp()
        myFixture.configureFromExistingVirtualFile(myFixture.copyFileToProject("classes.php"))
    }

    override fun getTestDataPath(): String {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/doctrine/fixtures"
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.doctrine.ObjectRepositoryTypeProvider
     */
    fun testGetRepositoryResolveByRepository() {
        assertPhpReferenceResolveTo(
            PhpFileType.INSTANCE,
            "<?php" +
                "/** @var \\Doctrine\\Common\\Persistence\\ObjectManager \$em */\n" +
                "\$em->getRepository('\\Foo\\Bar')->b<caret>ar();",
            PlatformPatterns.psiElement(Method::class.java).withName("bar"),
        )

        assertPhpReferenceSignatureContains(
            PhpFileType.INSTANCE,
            "<?php" +
                "/** @var \\Doctrine\\Common\\Persistence\\ObjectManager \$em */\n" +
                "\$em->getRepository('\\Foo\\Bar')->b<caret>ar();",
            "#M#" + '\u0151' + "#M#C\\Doctrine\\Common\\Persistence\\ObjectManager.getRepository" + '\u0185' + "\\Foo\\Bar.bar",
        )

        assertPhpReferenceResolveTo(
            PhpFileType.INSTANCE,
            "<?php" +
                "/** @var \\Doctrine\\Common\\Persistence\\ObjectManager \$em */\n" +
                "\$em->getRepository(\\Foo\\Bar::class)->b<caret>ar();",
            PlatformPatterns.psiElement(Method::class.java).withName("bar"),
        )
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.doctrine.ObjectRepositoryTypeProvider
     */
    fun testGetRepositoryResolveByRepositoryApiClassConstantCompatibility() {
        val result = "#M#" + '\u0151' + "#M#C\\Doctrine\\Common\\Persistence\\ObjectManager.getRepository" + '\u0185' + "#K#C\\Foo\\Bar.class.bar"

        assertPhpReferenceSignatureContains(
            PhpFileType.INSTANCE,
            "<?php" +
                "/** @var \\Doctrine\\Common\\Persistence\\ObjectManager \$em */\n" +
                "\$em->getRepository(\\Foo\\Bar::class)->b<caret>ar();",
            result,
        )
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.doctrine.ObjectRepositoryTypeProvider
     */
    fun testGetRepositoryFallbackToPhpType() {
        assertPhpReferenceResolveTo(
            PhpFileType.INSTANCE,
            "<?php" +
                "/** @var \\Doctrine\\Common\\Persistence\\ObjectManager \$em */\n" +
                "\$em->getRepository('\\Foo\\Bar')->fi<caret>nd();",
            PlatformPatterns.psiElement(Method::class.java).withName("find"),
        )

        assertPhpReferenceResolveTo(
            PhpFileType.INSTANCE,
            "<?php" +
                "/** @var \\Doctrine\\Common\\Persistence\\ObjectManager \$em */\n" +
                "\$em->getRepository('a')->fi<caret>nd();",
            PlatformPatterns.psiElement(Method::class.java).withName("find"),
        )
    }
}
