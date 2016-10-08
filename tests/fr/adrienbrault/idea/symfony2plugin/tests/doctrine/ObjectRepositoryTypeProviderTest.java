package fr.adrienbrault.idea.symfony2plugin.tests.doctrine;

import com.intellij.ide.plugins.PluginManager;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.patterns.PlatformPatterns;
import com.jetbrains.php.lang.PhpFileType;
import com.jetbrains.php.lang.psi.elements.Method;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

import java.io.File;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ObjectRepositoryTypeProviderTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void setUp() throws Exception {
        super.setUp();
        myFixture.configureFromExistingVirtualFile(myFixture.copyFileToProject("classes.php"));
    }

    public String getTestDataPath() {
        return new File(this.getClass().getResource("fixtures").getFile()).getAbsolutePath();
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.doctrine.ObjectRepositoryTypeProvider
     */
    public void testGetRepositoryResolveByRepository() {
        assertPhpReferenceResolveTo(PhpFileType.INSTANCE,
            "<?php" +
                "/** @var \\Doctrine\\Common\\Persistence\\ObjectManager $em */\n" +
                "$em->getRepository('\\Foo\\Bar')->b<caret>ar();",
            PlatformPatterns.psiElement(Method.class).withName("bar")
        );

        assertPhpReferenceSignatureContains(PhpFileType.INSTANCE,
            "<?php" +
                "/** @var \\Doctrine\\Common\\Persistence\\ObjectManager $em */\n" +
                "$em->getRepository('\\Foo\\Bar')->b<caret>ar();",
            "#M#" + '\u0151' + "#M#C\\Doctrine\\Common\\Persistence\\ObjectManager.getRepository" + '\u0185' + "\\Foo\\Bar.bar"
        );

        assertPhpReferenceResolveTo(PhpFileType.INSTANCE,
            "<?php" +
                "/** @var \\Doctrine\\Common\\Persistence\\ObjectManager $em */\n" +
                "$em->getRepository(\\Foo\\Bar::class)->b<caret>ar();",
            PlatformPatterns.psiElement(Method.class).withName("bar")
        );

    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.doctrine.ObjectRepositoryTypeProvider
     */
    public void testGetRepositoryResolveByRepositoryApiClassConstantCompatibility() {
        String result = "#M#" + '\u0151' + "#M#C\\Doctrine\\Common\\Persistence\\ObjectManager.getRepository" + '\u0185' + "#K#C\\Foo\\Bar.class.bar";

        assertPhpReferenceSignatureContains(PhpFileType.INSTANCE, "<?php" +
                "/** @var \\Doctrine\\Common\\Persistence\\ObjectManager $em */\n" +
                "$em->getRepository(\\Foo\\Bar::class)->b<caret>ar();",
            result
        );
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.doctrine.ObjectRepositoryTypeProvider
     */
    public void testGetRepositoryFallbackToPhpType() {
        assertPhpReferenceResolveTo(PhpFileType.INSTANCE,
            "<?php" +
                "/** @var \\Doctrine\\Common\\Persistence\\ObjectManager $em */\n" +
                "$em->getRepository('\\Foo\\Bar')->fi<caret>nd();",
            PlatformPatterns.psiElement(Method.class).withName("find")
        );

        assertPhpReferenceResolveTo(PhpFileType.INSTANCE,
            "<?php" +
                "/** @var \\Doctrine\\Common\\Persistence\\ObjectManager $em */\n" +
                "$em->getRepository('a')->fi<caret>nd();",
            PlatformPatterns.psiElement(Method.class).withName("find")
        );
    }
}
