package fr.adrienbrault.idea.symfony2plugin.tests.dic

import com.intellij.patterns.PlatformPatterns
import com.jetbrains.php.lang.PhpFileType
import com.jetbrains.php.lang.psi.elements.Method
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.dic.SymfonyContainerTypeProvider
 */
class SymfonyContainerTypeProviderTest : SymfonyLightCodeInsightFixtureTestCase() {
    override fun setUp() {
        super.setUp()
        myFixture.copyFileToProject("types.xml")
        myFixture.copyFileToProject("types2.xml")
        myFixture.copyFileToProject("classes.php")
    }

    override fun getTestDataPath(): String {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/dic/fixtures"
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.dic.SymfonyContainerTypeProvider
     */
    fun testContainerServicePhpType() {
        assertPhpReferenceResolveTo(
            PhpFileType.INSTANCE,
            "<?php" +
                "/** @var \$container \\Symfony\\Component\\DependencyInjection\\ContainerInterface */\n" +
                "\$container->get('foo')->for<caret>mat()",
            PlatformPatterns.psiElement(Method::class.java).withName("format"),
        )

        assertPhpReferenceNotResolveTo(
            PhpFileType.INSTANCE,
            "<?php" +
                "/** @var \$container \\Symfony\\Component\\DependencyInjection\\ContainerInterface */\n" +
                "\$container->get('foo1')->for<caret>mat()",
            PlatformPatterns.psiElement(Method::class.java).withName("format"),
        )

        assertPhpReferenceSignatureContains(
            PhpFileType.INSTANCE,
            "<?php" +
                "/** @var \$container \\Symfony\\Component\\DependencyInjection\\ContainerInterface */\n" +
                "\$container->get('foo')->for<caret>mat()",
            "#M#" + '\u0150' + "#M#C\\Symfony\\Component\\DependencyInjection\\ContainerInterface.get" + '\u0182' + "foo.format",
        )
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.dic.SymfonyContainerTypeProvider
     */
    fun testThatContainerServiceTypeResolvesOnFirstParameterAndAllowMultipleParameter() {
        assertPhpReferenceResolveTo(
            PhpFileType.INSTANCE,
            "<?php" +
                "/** @var \$container \\Symfony\\Component\\DependencyInjection\\ContainerInterface */\n" +
                "\$container->get('foo', 'foobar')->for<caret>mat()",
            PlatformPatterns.psiElement(Method::class.java).withName("format"),
        )
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.dic.SymfonyContainerTypeProvider
     */
    fun testThatDuplicateServiceClassInstancesAreMerged() {
        assertPhpReferenceResolveTo(
            PhpFileType.INSTANCE,
            "<?php" +
                "/** @var \$d \\Symfony\\Component\\DependencyInjection\\ContainerInterface */\n" +
                "\$d->get('foo.bar')->for<caret>mat();",
            PlatformPatterns.psiElement(Method::class.java).withName("format"),
        )

        assertPhpReferenceResolveTo(
            PhpFileType.INSTANCE,
            "<?php" +
                "/** @var \$d \\Symfony\\Component\\DependencyInjection\\ContainerInterface */\n" +
                "\$d->get('foo.bar')->get<caret>Bar();",
            PlatformPatterns.psiElement(Method::class.java).withName("getBar"),
        )
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.dic.SymfonyContainerTypeProvider
     */
    fun testThatNonContainerGetMethodDoesNotResolveToServiceClass() {
        assertPhpReferenceNotResolveTo(
            PhpFileType.INSTANCE,
            "<?php\n" +
                "class CustomServiceLocator {\n" +
                "    public function get(\$id) { return new CustomResult(); }\n" +
                "}\n" +
                "class CustomResult {}\n" +
                "\$locator = new CustomServiceLocator();\n" +
                "\$locator->get('foo')->for<caret>mat();\n",
            PlatformPatterns.psiElement(Method::class.java).withName("format"),
        )
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.dic.SymfonyContainerTypeProvider
     */
    fun testThatClassConstantResolves() {
        assertPhpReferenceResolveTo(
            PhpFileType.INSTANCE,
            "<?php" +
                "/** @var \$d \\Symfony\\Component\\DependencyInjection\\ContainerInterface */\n" +
                "\$d->get(MyDateTime::class)->for<caret>mat();",
            PlatformPatterns.psiElement(Method::class.java).withName("format"),
        )
    }
}
