package fr.adrienbrault.idea.symfony2plugin.tests.doctrine;

import fr.adrienbrault.idea.symfony2plugin.doctrine.dict.DoctrineClassMetadata;
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

        Collection<DoctrineClassMetadata> classRepositoryPair = DoctrineUtil.getClassRepositoryPair(psiFileFromText);

        DoctrineClassMetadata next = classRepositoryPair.iterator().next();

        assertEquals("Foo\\Apple", next.className());
        assertEquals("MyBundle\\Entity\\Repository\\AddressRepository", next.repositoryClass());
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

        Collection<DoctrineClassMetadata> classRepositoryPair = DoctrineUtil.getClassRepositoryPair(psiFileFromText);

        DoctrineClassMetadata apple = classRepositoryPair.stream().filter(m -> "Foo\\Apple".equals(m.className())).findFirst().get();
        assertEquals("Foo\\Apple", apple.className());
        assertEquals("Foobar", apple.repositoryClass());

        DoctrineClassMetadata car = classRepositoryPair.stream().filter(m -> "Foo\\Car".equals(m.className())).findFirst().get();
        assertEquals("Foo\\Car", car.className());
        assertNull(car.repositoryClass());
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

        Collection<DoctrineClassMetadata> classRepositoryPair = DoctrineUtil.getClassRepositoryPair(psiFileFromText);

        DoctrineClassMetadata next = classRepositoryPair.iterator().next();

        assertEquals("Foo\\Apple", next.className());
        assertEquals("Foobar", next.repositoryClass());
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

        Collection<DoctrineClassMetadata> classRepositoryPair = DoctrineUtil.getClassRepositoryPair(psiFileFromText);

        DoctrineClassMetadata apple = classRepositoryPair.stream().filter(m -> "Foo\\Apple".equals(m.className())).findFirst().get();
        assertEquals("Bar\\Foobar", apple.repositoryClass());

        DoctrineClassMetadata banana = classRepositoryPair.stream().filter(m -> "Foo\\Banana".equals(m.className())).findFirst().get();
        assertEquals("Bar\\Foobar", banana.repositoryClass());

        DoctrineClassMetadata yellow = classRepositoryPair.stream().filter(m -> "Foo\\Yellow".equals(m.className())).findFirst().get();
        assertEquals("Bar\\Foobar", yellow.repositoryClass());

        DoctrineClassMetadata black = classRepositoryPair.stream().filter(m -> "Foo\\Black".equals(m.className())).findFirst().get();
        assertEquals("BarAlias\\Foobar", black.repositoryClass());

        DoctrineClassMetadata white = classRepositoryPair.stream().filter(m -> "Foo\\White".equals(m.className())).findFirst().get();
        assertEquals("Foo\\Foobar", white.repositoryClass());
    }

    public void testGetClassRepositoryPairForClassConstantForYaml() {
        YAMLFile yamlFile = (YAMLFile) myFixture.configureByText(YAMLFileType.YML, "Doctrine\\Tests\\ORM\\Mapping\\User:\n" +
            "  type: entity\n" +
            "  fields: {}"
        );

        assertTrue(Objects.requireNonNull(DoctrineUtil.getClassRepositoryPair(yamlFile)).stream().anyMatch(
            m -> "Doctrine\\Tests\\ORM\\Mapping\\User".equals(m.className())
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
            m -> "User".equals(m.className())
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
            m -> "MyProject\\Model\\Admin".equals(m.className())
        ));

        YAMLFile yamlFile5 = (YAMLFile) myFixture.configureByText(YAMLFileType.YML, "MyProject\\Model\\User2:\n" +
            "  type: mappedSuperclass\n" +
            "  # other fields mapping\n" +
            "  manyToOne:\n" +
            "    address:\n" +
            "      targetEntity: Address\n"
        );

        assertTrue(Objects.requireNonNull(DoctrineUtil.getClassRepositoryPair(yamlFile5)).stream().anyMatch(
            m -> "MyProject\\Model\\User2".equals(m.className())
        ));

        YAMLFile yamlFile6 = (YAMLFile) myFixture.configureByText(YAMLFileType.YML, "\\Doctrine\\Foobar:\n" +
            "  type: entity\n" +
            "  fields: {}"
        );

        assertTrue(Objects.requireNonNull(DoctrineUtil.getClassRepositoryPair(yamlFile6)).stream().anyMatch(
            m -> "\\Doctrine\\Foobar".equals(m.className())
        ));

        YAMLFile yamlFile7 = (YAMLFile) myFixture.configureByText(YAMLFileType.YML, "'\\Doctrine\\Foobar':\n" +
            "  type: entity\n" +
            "  fields: {}"
        );

        assertTrue(Objects.requireNonNull(DoctrineUtil.getClassRepositoryPair(yamlFile7)).stream().anyMatch(
            m -> "\\Doctrine\\Foobar".equals(m.className())
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
            m -> "Documents\\User".equals(m.className())
        ));

        YAMLFile yamlFile2 = (YAMLFile) myFixture.configureByText(YAMLFileType.YML, "User:\n" +
            "  type: document\n" +
            "  embedOne:\n" +
            "    address:\n" +
            "      targetDocument: Address"
        );

        assertTrue(Objects.requireNonNull(DoctrineUtil.getClassRepositoryPair(yamlFile2)).stream().anyMatch(
            m -> "User".equals(m.className())
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
