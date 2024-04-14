package fr.adrienbrault.idea.symfony2plugin.tests.doctrine.querybuilder;

import com.intellij.codeInsight.lookup.LookupElementPresentation;
import com.jetbrains.php.lang.PhpFileType;
import fr.adrienbrault.idea.symfony2plugin.doctrine.querybuilder.QueryBuilderCompletionContributor;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import org.jetbrains.annotations.NotNull;

/**
 * @see QueryBuilderCompletionContributor
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class QueryBuilderCompletionContributorTest extends SymfonyLightCodeInsightFixtureTestCase {
    public void setUp() throws Exception {
        super.setUp();
        myFixture.copyFileToProject("doctrine.orm.yml");
        myFixture.copyFileToProject("QueryBuilderCompletionContributor.php");
    }

    public String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/doctrine/querybuilder/fixtures";
    }

    public void testCompletionForSelect() {
        assertCompletionContains(
            "test.php",
            createQueryBuilderWrap("$qb->select('<caret>');"),
            "foobar.id", "foobar.name", "s.id"
        );

        assertCompletionContains(
            "test.php",
            createQueryBuilderWrap("$qb->select('foo.bar', '<caret>');"),
            "foobar.id", "foobar.name", "s.id"
        );

        assertCompletionContains(
            "test.php",
            createQueryBuilderWrap("$qb->select('FOO(f<caret>)');"),
            "foobar.id", "foobar.name"
        );
    }

    public void testCompletionForSelectField() {
        assertCompletionContains(
            "test.php",
            createQueryBuilderWrap("$qb->select('foobar, foobar.na<caret>me');"),
            "foobar.name"
        );
    }

    public void testCompletionForAddSelect() {
        assertCompletionContains(
            "test.php",
            createQueryBuilderWrap("$qb->addSelect('<caret>');"),
            "foobar.id", "foobar.name", "s.id"
        );

        assertCompletionContains(
            "test.php",
            createQueryBuilderWrap("$qb->addSelect('foo.bar', '<caret>');"),
            "foobar.id", "foobar.name", "s.id"
        );
    }

    public void testServiceEntityRepositoryConstructorModelContextOnQueryBuilderJoin() {
        assertCompletionContains("test.php", "<?php\n" +
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
            "        $qb->andWhere('s.<caret>');\n" +
            "    }\n" +
            "}", "s.name", "s.id");
    }

    public void testCompletionForJoinViaClassNameOnNoRelation() {
        assertCompletionContains("test.php", "<?php\n" +
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
            "        $qb->join(\\App\\Entity::class, 'foobar');\n" +
            "        $qb->andWhere('foobar.<caret>');\n" +
            "    }\n" +
            "}", "foobar.name", "foobar.id");

        assertCompletionContains("test.php", "<?php\n" +
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
            "        $qb->join(\"App\\Entity\", 'foobar');\n" +
            "        $qb->andWhere('foobar.<caret>');\n" +
            "    }\n" +
            "}", "foobar.name", "foobar.id");
    }

    public void testCompletionForWhere() {
        assertCompletionContains(
            "test.php",
            createQueryBuilderWrap("$qb->andWhere('<caret>');"),
            "foobar.id", "foobar.name", "s.id"
        );

        assertCompletionContains(
            "test.php",
            createQueryBuilderWrap("$qb->where('<caret>');"),
            "foobar.id", "foobar.name", "s.id"
        );
    }

    public void testCompletionForWhereAfterPoint() {
        assertCompletionContains(
            "test.php",
            createQueryBuilderWrap("$qb->andWhere('foobar.test = foobar.<caret>');"),
            "foobar.id", "foobar.name"
        );

        assertCompletionContains(
            "test.php",
            createQueryBuilderWrap("$qb->andWhere('foobar.test >= foobar.<caret>');"),
            "foobar.id", "foobar.name"
        );

        assertCompletionContains(
            "test.php",
            createQueryBuilderWrap("$qb->andWhere('foobar.test AND foobar.<caret>');"),
            "foobar.id", "foobar.name"
        );
    }

    public void testCompletionForWhereForRoot() {
        assertCompletionContains(
            "test.php",
            createQueryBuilderWrap("$qb->andWhere('foobar.test = foobar<caret>');"),
            "foobar.id", "foobar.name"
        );
    }

    public void testParameterCompletion() {
        assertCompletionContains(
            "test.php",
            createQueryBuilderWrap("$qb->andWhere('foobar.test = :<caret>');"),
            ":foobar_test", ":foobarTest", ":test"
        );

        assertCompletionContains(
            "test.php",
            createQueryBuilderWrap("$qb->andWhere('foobar.test = :t<caret>');"),
            ":test"
        );

        assertCompletionContains(
            "test.php",
            createQueryBuilderWrap("$qb->andWhere('foobar.test = <caret>');"),
            ":test"
        );
    }

    public void testSetParameterCompletion() {
        assertCompletionContains(
            "test.php",
            createQueryBuilderWrap(
                "$qb->andWhere('foobar.test = :foobar');\n" +
                "$qb->setParameter('<caret>');"
            ),
            "foobar"
        );

        assertCompletionContains(
            "test.php",
            createQueryBuilderWrap(
                "$qb->andWhere('foobar.test = :foobar');\n" +
                "$qb->setParameters(['<caret>']);"
            ),
            "foobar"
        );
    }

    private String createQueryBuilderWrap(@NotNull String content) {
        return "<?php\n" +
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
            "        $qb->join(\"App\\Entity\", 'foobar');\n" +
            "        " + content + "\n" +
            "    }\n" +
            "}";
    }
}
