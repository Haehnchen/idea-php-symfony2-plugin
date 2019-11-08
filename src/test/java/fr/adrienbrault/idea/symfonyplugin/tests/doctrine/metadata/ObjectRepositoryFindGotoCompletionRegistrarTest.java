package fr.adrienbrault.idea.symfonyplugin.tests.doctrine.metadata;

import com.intellij.patterns.PlatformPatterns;
import com.jetbrains.php.lang.PhpFileType;
import fr.adrienbrault.idea.symfonyplugin.tests.SymfonyLightCodeInsightFixtureTestCase;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfonyplugin.doctrine.metadata.ObjectRepositoryFindGotoCompletionRegistrar
 */
public class ObjectRepositoryFindGotoCompletionRegistrarTest extends SymfonyLightCodeInsightFixtureTestCase {
    public void setUp() throws Exception {
        super.setUp();
        myFixture.copyFileToProject("ObjectRepositoryFindGotoCompletionRegistrar.php");
    }

    public String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfonyplugin/tests/doctrine/metadata/fixtures";
    }

    public void testThatCompletionForDoctrineMetadataInArrayIsProvided() {
        for (String s : new String[]{"findBy", "findOneBy"}) {
            assertCompletionContains(PhpFileType.INSTANCE, "<?php\n" +
                    "/** @var $em \\Doctrine\\Common\\Persistence\\ObjectManager */\n" +
                    "$em->getRepository('Foo\\Bar')->" + s + "(['<caret>'])",
                "phonenumbers", "email"
            );

            assertCompletionContains(PhpFileType.INSTANCE, "<?php\n" +
                    "/** @var $em \\Doctrine\\Common\\Persistence\\ObjectManager */\n" +
                    "$em->getRepository('Foo\\Bar')->" + s + "(['foo', '<caret>' => 'foo'])",
                "phonenumbers", "email"
            );

            assertCompletionContains(PhpFileType.INSTANCE, "<?php\n" +
                    "/** @var $em \\Doctrine\\Common\\Persistence\\ObjectManager */\n" +
                    "$em->getRepository('Foo\\Bar')->" + s + "(['foo' => 'foo', '<caret>' => 'foo'])",
                "phonenumbers", "email"
            );
        }
    }

    public void testThatNavigationForDoctrineMetadataInArrayIsProvided() {
        for (String s : new String[]{"findBy", "findOneBy"}) {
            assertNavigationMatch(PhpFileType.INSTANCE, "<?php\n" +
                    "/** @var $em \\Doctrine\\Common\\Persistence\\ObjectManager */\n" +
                    "$em->getRepository('Foo\\Bar')->" + s + "(['phonen<caret>umbers'])",
                PlatformPatterns.psiElement()
            );

            assertNavigationMatch(PhpFileType.INSTANCE, "<?php\n" +
                    "/** @var $em \\Doctrine\\Common\\Persistence\\ObjectManager */\n" +
                    "$em->getRepository('Foo\\Bar')->" + s + "(['foo', 'phonen<caret>umbers' => 'foo'])",
                PlatformPatterns.psiElement()
            );

            assertNavigationMatch(PhpFileType.INSTANCE, "<?php\n" +
                    "/** @var $em \\Doctrine\\Common\\Persistence\\ObjectManager */\n" +
                    "$em->getRepository('Foo\\Bar')->" + s + "(['phonen<caret>umbers'])",
                PlatformPatterns.psiElement()
            );
        }
    }

    public void testThatRepositoryIsResolved() {
        assertCompletionContains(PhpFileType.INSTANCE, "<?php\n" +
                "/** @var $r \\Foo\\Repository\\BarRepository */\n" +
                "$r->findBy(['<caret>'])",
            "phonenumbers", "email"
        );

        assertCompletionContains(PhpFileType.INSTANCE, "<?php\n" +
            "namespace Foo\\Repository;\n" +
            "" +
            "class BarRepository implements \\Doctrine\\Common\\Persistence\\ObjectRepository" +
            "{\n" +
            "   function foo()\n" +
            "   {\n" +
            "       $this->findBy(['<caret>'])"+
            "   }\n" +
            "}\n" +
            "phonenumbers", "email"
        );
    }
}
