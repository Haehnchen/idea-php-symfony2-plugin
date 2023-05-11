package fr.adrienbrault.idea.symfony2plugin.tests.doctrine;

import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiFile;
import com.jetbrains.php.lang.PhpFileType;
import com.jetbrains.php.lang.psi.PhpPsiElementFactory;
import fr.adrienbrault.idea.symfony2plugin.doctrine.DoctrineUtil;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import org.jetbrains.yaml.YAMLFileType;
import org.jetbrains.yaml.psi.YAMLFile;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class DoctrineUtilTest extends SymfonyLightCodeInsightFixtureTestCase {
    public String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/doctrine/fixtures";
    }

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

    public void testGetClassRepositoryPairForClassConstantForYaml() {
        YAMLFile yamlFile = (YAMLFile) myFixture.configureByText(YAMLFileType.YML, "Doctrine\\Tests\\ORM\\Mapping\\User:\n" +
            "  type: entity\n" +
            "  fields: {}"
        );

        assertTrue(Objects.requireNonNull(DoctrineUtil.getClassRepositoryPair(yamlFile)).stream().anyMatch(
            stringStringPair -> "Doctrine\\Tests\\ORM\\Mapping\\User".equals(stringStringPair.getFirst())
        ));

        YAMLFile yamlFile2 = (YAMLFile) myFixture.configureByText(YAMLFileType.YML, "Doctrine\\Tests\\ORM\\Mapping\\User:\n" +
            "  type: entity2\n"
        );

        assertNull(DoctrineUtil.getClassRepositoryPair(yamlFile2));

        YAMLFile yamlFile3 = (YAMLFile) myFixture.configureByText(YAMLFileType.YML, "User:\n" +
            "  type: entity\n" +
            "  embedded:\n" +
            "    address:\n" +
            "      class: Address"
        );

        assertTrue(Objects.requireNonNull(DoctrineUtil.getClassRepositoryPair(yamlFile3)).stream().anyMatch(
            stringStringPair -> "User".equals(stringStringPair.getFirst())
        ));

        YAMLFile yamlFile4 = (YAMLFile) myFixture.configureByText(YAMLFileType.YML, "MyProject\\Model\\Admin:\n" +
            "  type: entity\n" +
            "  associationOverride:\n" +
            "    address:\n" +
            "      joinColumn:\n" +
            "        adminaddress_id:\n" +
            "          name: adminaddress_id\n" +
            "          referencedColumnName: id"
        );

        assertTrue(Objects.requireNonNull(DoctrineUtil.getClassRepositoryPair(yamlFile4)).stream().anyMatch(
            stringStringPair -> "MyProject\\Model\\Admin".equals(stringStringPair.getFirst())
        ));

        YAMLFile yamlFile5 = (YAMLFile) myFixture.configureByText(YAMLFileType.YML, "MyProject\\Model\\User2:\n" +
            "  type: mappedSuperclass\n" +
            "  # other fields mapping\n" +
            "  manyToOne:\n" +
            "    address:\n" +
            "      targetEntity: Address\n"
        );

        assertTrue(Objects.requireNonNull(DoctrineUtil.getClassRepositoryPair(yamlFile5)).stream().anyMatch(
            stringStringPair -> "MyProject\\Model\\User2".equals(stringStringPair.getFirst())
        ));

        YAMLFile yamlFile6 = (YAMLFile) myFixture.configureByText(YAMLFileType.YML, "\\Doctrine\\Foobar:\n" +
            "  type: entity\n" +
            "  fields: {}"
        );

        assertTrue(Objects.requireNonNull(DoctrineUtil.getClassRepositoryPair(yamlFile6)).stream().anyMatch(
            stringStringPair -> "\\Doctrine\\Foobar".equals(stringStringPair.getFirst())
        ));

        YAMLFile yamlFile7 = (YAMLFile) myFixture.configureByText(YAMLFileType.YML, "'\\Doctrine\\Foobar':\n" +
            "  type: entity\n" +
            "  fields: {}"
        );

        assertTrue(Objects.requireNonNull(DoctrineUtil.getClassRepositoryPair(yamlFile7)).stream().anyMatch(
            stringStringPair -> "\\Doctrine\\Foobar".equals(stringStringPair.getFirst())
        ));
    }

    public void testGetClassRepositoryPairForClassConstantForYamlNoMatch() {
        YAMLFile yamlFile = (YAMLFile) myFixture.configureByText(YAMLFileType.YML, "Foobar:\n" +
            "    id: foobar\n" +
            "    type: foo\n"
        );

        assertNull(DoctrineUtil.getClassRepositoryPair(yamlFile));

        YAMLFile yamlFile2 = (YAMLFile) myFixture.configureByText(YAMLFileType.YML, "Doctrine\\Tes-ts\\ORM\\Mapping\\User:\n" +
            "  type: entity\n" +
            "  fields: {}"
        );

        assertNull(DoctrineUtil.getClassRepositoryPair(yamlFile2));
    }

    public void testGetClassRepositoryPairForClassConstantForYamlForOdm() {
        YAMLFile yamlFile = (YAMLFile) myFixture.configureByText(YAMLFileType.YML, "Documents\\User:\n" +
            "  db: documents\n" +
            "  collection: user\n" +
            "  fields: {}"
        );

        assertTrue(Objects.requireNonNull(DoctrineUtil.getClassRepositoryPair(yamlFile)).stream().anyMatch(
            stringStringPair -> "Documents\\User".equals(stringStringPair.getFirst())
        ));

        YAMLFile yamlFile2 = (YAMLFile) myFixture.configureByText(YAMLFileType.YML, "User:\n" +
            "  type: document\n" +
            "  embedOne:\n" +
            "    address:\n" +
            "      targetDocument: Address"
        );

        assertTrue(Objects.requireNonNull(DoctrineUtil.getClassRepositoryPair(yamlFile2)).stream().anyMatch(
            stringStringPair -> "User".equals(stringStringPair.getFirst())
        ));
    }

    public void testGetDoctrineOrmFunctions() {
        myFixture.copyFileToProject("doctrine_function_node.php");

        Map<String, String> doctrineOrmFunctions = DoctrineUtil.getDoctrineOrmFunctions(getProject());

        assertEquals("\\Doctrine\\ORM\\Query\\Functions\\MinFunction", doctrineOrmFunctions.get("min"));

        assertContainsElements(
            doctrineOrmFunctions.keySet(),
            "min",
            "length",
            "substring",
            "current_time"
        );
    }
}
