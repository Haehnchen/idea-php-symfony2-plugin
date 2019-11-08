package fr.adrienbrault.idea.symfonyplugin.tests.doctrine;

import com.intellij.patterns.PlatformPatterns;
import com.jetbrains.php.lang.PhpFileType;
import com.jetbrains.php.lang.psi.elements.Method;
import fr.adrienbrault.idea.symfonyplugin.tests.SymfonyLightCodeInsightFixtureTestCase;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfonyplugin.doctrine.ObjectManagerFindTypeProvider
 */
public class ObjectManagerFindTypeProviderTest extends SymfonyLightCodeInsightFixtureTestCase {
    public void setUp() throws Exception {
        super.setUp();
        myFixture.copyFileToProject("ObjectManagerFindTypeProvider.php");
    }

    public String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfonyplugin/tests/doctrine/fixtures";
    }

    public void testThatObjectManagerFindMethodIsResolved() {
        assertPhpReferenceResolveTo(PhpFileType.INSTANCE,
            "<?php" +
                "/** @var \\Doctrine\\Common\\Persistence\\ObjectManager $om */\n" +
                "$om->find('\\Foo\\Bar', 'foobar')->get<caret>Id();",
            PlatformPatterns.psiElement(Method.class).withName("getId")
        );

        assertPhpReferenceResolveTo(PhpFileType.INSTANCE,
            "<?php" +
                "/** @var \\Doctrine\\Common\\Persistence\\ObjectManager $om */\n" +
                "$om->find(\\Foo\\Bar::class, 'foobar')->get<caret>Id();",
            PlatformPatterns.psiElement(Method.class).withName("getId")
        );
    }
}
