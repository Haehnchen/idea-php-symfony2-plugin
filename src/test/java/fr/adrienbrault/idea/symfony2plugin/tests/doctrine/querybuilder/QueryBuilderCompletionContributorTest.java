package fr.adrienbrault.idea.symfony2plugin.tests.doctrine.querybuilder;

import fr.adrienbrault.idea.symfony2plugin.doctrine.querybuilder.QueryBuilderCompletionContributor;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

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
}
