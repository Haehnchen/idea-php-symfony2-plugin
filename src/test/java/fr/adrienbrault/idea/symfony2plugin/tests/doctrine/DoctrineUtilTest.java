package fr.adrienbrault.idea.symfony2plugin.tests.doctrine;

import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiFile;
import com.jetbrains.php.lang.PhpFileType;
import com.jetbrains.php.lang.psi.PhpPsiElementFactory;
import fr.adrienbrault.idea.symfony2plugin.doctrine.DoctrineUtil;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

import java.util.Collection;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class DoctrineUtilTest extends SymfonyLightCodeInsightFixtureTestCase {
    /**
     * @see DoctrineUtil#getClassRepositoryPair
     */
    public void testGetClassRepositoryPairForStringValue() {
        PsiFile psiFileFromText = PhpPsiElementFactory.createPsiFileFromText(getProject(), "" +
            "<?php\n" +
            "\n" +
            "namespace Foo;\n" +
            "\n" +
            "use Doctrine\\ORM\\Mapping as ORM;\n" +
            "\n" +
            "/**\n" +
            " * @ORM\\Entity(repositoryClass=\"MyBundle\\Entity\\Repository\\AddressRepository\")\n" +
            " */\n" +
            "class Apple {\n" +
            "}\n"
        );

        Collection<Pair<String, String>> classRepositoryPair = DoctrineUtil.getClassRepositoryPair(psiFileFromText);

        Pair<String, String> next = classRepositoryPair.iterator().next();

        assertEquals("Foo\\Apple", next.getFirst());
        assertEquals("MyBundle\\Entity\\Repository\\AddressRepository", next.getSecond());
    }

    /**
     * @see DoctrineUtil#getClassRepositoryPair
     */
    public void testGetClassRepositoryPairForPhp8AttributeStringValue() {
        myFixture.configureByText(PhpFileType.INSTANCE, "<?php class Foobar {};");

        PsiFile psiFileFromText = PhpPsiElementFactory.createPsiFileFromText(getProject(), "" +
            "<?php\n" +
            "namespace Foo;\n" +
            "\n" +
            "use Foobar;\n" +
            "use Doctrine\\ORM\\Mapping\\Entity;\n" +
            "\n" +
            "#[Entity(repositoryClass: Foobar::class, readOnly: false)]\n" +
            "class Apple\n" +
            "{\n" +
            "}\n" +
            "\n" +
            "\n" +
            "#[Entity()]\n" +
            "class Car\n" +
            "{\n" +
            "}\n" +
            "\n"
        );

        Collection<Pair<String, String>> classRepositoryPair = DoctrineUtil.getClassRepositoryPair(psiFileFromText);

        Pair<String, String> apple = classRepositoryPair.stream().filter(stringStringPair -> "Foo\\Apple".equals(stringStringPair.getFirst())).findFirst().get();
        assertEquals("Foo\\Apple", apple.getFirst());
        assertEquals("Foobar", apple.getSecond());

        Pair<String, String> car = classRepositoryPair.stream().filter(stringStringPair -> "Foo\\Car".equals(stringStringPair.getFirst())).findFirst().get();
        assertEquals("Foo\\Car", car.getFirst());
        assertNull(car.getSecond());
    }

    /**
     * @see DoctrineUtil#getClassRepositoryPair
     */
    public void testGetClassRepositoryPairForClassConstant() {
        myFixture.configureByText(PhpFileType.INSTANCE, "<?php class Foobar {};");

        PsiFile psiFileFromText = PhpPsiElementFactory.createPsiFileFromText(getProject(), "<?php\n" +
            "\n" +
            "namespace Foo;\n" +
            "\n" +
            "use Doctrine\\ORM\\Mapping as ORM;\n" +
            "use Foobar;\n" +
            "\n" +
            "/**\n" +
            " * @ORM\\Entity(repositoryClass=Foobar::class)\n" +
            " */\n" +
            "class Apple {\n" +
            "}\n"
        );

        Collection<Pair<String, String>> classRepositoryPair = DoctrineUtil.getClassRepositoryPair(psiFileFromText);

        Pair<String, String> next = classRepositoryPair.iterator().next();

        assertEquals("Foo\\Apple", next.getFirst());
        assertEquals("Foobar", next.getSecond());
    }

    public void testGetClassRepositoryPairForClassConstanta() {
        myFixture.configureByText(PhpFileType.INSTANCE, "<?php\n" +
            "namespace Bar;\n" +
            "" +
            "class Foobar {};\n"
        );

        PsiFile psiFileFromText = PhpPsiElementFactory.createPsiFileFromText(getProject(), "<?php\n" +
            "\n" +
            "namespace Foo;\n" +
            "\n" +
            "use Doctrine\\ORM\\Mapping as ORM;\n" +
            "use Bar\\Foobar;\n" +
            "use Bar\\Foobar as Car;\n" +
            "use Bar as BarAlias;\n" +
            "" +
            "\n" +
            "/**\n" +
            " * @ORM\\Entity(repositoryClass=Foobar::class)\n" +
            " */\n" +
            "class Apple {}\n" +
            "" +
            "/**\n" +
            " * @ORM\\Entity(repositoryClass=Car::class)\n" +
            " */\n" +
            "class Banana {}\n" +
            "/**\n" +
            " * @ORM\\Entity(repositoryClass=\\Bar\\Foobar::class)\n" +
            " */\n" +
            "class Yellow {}\n" +
            "/**\n" +
            " * @ORM\\Entity(repositoryClass=BarAlias\\Foobar::class)\n" +
            " */\n" +
            "class Red {}\n" +
            "/**\n" +
            " * @ORM\\Entity(repositoryClass=\"BarAlias\\Foobar\")\n" +
            " */\n" +
            "class Black {}\n" +
            "/**\n" +
            " * @ORM\\Entity(repositoryClass=\"Foobar\")\n" +
            " */\n" +
            "class White {}\n"
        );

        Collection<Pair<String, String>> classRepositoryPair = DoctrineUtil.getClassRepositoryPair(psiFileFromText);

        Pair<String, String> apple = classRepositoryPair.stream().filter(stringStringPair -> "Foo\\Apple".equals(stringStringPair.getFirst())).findFirst().get();
        assertEquals("Bar\\Foobar", apple.getSecond());

        Pair<String, String> banana = classRepositoryPair.stream().filter(stringStringPair -> "Foo\\Banana".equals(stringStringPair.getFirst())).findFirst().get();
        assertEquals("Bar\\Foobar", banana.getSecond());

        Pair<String, String> yellow = classRepositoryPair.stream().filter(stringStringPair -> "Foo\\Yellow".equals(stringStringPair.getFirst())).findFirst().get();
        assertEquals("Bar\\Foobar", yellow.getSecond());

        Pair<String, String> black = classRepositoryPair.stream().filter(stringStringPair -> "Foo\\Black".equals(stringStringPair.getFirst())).findFirst().get();
        assertEquals("BarAlias\\Foobar", black.getSecond());

        Pair<String, String> white = classRepositoryPair.stream().filter(stringStringPair -> "Foo\\White".equals(stringStringPair.getFirst())).findFirst().get();
        assertEquals("Foo\\Foobar", white.getSecond());
    }
}
