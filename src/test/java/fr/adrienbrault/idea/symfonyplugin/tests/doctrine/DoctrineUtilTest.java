package fr.adrienbrault.idea.symfonyplugin.tests.doctrine;

import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiFile;
import com.jetbrains.php.lang.PhpFileType;
import com.jetbrains.php.lang.psi.PhpPsiElementFactory;
import fr.adrienbrault.idea.symfonyplugin.doctrine.DoctrineUtil;
import fr.adrienbrault.idea.symfonyplugin.tests.SymfonyLightCodeInsightFixtureTestCase;

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
}
