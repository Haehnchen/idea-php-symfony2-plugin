package fr.adrienbrault.idea.symfony2plugin.tests.assistant.signature

import com.intellij.patterns.PlatformPatterns
import com.jetbrains.php.lang.PhpFileType
import com.jetbrains.php.lang.psi.elements.Method
import fr.adrienbrault.idea.symfony2plugin.Settings
import fr.adrienbrault.idea.symfony2plugin.assistant.signature.MethodSignatureSetting
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase

/**
 * @see fr.adrienbrault.idea.symfony2plugin.assistant.signature.MethodSignatureTypeProvider
 */
class MethodSignatureTypeProviderTest : SymfonyLightCodeInsightFixtureTestCase() {
    override fun setUp() {
        super.setUp()

        Settings.getInstance(project).objectSignatureTypeProvider = true
        Settings.getInstance(project).methodSignatureSettings = arrayListOf(
            MethodSignatureSetting("\\App\\Factory", "create", 0, "Class"),
        )
    }

    fun testThatConfiguredMethodSignatureResolvesClass() {
        assertPhpReferenceResolveTo(
            PhpFileType.INSTANCE,
            """
            <?php
            namespace App;

            class Factory
            {
                public function create(${'$'}class) {}
            }

            class Target
            {
                public function target() {}
            }

            (new Factory())->create('App\Target')->tar<caret>get();
            """.trimIndent(),
            PlatformPatterns.psiElement(Method::class.java).withName("target"),
        )
    }
}
