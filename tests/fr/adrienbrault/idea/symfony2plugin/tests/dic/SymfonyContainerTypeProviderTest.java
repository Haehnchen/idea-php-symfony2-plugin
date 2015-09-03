package fr.adrienbrault.idea.symfony2plugin.tests.dic;

import com.intellij.patterns.PlatformPatterns;
import com.jetbrains.php.lang.PhpFileType;
import com.jetbrains.php.lang.psi.elements.Method;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

import java.io.File;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class SymfonyContainerTypeProviderTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void setUp() throws Exception {
        super.setUp();
        myFixture.configureFromExistingVirtualFile(myFixture.copyFileToProject("types.xml"));
        myFixture.configureFromExistingVirtualFile(myFixture.copyFileToProject("classes.php"));
    }

    public String getTestDataPath() {
        return new File(this.getClass().getResource("fixtures").getFile()).getAbsolutePath();
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.dic.SymfonyContainerTypeProvider
     */
    public void testContainerServicePhpType() {
        assertPhpReferenceResolveTo(PhpFileType.INSTANCE,
            "<?php" +
                "/** @var $container \\Symfony\\Component\\DependencyInjection\\ContainerInterface */\n" +
                "$container->get('foo')->for<caret>mat()",
            PlatformPatterns.psiElement(Method.class).withName("format")
        );

        assertPhpReferenceNotResolveTo(PhpFileType.INSTANCE,
            "<?php" +
                "/** @var $container \\Symfony\\Component\\DependencyInjection\\ContainerInterface */\n" +
                "$container->get('foo1')->for<caret>mat()",
            PlatformPatterns.psiElement(Method.class).withName("format")
        );

        assertPhpReferenceSignatureEquals(PhpFileType.INSTANCE,
            "<?php" +
                "/** @var $container \\Symfony\\Component\\DependencyInjection\\ContainerInterface */\n" +
                "$container->get('foo')->for<caret>mat()",
            "#M#" + '\u0150' + "#M#C\\Symfony\\Component\\DependencyInjection\\ContainerInterface.get" + '\u0180' + "foo.format"
        );
    }

}
