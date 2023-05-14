package fr.adrienbrault.idea.symfony2plugin.tests.doctrine.querybuilder;

import com.intellij.patterns.PlatformPatterns;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import org.jetbrains.yaml.psi.YAMLKeyValue;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class QueryBuilderGotoDeclarationHandlerTest extends SymfonyLightCodeInsightFixtureTestCase {
    public void setUp() throws Exception {
        super.setUp();
        myFixture.copyFileToProject("doctrine.orm.yml");
        myFixture.copyFileToProject("QueryBuilderCompletionContributor.php");
    }

    public String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/doctrine/querybuilder/fixtures";
    }

    public void testNavigationForFields() {
        assertNavigationMatch("test.php", "<?php\n" +
            "\n" +
            "use Doctrine\\Bundle\\DoctrineBundle\\Repository\\ServiceEntityRepository;\n" +
            "\n" +
            "class Repository extends ServiceEntityRepository\n" +
            "{\n" +
            "    public function __construct(RegistryInterface $registry)\n" +
            "    {\n" +
            "        parent::__construct($registry, \\App\\Entity::class);\n" +
            "    }\n" +
            "\n" +
            "    public function foobar()\n" +
            "    {\n" +
            "        $qb = $this->createQueryBuilder('s');\n" +
            "        $qb->andWhere('s.i<caret>d');\n" +
            "    }\n" +
            "}",
            PlatformPatterns.psiElement(YAMLKeyValue.class)
        );

        assertNavigationMatch("test.php", "<?php\n" +
                "\n" +
                "use Doctrine\\Bundle\\DoctrineBundle\\Repository\\ServiceEntityRepository;\n" +
                "\n" +
                "class Repository extends ServiceEntityRepository\n" +
                "{\n" +
                "    public function __construct(RegistryInterface $registry)\n" +
                "    {\n" +
                "        parent::__construct($registry, \\App\\Entity::class);\n" +
                "    }\n" +
                "\n" +
                "    public function foobar()\n" +
                "    {\n" +
                "        $qb = $this->createQueryBuilder('s');\n" +
                "        $qb->select('s.i<caret>d');\n" +
                "    }\n" +
                "}",
            PlatformPatterns.psiElement(YAMLKeyValue.class)
        );

        assertNavigationMatch("test.php", "<?php\n" +
                "\n" +
                "use Doctrine\\Bundle\\DoctrineBundle\\Repository\\ServiceEntityRepository;\n" +
                "\n" +
                "class Repository extends ServiceEntityRepository\n" +
                "{\n" +
                "    public function __construct(RegistryInterface $registry)\n" +
                "    {\n" +
                "        parent::__construct($registry, \\App\\Entity::class);\n" +
                "    }\n" +
                "\n" +
                "    public function foobar()\n" +
                "    {\n" +
                "        $qb = $this->createQueryBuilder('s');\n" +
                "        $qb->addSelect('foo,s.i<caret>d,foobar as foo');\n" +
                "    }\n" +
                "}",
            PlatformPatterns.psiElement(YAMLKeyValue.class)
        );

        assertNavigationMatch("test.php", "<?php\n" +
                "\n" +
                "use Doctrine\\Bundle\\DoctrineBundle\\Repository\\ServiceEntityRepository;\n" +
                "\n" +
                "class Repository extends ServiceEntityRepository\n" +
                "{\n" +
                "    public function __construct(RegistryInterface $registry)\n" +
                "    {\n" +
                "        parent::__construct($registry, \\App\\Entity::class);\n" +
                "    }\n" +
                "\n" +
                "    public function foobar()\n" +
                "    {\n" +
                "        $qb = $this->createQueryBuilder('s');\n" +
                "        $qb->addSelect('foo,FOOBAR(s.i<caret>d),foobar as foo');\n" +
                "    }\n" +
                "}",
            PlatformPatterns.psiElement(YAMLKeyValue.class)
        );

    }

    public void testNavigationForJoinAlias() {
        assertNavigationMatch("test.php", "<?php\n" +
                "\n" +
                "use Doctrine\\Bundle\\DoctrineBundle\\Repository\\ServiceEntityRepository;\n" +
                "\n" +
                "class Repository extends ServiceEntityRepository\n" +
                "{\n" +
                "    public function __construct(RegistryInterface $registry)\n" +
                "    {\n" +
                "        parent::__construct($registry, \\App\\Entity::class);\n" +
                "    }\n" +
                "\n" +
                "    public function foobar()\n" +
                "    {\n" +
                "        $qb = $this->createQueryBuilder('s');\n" +
                "        $qb->join('', '', '', 'foo ON s.i<caret>d = foo.bar');\n" +
                "    }\n" +
                "}",
            PlatformPatterns.psiElement(YAMLKeyValue.class)
        );
    }

    public void testNavigationForJoinIndexBy() {
        assertNavigationMatch("test.php", "<?php\n" +
                "\n" +
                "use Doctrine\\Bundle\\DoctrineBundle\\Repository\\ServiceEntityRepository;\n" +
                "\n" +
                "class Repository extends ServiceEntityRepository\n" +
                "{\n" +
                "    public function __construct(RegistryInterface $registry)\n" +
                "    {\n" +
                "        parent::__construct($registry, \\App\\Entity::class);\n" +
                "    }\n" +
                "\n" +
                "    public function foobar()\n" +
                "    {\n" +
                "        $qb = $this->createQueryBuilder('s');\n" +
                "        $qb->join('', '', '', '', ' s.i<caret>d ');\n" +
                "    }\n" +
                "}",
            PlatformPatterns.psiElement(YAMLKeyValue.class)
        );
    }

    public void testNavigationQueryBuilderCreateIndexBy() {
        assertNavigationMatch("test.php", "<?php\n" +
                "\n" +
                "use Doctrine\\Bundle\\DoctrineBundle\\Repository\\ServiceEntityRepository;\n" +
                "\n" +
                "class Repository extends ServiceEntityRepository\n" +
                "{\n" +
                "    public function __construct(RegistryInterface $registry)\n" +
                "    {\n" +
                "        parent::__construct($registry, \\App\\Entity::class);\n" +
                "    }\n" +
                "\n" +
                "    public function foobar()\n" +
                "    {\n" +
                "        $qb = $this->createQueryBuilder('s', ' s.i<caret>d ');\n" +
                "    }\n" +
                "}",
            PlatformPatterns.psiElement(YAMLKeyValue.class)
        );
    }

    public void testNavigationFromIndexBy() {
        assertNavigationMatch("test.php", "<?php\n" +
                "\n" +
                "use Doctrine\\Bundle\\DoctrineBundle\\Repository\\ServiceEntityRepository;\n" +
                "\n" +
                "class Repository extends ServiceEntityRepository\n" +
                "{\n" +
                "    public function __construct(RegistryInterface $registry)\n" +
                "    {\n" +
                "        parent::__construct($registry, \\App\\Entity::class);\n" +
                "    }\n" +
                "\n" +
                "    public function foobar()\n" +
                "    {\n" +
                "        $qb = $this->createQueryBuilder('s');\n" +
                "        $qb->from('', '', ' s.i<caret>d ');\n" +
                "    }\n" +
                "}",
            PlatformPatterns.psiElement(YAMLKeyValue.class)
        );
    }

    public void testNavigationGroupBy() {
        assertNavigationMatch("test.php", "<?php\n" +
                "\n" +
                "use Doctrine\\Bundle\\DoctrineBundle\\Repository\\ServiceEntityRepository;\n" +
                "\n" +
                "class Repository extends ServiceEntityRepository\n" +
                "{\n" +
                "    public function __construct(RegistryInterface $registry)\n" +
                "    {\n" +
                "        parent::__construct($registry, \\App\\Entity::class);\n" +
                "    }\n" +
                "\n" +
                "    public function foobar()\n" +
                "    {\n" +
                "        $qb = $this->createQueryBuilder('s');\n" +
                "        $qb->groupBy('s.i<caret>d');\n" +
                "    }\n" +
                "}",
            PlatformPatterns.psiElement(YAMLKeyValue.class)
        );

        assertNavigationMatch("test.php", "<?php\n" +
                "\n" +
                "use Doctrine\\Bundle\\DoctrineBundle\\Repository\\ServiceEntityRepository;\n" +
                "\n" +
                "class Repository extends ServiceEntityRepository\n" +
                "{\n" +
                "    public function __construct(RegistryInterface $registry)\n" +
                "    {\n" +
                "        parent::__construct($registry, \\App\\Entity::class);\n" +
                "    }\n" +
                "\n" +
                "    public function foobar()\n" +
                "    {\n" +
                "        $qb = $this->createQueryBuilder('s');\n" +
                "        $qb->addGroupBy('s.i<caret>d');\n" +
                "    }\n" +
                "}",
            PlatformPatterns.psiElement(YAMLKeyValue.class)
        );
    }
}
