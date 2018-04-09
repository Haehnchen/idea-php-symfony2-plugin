package fr.adrienbrault.idea.symfony2plugin.tests.dic;

import com.intellij.patterns.PlatformPatterns;
import com.jetbrains.php.lang.PhpFileType;
import com.jetbrains.php.lang.psi.elements.Method;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

import java.io.File;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.dic.SymfonyContainerTypeProvider
 */
public class SymfonyContainerTypeProviderTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void setUp() throws Exception {
        super.setUp();
        myFixture.copyFileToProject("types.xml");
        myFixture.copyFileToProject("types2.xml");
        myFixture.copyFileToProject("classes.php");
    }

    public String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/dic/fixtures";
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

        assertPhpReferenceSignatureContains(PhpFileType.INSTANCE,
            "<?php" +
                "/** @var $container \\Symfony\\Component\\DependencyInjection\\ContainerInterface */\n" +
                "$container->get('foo')->for<caret>mat()",
            "#M#" + '\u0150' + "#M#C\\Symfony\\Component\\DependencyInjection\\ContainerInterface.get" + '\u0182' + "foo.format"
        );
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.dic.SymfonyContainerTypeProvider
     */
    public void testThatContainerServiceTypeResolvesOnFirstParameterAndAllowMultipleParameter() {
        assertPhpReferenceResolveTo(PhpFileType.INSTANCE,
            "<?php" +
                "/** @var $container \\Symfony\\Component\\DependencyInjection\\ContainerInterface */\n" +
                "$container->get('foo', 'foobar')->for<caret>mat()",
            PlatformPatterns.psiElement(Method.class).withName("format")
        );
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.dic.SymfonyContainerTypeProvider
     */
    public void testThatDuplicateServiceClassInstancesAreMerged() {
        assertPhpReferenceResolveTo(PhpFileType.INSTANCE,
            "<?php" +
                "/** @var $d \\Symfony\\Component\\DependencyInjection\\ContainerInterface */\n" +
                "$d->get('foo.bar')->for<caret>mat();",
            PlatformPatterns.psiElement(Method.class).withName("format")
        );

        assertPhpReferenceResolveTo(PhpFileType.INSTANCE,
            "<?php" +
                "/** @var $d \\Symfony\\Component\\DependencyInjection\\ContainerInterface */\n" +
                "$d->get('foo.bar')->get<caret>Bar();",
            PlatformPatterns.psiElement(Method.class).withName("getBar")
        );
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.dic.SymfonyContainerTypeProvider
     */
    public void testThatClassConstantResolves() {
        assertPhpReferenceResolveTo(PhpFileType.INSTANCE,
            "<?php" +
                "/** @var $d \\Symfony\\Component\\DependencyInjection\\ContainerInterface */\n" +
                "$d->get(MyDateTime::class)->for<caret>mat();",
            PlatformPatterns.psiElement(Method.class).withName("format")
        );
    }
}
