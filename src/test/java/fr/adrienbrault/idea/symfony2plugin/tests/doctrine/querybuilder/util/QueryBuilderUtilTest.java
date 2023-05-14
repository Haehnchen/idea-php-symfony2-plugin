package fr.adrienbrault.idea.symfony2plugin.tests.doctrine.querybuilder.util;

import com.intellij.openapi.util.Pair;
import fr.adrienbrault.idea.symfony2plugin.doctrine.querybuilder.util.QueryBuilderUtil;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

import java.util.ArrayList;
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

    public void testGetFieldString() {
        Collection<Pair<String, String>> items = new ArrayList<>() {{
            add(Pair.create("user.foo = :foo AND user.ba<caret>r AND bar.foo", "user.bar"));
            add(Pair.create("user.foo = :foo AND us<caret>er.bar AND bar.foo", "user.bar"));
            add(Pair.create("user.ba<caret>r>=", "user.bar"));
            add(Pair.create(">=user.ba<caret>r", "user.bar"));
            add(Pair.create("user.<caret>bar", "user.bar"));
            add(Pair.create("user<caret>.bar", "user.bar"));
            add(Pair.create(" test.i<caret>d ", "test.id"));
            add(Pair.create("TEST(test.i<caret>d)", "test.id"));
            add(Pair.create(" TEST( test.i<caret>d ) ", "test.id"));
            add(Pair.create("test.foo = test.i<caret>d", "test.id"));
            add(Pair.create("user.ba<caret>rÃ„", "user.bar")); // nice usability but should this really match?
            add(Pair.create("us<caret>er.barÃ„", "user.bar")); // nice usability but should this really match?
            add(Pair.create("us<caret>er", null));
            // add(Pair.create("user.bar<caret> AND", null)); // ???
        }};

        for (Pair<String, String> item : items) {
            String content = item.getFirst();

            String string = QueryBuilderUtil.getFieldString(content.replace("<caret>", ""), content.indexOf("<caret>"));
            assertEquals(item.getSecond(), string);
        }
    }
}
