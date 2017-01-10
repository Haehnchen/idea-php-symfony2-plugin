package fr.adrienbrault.idea.symfony2plugin.tests.doctrine.querybuilder.util;

import fr.adrienbrault.idea.symfony2plugin.doctrine.querybuilder.util.QueryBuilderUtil;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

import java.util.Collection;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class QueryBuilderUtilTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void testExtractQueryBuilderRepositoryParametersForShortcut() {
        Collection<String> strings = QueryBuilderUtil.extractQueryBuilderRepositoryParameters(
            "#M#ő#M#C\\Doctrine\\ORM\\EntityManager.getRepositoryƅespendDoctrineModelBundle:Car.createQueryBuilder|#M#M#C\\Doctrine\\ORM\\EntityManager.getRepository.createQueryBuilder"
        );

        assertContainsElements(strings, "espendDoctrineModelBundle:Car");
    }

    public void testExtractQueryBuilderRepositoryParametersForClassConstant() {
        Collection<String> strings = QueryBuilderUtil.extractQueryBuilderRepositoryParameters(
            "#M#ő#M#C\\Doctrine\\ORM\\EntityManager.getRepositoryƅ#K#C\\espend\\Doctrine\\ModelBundle\\Entity\\Car.class.createQueryBuilder|#M#M#C\\Doctrine\\ORM\\EntityManager.getRepository.createQueryBuilder"
        );

        assertContainsElements(strings, "#K#C\\espend\\Doctrine\\ModelBundle\\Entity\\Car.class");
    }
}
