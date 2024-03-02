package fr.adrienbrault.idea.symfony2plugin.tests.doctrine;

import com.intellij.patterns.PlatformPatterns;
import com.jetbrains.php.lang.PhpFileType;
import com.jetbrains.php.lang.psi.elements.Method;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.doctrine.ObjectManagerFindContextTypeProvider
 */
public class ObjectManagerFindContextTypeProviderTest extends SymfonyLightCodeInsightFixtureTestCase {
    public void setUp() throws Exception {
        super.setUp();
        myFixture.copyFileToProject("ObjectManagerFindContextTypeProvider.orm.yml");
        myFixture.copyFileToProject("ObjectManagerFindContextTypeProvider.php");
    }

    public String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/doctrine/fixtures";
    }

    public void testThatEntityIsAttachedToVariables() {
        assertPhpReferenceResolveTo(PhpFileType.INSTANCE,
            "<?php\n" +
                "/* @var $er \\Foo\\BarRepository */" +
                "$er->find()->get<caret>Id();\n",
            PlatformPatterns.psiElement(Method.class).withName("getId")
        );

        assertPhpReferenceResolveTo(PhpFileType.INSTANCE,
            "<?php\n" +
                "/* @var $er \\Foo\\BarRepository */" +
                "$er->findOneByName()->get<caret>Id();\n",
            PlatformPatterns.psiElement(Method.class).withName("getId")
        );
    }

    public void testThatEntityIsAttachedToAVariableContext() {
        assertPhpReferenceResolveTo(PhpFileType.INSTANCE,
            "<?php\n" +
                "function(\\Foo\\BarRepository $er)\n" +
                "{\n" +
                "$er->find()->get<caret>Id();\n" +
                "}\n",
            PlatformPatterns.psiElement(Method.class).withName("getId")
        );
    }

    public void testThatEntityIsAttachedToConstructor() {
        // 2023.3.4 issue
        if (true) return;

        assertPhpReferenceResolveTo(PhpFileType.INSTANCE,
            "<?php\n" +
                "\n" +
                "class Foo\n" +
                "{\n" +
                "    private $er;\n" +
                "\n" +
                "    public function __construct(\\Foo\\BarRepository $er)\n" +
                "    {\n" +
                "        $this->er = $er;\n" +
                "    }\n" +
                "\n" +
                "    public function foo()\n" +
                "    {\n" +
                "        $this->er->find()->ge<caret>tId();\n" +
                "    }\n" +
                "}\n",
            PlatformPatterns.psiElement(Method.class).withName("getId")
        );
    }
}
