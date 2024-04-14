package fr.adrienbrault.idea.symfony2plugin.tests.doctrine.querybuilder.util;

import com.intellij.openapi.util.Pair;
import fr.adrienbrault.idea.symfony2plugin.doctrine.querybuilder.dict.QueryBuilderCompletionContribution;
import fr.adrienbrault.idea.symfony2plugin.doctrine.querybuilder.dict.QueryBuilderCompletionContributionType;
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

    public void testGuestCompletionContribution() {
        Collection<QueryBuilderCompletionContribution> t1 = QueryBuilderUtil.guestCompletionContribution("test_foo.te");
        assertEquals(1, t1.size());
        assertTrue(t1.stream().anyMatch(c -> c.type() == QueryBuilderCompletionContributionType.PROPERTY && "test_foo.te".equals(c.prefix())));

        Collection<QueryBuilderCompletionContribution> t2 = QueryBuilderUtil.guestCompletionContribution("");
        assertEquals(2, t2.size());
        assertTrue(t2.stream().anyMatch(c -> c.type() == QueryBuilderCompletionContributionType.PROPERTY && c.prefix().isEmpty()));
        assertTrue(t2.stream().anyMatch(c -> c.type() == QueryBuilderCompletionContributionType.FUNCTION && c.prefix().isEmpty()));

        Collection<QueryBuilderCompletionContribution> t3 = QueryBuilderUtil.guestCompletionContribution("test_foo.");
        assertEquals(1, t3.size());
        assertTrue(t3.stream().anyMatch(c -> c.type() == QueryBuilderCompletionContributionType.PROPERTY && "test_foo.".equals(c.prefix())));

        Collection<QueryBuilderCompletionContribution> t4 = QueryBuilderUtil.guestCompletionContribution("foo, test_foo.test_foo");
        assertEquals(1, t4.size());
        assertTrue(t4.stream().anyMatch(c -> c.type() == QueryBuilderCompletionContributionType.PROPERTY && "test_foo.test_foo".equals(c.prefix())));

        Collection<QueryBuilderCompletionContribution> t5 = QueryBuilderUtil.guestCompletionContribution("(test_foo.test_foo");
        assertEquals(1, t5.size());
        assertTrue(t5.stream().anyMatch(c -> c.type() == QueryBuilderCompletionContributionType.PROPERTY && "test_foo.test_foo".equals(c.prefix())));

        Collection<QueryBuilderCompletionContribution> t6 = QueryBuilderUtil.guestCompletionContribution("test_foo");
        assertEquals(2, t6.size());
        assertTrue(t6.stream().anyMatch(c -> c.type() == QueryBuilderCompletionContributionType.FUNCTION && "test_foo".equals(c.prefix())));

        Collection<QueryBuilderCompletionContribution> t7 = QueryBuilderUtil.guestCompletionContribution("= test_foo.a");
        assertEquals(1, t7.size());
        assertTrue(t7.stream().anyMatch(c -> c.type() == QueryBuilderCompletionContributionType.PROPERTY && "test_foo.a".equals(c.prefix())));

        Collection<QueryBuilderCompletionContribution> t8 = QueryBuilderUtil.guestCompletionContribution("= test_foo");
        assertEquals(1, t8.size());
        assertTrue(t8.stream().anyMatch(c -> c.type() == QueryBuilderCompletionContributionType.PROPERTY && "test_foo".equals(c.prefix())));

        Collection<QueryBuilderCompletionContribution> t9 = QueryBuilderUtil.guestCompletionContribution("AND test_foo.");
        assertEquals(1, t9.size());
        assertTrue(t9.stream().anyMatch(c -> c.type() == QueryBuilderCompletionContributionType.PROPERTY && "test_foo.".equals(c.prefix())));

        Collection<QueryBuilderCompletionContribution> t10 = QueryBuilderUtil.guestCompletionContribution("FOO(test_foo.fo");
        assertEquals(1, t10.size());
        assertTrue(t10.stream().anyMatch(c -> c.type() == QueryBuilderCompletionContributionType.PROPERTY && "test_foo.fo".equals(c.prefix())));

        Collection<QueryBuilderCompletionContribution> t11 = QueryBuilderUtil.guestCompletionContribution("FOO(test_foo.fo");
        assertEquals(1, t11.size());
        assertTrue(t11.stream().anyMatch(c -> c.type() == QueryBuilderCompletionContributionType.PROPERTY && "test_foo.fo".equals(c.prefix())));
    }
}
