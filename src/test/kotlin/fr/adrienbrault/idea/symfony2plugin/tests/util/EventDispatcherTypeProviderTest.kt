package fr.adrienbrault.idea.symfony2plugin.tests.util

import com.intellij.patterns.PlatformPatterns
import com.jetbrains.php.lang.PhpFileType
import com.jetbrains.php.lang.psi.elements.Method
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 *
 * @see fr.adrienbrault.idea.symfony2plugin.util.EventDispatcherTypeProvider
 */
class EventDispatcherTypeProviderTest : SymfonyLightCodeInsightFixtureTestCase() {
    override fun setUp() {
        super.setUp()
        myFixture.copyFileToProject("EventDispatcher.php")
    }

    override fun getTestDataPath(): String {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/util/fixtures"
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.util.EventDispatcherTypeProvider
     */
    fun testEventDispatcherTypeSignature() {
        assertPhpReferenceSignatureContains(
            PhpFileType.INSTANCE,
            "<?php" +
                "/** @var \$d \\Symfony\\Component\\EventDispatcher\\EventDispatcherInterface \$d */\n" +
                "\$d->dispatch('foo', new \\FooEvent())->on<caret>Foo();",
            "#M#" + '\u0187' + "#M#C\\Symfony\\Component\\EventDispatcher\\EventDispatcherInterface.dispatch" + '\u0197' + "\\FooEvent.onFoo",
        )

        assertPhpReferenceResolveTo(
            PhpFileType.INSTANCE,
            "<?php" +
                "/** @var \$d \\Symfony\\Component\\EventDispatcher\\EventDispatcherInterface \$d */\n" +
                "\$d->dispatch('foo', new \\FooEvent())->on<caret>Foo();",
            PlatformPatterns.psiElement(Method::class.java).withName("onFoo"),
        )
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.util.EventDispatcherTypeProvider
     */
    fun testEventDispatcherTypeSignatureWithVariableReferences() {
        assertPhpReferenceResolveTo(
            PhpFileType.INSTANCE,
            "<?php" +
                "\$foo = new \\FooEvent();\n" +
                "/** @var \$d \\Symfony\\Component\\EventDispatcher\\EventDispatcherInterface \$d */\n" +
                "\$d->dispatch('foo', \$foo)->on<caret>Foo();",
            PlatformPatterns.psiElement(Method::class.java).withName("onFoo"),
        )
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.util.EventDispatcherTypeProvider
     */
    fun testEventDispatcherTypeSignatureProvidesExtendsClassNavigation() {
        assertPhpReferenceResolveTo(
            PhpFileType.INSTANCE,
            "<?php" +
                "/** @var \$d \\Symfony\\Component\\EventDispatcher\\EventDispatcherInterface \$d */\n" +
                "\$d->dispatch('foo', new \\FooEvent())->stopPropagation<caret>();",
            PlatformPatterns.psiElement(Method::class.java).withName("stopPropagation"),
        )
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.util.EventDispatcherTypeProvider
     */
    fun testEventDispatcherTypeSignatureNotSupported() {
        assertPhpReferenceSignatureContains(
            PhpFileType.INSTANCE,
            "<?php" +
                "/** @var \$d \\Symfony\\Component\\EventDispatcher\\EventDispatcherInterface \$d */\n" +
                "\$d->dispatch('foo', 'foo')->on<caret>Foo();",
            "#M#M#C\\Symfony\\Component\\EventDispatcher\\EventDispatcherInterface.dispatch.onFoo",
        )
    }
}
